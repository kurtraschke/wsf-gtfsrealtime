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

import org.onebusaway.gtfs.model.calendar.CalendarServiceData;

import com.google.inject.Provider;

import java.util.TimeZone;

import javax.inject.Inject;
import javax.inject.Named;

public class AgencyTimeZoneProvider implements Provider<TimeZone> {

  @Inject
  private CalendarServiceData _csd;

  @Inject
  @Named("WSF.agencyId")
  private String _agencyId;

  @Override
  public TimeZone get() {
    return _csd.getTimeZoneForAgencyId(_agencyId);
  }
}
