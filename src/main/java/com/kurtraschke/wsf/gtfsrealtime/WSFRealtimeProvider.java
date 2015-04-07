/*
 * Copyright (C) 2015 Kurt Raschke <kurt@kurtraschke.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.kurtraschke.wsf.gtfsrealtime;

import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.onebusaway.gtfs_realtime.exporter.GtfsRealtimeFullUpdate;
import org.onebusaway.gtfs_realtime.exporter.GtfsRealtimeGuiceBindingTypes.Alerts;
import org.onebusaway.gtfs_realtime.exporter.GtfsRealtimeGuiceBindingTypes.TripUpdates;
import org.onebusaway.gtfs_realtime.exporter.GtfsRealtimeGuiceBindingTypes.VehiclePositions;
import org.onebusaway.gtfs_realtime.exporter.GtfsRealtimeSink;

import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.Position;
import com.google.transit.realtime.GtfsRealtime.TripDescriptor;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeEvent;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate;
import com.google.transit.realtime.GtfsRealtime.VehicleDescriptor;
import com.google.transit.realtime.GtfsRealtime.VehiclePosition;
import com.kurtraschke.wsf.gtfsrealtime.model.ActivatedTrip;
import com.kurtraschke.wsf.gtfsrealtime.services.TripResolutionService;
import com.kurtraschke.wsf.gtfsrealtime.services.WSFVesselLocationService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.XMLGregorianCalendar;

import gov.wa.wsdot.ferries.vessels.VesselLocationResponse;

public class WSFRealtimeProvider {

  private static final Logger _log = LoggerFactory.getLogger(WSFRealtimeProvider.class);

  @Inject
  private ScheduledExecutorService _executor;

  @Inject
  @VehiclePositions
  private GtfsRealtimeSink _vehiclePositionsSink;

  @Inject
  @TripUpdates
  private GtfsRealtimeSink _tripUpdatesSink;

  @Inject
  @Alerts
  private GtfsRealtimeSink _alertsSink;

  @Inject
  @Named("refreshInterval.vessels")
  private Integer _vesselRefreshInterval;

  @Inject
  @AgencyTimeZone
  private TimeZone _agencyTimeZone;

  @Inject
  private WSFVesselLocationService _vesselLocationService;

  @Inject
  private TripResolutionService _tripResolutionService;

  private static final float KNOT_TO_KM_H = 1.852f;

  private static final float KM_H_TO_M_S = 1000f / 3600f;

  @PostConstruct
  public void start() {
    _log.info("Starting GTFS-realtime service");
    _executor.scheduleWithFixedDelay(new VesselRefreshTask(), 0, _vesselRefreshInterval, TimeUnit.SECONDS);
  }

  @PreDestroy
  public void stop() {
    _log.info("Stopping GTFS-realtime service");
    _executor.shutdownNow();
  }

  private class VesselRefreshTask implements Runnable {

    @Override
    public void run() {
      _log.info("Refreshing vessels...");
      try {
        Iterable<VesselLocationResponse> allVessels = _vesselLocationService.getAllVessels();

        GtfsRealtimeFullUpdate vehiclePositionsUpdate = new GtfsRealtimeFullUpdate();
        GtfsRealtimeFullUpdate tripUpdatesUpdate = new GtfsRealtimeFullUpdate();

        for (VesselLocationResponse vlr : allVessels) {
          if (!vlr.isInService()) {
            _log.debug("Discarding update for vessel {} because vessel is not in service.", vlr.getVesselID());
            continue;
          }

          if (vlr.getArrivingTerminalID().isNil() || vlr.getScheduledDeparture().isNil()) {
            _log.debug("Discarding update for vessel {} because arriving terminal or scheduled departure are undefined.", vlr.getVesselID());
            continue;
          }

          try {
            VehicleDescriptor vd = buildVehicleDescriptor(vlr);
            TripDescriptor td = buildTripDescriptor(vlr);

            if (td == null) {
              _log.debug("Discarding update for vessel {} because trip could not be mapped.", vlr.getVesselID());
              continue;
            }

            FeedEntity vehiclePositionFeedEntity = wrapVehiclePosition(buildVehiclePosition(vlr, vd, td));
            vehiclePositionsUpdate.addEntity(vehiclePositionFeedEntity);

            FeedEntity tripUpdateFeedEntity = wrapTripUpdate(buildTripUpdate(vlr, vd, td));
            if (tripUpdateFeedEntity.getTripUpdate().getStopTimeUpdateCount() > 0) {
              tripUpdatesUpdate.addEntity(tripUpdateFeedEntity);
            } else {
              _log.debug("Discarding update for vessel {} because no StopTimeUpdates were produced.", vlr.getVesselID());
            }
          } catch (Exception ex) {
            _log.warn(String.format("Error updating vessel %d:", vlr.getVesselID()), ex);
          }
        }

        _vehiclePositionsSink.handleFullUpdate(vehiclePositionsUpdate);
        _tripUpdatesSink.handleFullUpdate(tripUpdatesUpdate);

      } catch (URISyntaxException | IOException | JAXBException ex) {
        _log.error("Vessel update error:", ex);
      }
    }

    private VehicleDescriptor buildVehicleDescriptor(VesselLocationResponse vlr) {
      VehicleDescriptor.Builder vehicleDescriptor = VehicleDescriptor.newBuilder();

      vehicleDescriptor.setId(vlr.getVesselID().toString());
      vehicleDescriptor.setLicensePlate(vlr.getMmsi().getValue().toString());
      vehicleDescriptor.setLabel(vlr.getVesselName().getValue());

      return vehicleDescriptor.build();
    }

    private TripDescriptor buildTripDescriptor(VesselLocationResponse vlr) {
      TripDescriptor.Builder tripDescriptor = TripDescriptor.newBuilder();

      ActivatedTrip activatedTrip = _tripResolutionService.resolve(vlr.getDepartingTerminalID().toString(),
              ts(vlr.getScheduledDeparture().getValue()),
              vlr.getArrivingTerminalID().getValue().toString());

      if (activatedTrip == null) {
        return null;
      }

      Trip trip = activatedTrip.getTrip();
      ServiceDate sd = activatedTrip.getServiceDate();

      tripDescriptor.setTripId(trip.getId().getId());
      tripDescriptor.setRouteId(trip.getRoute().getId().getId());
      tripDescriptor.setStartDate(sd.getAsString());

      return tripDescriptor.build();
    }

    private Position buildPosition(VesselLocationResponse vlr) {
      Position.Builder position = Position.newBuilder();

      position.setLatitude(vlr.getLatitude().floatValue());
      position.setLongitude(vlr.getLongitude().floatValue());
      position.setBearing(vlr.getHeading());
      position.setSpeed(vlr.getSpeed().floatValue() * KNOT_TO_KM_H * KM_H_TO_M_S);

      return position.build();
    }

    private VehiclePosition buildVehiclePosition(VesselLocationResponse vlr,
            VehicleDescriptor vd, TripDescriptor td) {
      VehiclePosition.Builder vehiclePosition = VehiclePosition.newBuilder();
      vehiclePosition.setVehicle(vd);
      vehiclePosition.setTrip(td);
      vehiclePosition.setPosition(buildPosition(vlr));

      vehiclePosition.setTimestamp(ts(vlr.getTimeStamp()));

      return vehiclePosition.build();
    }

    private FeedEntity wrapVehiclePosition(VehiclePosition vp) {
      FeedEntity.Builder feb = FeedEntity.newBuilder();
      feb.setVehicle(vp);
      feb.setId(vp.getVehicle().getId());
      return feb.build();
    }

    private TripUpdate buildTripUpdate(VesselLocationResponse vlr,
            VehicleDescriptor vd, TripDescriptor td) {
      TripUpdate.Builder tripUpdate = TripUpdate.newBuilder();
      tripUpdate.setVehicle(vd);

      tripUpdate.setTrip(td);

      if (!vlr.getLeftDock().isNil()) {
        StopTimeUpdate.Builder stopTimeUpdate = tripUpdate.addStopTimeUpdateBuilder();

        stopTimeUpdate.setStopId(vlr.getDepartingTerminalID().toString());

        StopTimeEvent.Builder stopTimeEvent = stopTimeUpdate.getDepartureBuilder();
        stopTimeEvent.setTime(ts(vlr.getLeftDock().getValue()));

      }

      if (!vlr.getArrivingTerminalID().isNil() && !vlr.getEta().isNil()) {
        StopTimeUpdate.Builder stopTimeUpdate = tripUpdate.addStopTimeUpdateBuilder();

        stopTimeUpdate.setStopId(vlr.getArrivingTerminalID().getValue().toString());

        StopTimeEvent.Builder stopTimeEvent = stopTimeUpdate.getArrivalBuilder();
        stopTimeEvent.setTime(ts(vlr.getEta().getValue()));
      }

      tripUpdate.setTimestamp(ts(vlr.getTimeStamp()));

      return tripUpdate.build();
    }

    private FeedEntity wrapTripUpdate(TripUpdate tu) {
      FeedEntity.Builder feb = FeedEntity.newBuilder();
      feb.setTripUpdate(tu);
      feb.setId(tu.getVehicle().getId());
      return feb.build();
    }

    private long ts(XMLGregorianCalendar xgc) {
      GregorianCalendar gc = xgc.toGregorianCalendar(_agencyTimeZone, null, null);
      return (gc.getTimeInMillis() / 1000L);
    }
  }
}
