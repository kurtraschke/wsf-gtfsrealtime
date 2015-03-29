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
package com.kurtraschke.wsf.gtfsrealtime.services;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.calendar.CalendarServiceData;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.onebusaway.gtfs.services.GtfsRelationalDao;

import com.google.common.collect.Iterables;
import com.kurtraschke.wsf.gtfsrealtime.AgencyTimeZone;
import com.kurtraschke.wsf.gtfsrealtime.model.ActivatedTrip;

import java.util.Date;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;

public class TripResolutionService {

  @Inject
  private GtfsRelationalDao _dao;

  @Inject
  private CalendarServiceData _csd;

  @Inject
  @AgencyTimeZone
  private TimeZone _agencyTimeZone;

  @Inject
  @Named("WSF.agencyId")
  private String _agencyId;

  private int maxStopTime() {
    return _dao.getAllStopTimes().stream()
            .flatMap(
                    st -> {
                      return Stream.concat(
                              st.isArrivalTimeSet() ? Stream.of(st.getArrivalTime()) : Stream.<Integer>empty(),
                              st.isDepartureTimeSet() ? Stream.of(st.getDepartureTime()) : Stream.<Integer>empty());
                    }
            )
            .reduce(Integer::max).get();
  }

  public ActivatedTrip resolve(String departingTerminalId, long departureTime, String arrivingTerminalId) {
    ServiceDate initialServiceDate = new ServiceDate(new Date(departureTime * 1000));
    int maxStopTime = maxStopTime();
    int lookBackDays = (maxStopTime / 86400) + 1;

    AgencyAndId stopId = new AgencyAndId(_agencyId, departingTerminalId);
    AgencyAndId routeId = new AgencyAndId(_agencyId, departingTerminalId + arrivingTerminalId);

    Set<ActivatedTrip> collect = _dao.getAllStopTimes().stream()
            .filter(st -> st.getStop().getId().equals(stopId))
            .filter(st -> st.getTrip().getRoute().getId().equals(routeId))
            .flatMap(
                    st -> {
                      return Stream.iterate(initialServiceDate, ServiceDate::previous).limit(lookBackDays)
                      .filter(sd -> _csd.getServiceIdsForDate(sd).contains(st.getTrip().getServiceId()))
                      .filter(sd -> st.getDepartureTime() == (int) (departureTime - (sd.getAsCalendar(_agencyTimeZone).getTimeInMillis() / 1000)))
                      .map(sd -> new ActivatedTrip(st.getTrip(), sd));
                    }
            )
            .collect(Collectors.toSet());

    return Iterables.getOnlyElement(collect);
  }

}
