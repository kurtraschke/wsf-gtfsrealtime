# wsf-gtfsrealtime
GTFS-realtime converter for the [Washington State Ferries](http://www.wsdot.wa.gov/ferries/) [API](http://www.wsdot.wa.gov/ferries/api/vessels/documentation/rest.html)

## Build

Build with `mvn install`.

## Configure

1. Get an API access key [from WSDOT](http://www.wsdot.wa.gov/traffic/api/).
2. Get a copy of the WSF GTFS: https://business.wsdot.wa.gov/Transit/csv_files/wsf/google_transit.zip
3. Copy `config.sample` to `config` and edit it to set the API access key and the path to the GTFS feed.

## Run

Run with `java -jar target/wsf-gtfsrealtime-1.0-SNAPSHOT-withAllDependencies.jar --config=config`.

Configuration values can also be set as Java properties (such as `-DWSF.agencyId=WSF`); the configuration file can be omitted entirely if all required values are defined this way.

You may wish to adjust the logging levels in `logback.xml` to cut down on the volume of logging after initial configuration and testing.