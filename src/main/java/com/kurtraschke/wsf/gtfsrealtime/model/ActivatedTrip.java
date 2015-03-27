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
package com.kurtraschke.wsf.gtfsrealtime.model;

import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.model.calendar.ServiceDate;

import java.util.Objects;

public class ActivatedTrip {

  private final Trip trip;
  private final ServiceDate serviceDate;

  public ActivatedTrip(Trip trip, ServiceDate serviceDate) {
    this.trip = trip;
    this.serviceDate = serviceDate;
  }

  /**
   * @return the trip
   */
  public Trip getTrip() {
    return trip;
  }

  /**
   * @return the serviceDate
   */
  public ServiceDate getServiceDate() {
    return serviceDate;
  }

  @Override
  public String toString() {
    return "ActivatedTrip{" + "trip=" + trip + ", serviceDate=" + serviceDate + '}';
  }

  @Override
  public int hashCode() {
    int hash = 3;
    hash = 89 * hash + Objects.hashCode(this.trip);
    hash = 89 * hash + Objects.hashCode(this.serviceDate);
    return hash;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final ActivatedTrip other = (ActivatedTrip) obj;
    if (!Objects.equals(this.trip, other.trip)) {
      return false;
    }
    if (!Objects.equals(this.serviceDate, other.serviceDate)) {
      return false;
    }
    return true;
  }
}
