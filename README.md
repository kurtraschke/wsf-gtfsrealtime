# wsf-gtfsrealtime
GTFS-realtime converter for the [Washington State Ferries](http://www.wsdot.wa.gov/ferries/) [API](http://www.wsdot.wa.gov/ferries/api/vessels/documentation/rest.html)

## Building

Build with `mvn install`.

## Setup

1. Get an API access key [from WSDOT](http://www.wsdot.wa.gov/traffic/api/).
2. Get a copy of the WSF GTFS: https://business.wsdot.wa.gov/Transit/csv_files/wsf/google_transit.zip
3. Copy `config.sample` to `config` and edit it to set the API access key and the path to the GTFS feed.
4. Run with `java -jar target/wsf-gtfsrealtime-1.0-SNAPSHOT-withAllDependencies.jar --config=config`.
