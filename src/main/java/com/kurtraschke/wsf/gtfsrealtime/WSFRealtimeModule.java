/*
 * Copyright (C) 2015 Kurt Raschke <kurt@kurtraschke.com>
 * Copyright (C) 2012 Google, Inc.
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

import org.onebusaway.gtfs.model.calendar.CalendarServiceData;
import org.onebusaway.gtfs.services.GtfsRelationalDao;
import org.onebusaway.gtfs_realtime.exporter.GtfsRealtimeExporterModule;
import org.onebusaway.guice.jsr250.JSR250Module;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.Scopes;
import com.kurtraschke.wsf.gtfsrealtime.services.AgencyTimeZoneProvider;
import com.kurtraschke.wsf.gtfsrealtime.services.CalendarServiceDataProvider;
import com.kurtraschke.wsf.gtfsrealtime.services.GtfsRelationalDaoProvider;

import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class WSFRealtimeModule extends AbstractModule {

  public static void addModuleAndDependencies(Set<Module> modules) {
    modules.add(new WSFRealtimeModule());
    GtfsRealtimeExporterModule.addModuleAndDependencies(modules);
    JSR250Module.addModuleAndDependencies(modules);
  }

  @Override
  protected void configure() {
    bind(HttpClientConnectionManager.class)
            .toInstance(new PoolingHttpClientConnectionManager());
    
    bind(ScheduledExecutorService.class)
            .toInstance(Executors.newSingleThreadScheduledExecutor());

    bind(CalendarServiceData.class)
            .toProvider(CalendarServiceDataProvider.class)
            .in(Scopes.SINGLETON);
    
    bind(GtfsRelationalDao.class)
            .toProvider(GtfsRelationalDaoProvider.class)
            .in(Scopes.SINGLETON);
    
    bind(TimeZone.class)
            .annotatedWith(AgencyTimeZone.class)
            .toProvider(AgencyTimeZoneProvider.class)
            .in(Scopes.SINGLETON);
  }

  /**
   * Implement hashCode() and equals() such that two instances of the module
   * will be equal.
   */
  @Override
  public int hashCode() {
    return this.getClass().hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null) {
      return false;
    }
    return this.getClass().equals(o.getClass());
  }
}
