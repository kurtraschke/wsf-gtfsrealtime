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

import com.google.common.collect.ImmutableList;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import gov.wa.wsdot.ferries.vessels.ArrayOfVesselLocationResponse;
import gov.wa.wsdot.ferries.vessels.VesselLocationResponse;

@Singleton
public class WSFVesselLocationService {

  @Inject
  @Named("WSF.apiaccesscode")
  String _apiAccessCode;

  @Inject
  HttpClientConnectionManager _connectionManager;

  Unmarshaller _um;

  public WSFVesselLocationService() throws JAXBException {
    JAXBContext jc = JAXBContext.newInstance(ArrayOfVesselLocationResponse.class);
    _um = jc.createUnmarshaller();
  }

  public List<VesselLocationResponse> getAllVessels() throws URISyntaxException, IOException, JAXBException {
    ImmutableList.Builder<VesselLocationResponse> allVessels = ImmutableList.builder();

    URI endpoint = new URIBuilder("http://www.wsdot.wa.gov/ferries/api/vessels/rest/vessellocations")
            .addParameter("apiaccesscode", _apiAccessCode)
            .build();
    CloseableHttpClient httpclient = HttpClients.custom()
            .setConnectionManager(_connectionManager)
            .build();
    HttpGet request = new HttpGet(endpoint);
    request.addHeader("Accept", "text/xml");

    try (CloseableHttpResponse response = httpclient.execute(request)) {
      HttpEntity entity = response.getEntity();
      if (entity != null) {
        try (InputStream instream = entity.getContent()) {
          JAXBElement<ArrayOfVesselLocationResponse> responseArray;
          responseArray = _um.unmarshal(new StreamSource(instream),
                  ArrayOfVesselLocationResponse.class);

          allVessels.addAll(responseArray.getValue().getVesselLocationResponse());
        }
      }
    }
    return allVessels.build();
  }
}
