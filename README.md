# Heimdall - Gatekeeper &amp; Load Balancer for On Demand Media Streaming

## Naming
Heimdall the Gatekeeper is a servant of All-Father and ally of Thor. He was the guardian of Asgard, standing on the rainbow bifrost bridge and denying access to any unauthorised beings.

## Running

```bash
./bin/sbt run
```

And then go to <http://localhost:9000/media/alive> to see the healthcheck endpoint.


### Testing API
* Update conf/env/localdev.conf with dev twobox number and thrift_auth_secret. For example:
```
service {
  environment {
    type = "dbox001" # twobox number
  }
  properties = {}
}
```
Provide dredd thrift auth secret.
It can be found on machine hosting dredd-service in /opt/dredd-service/dredd-service/conf/application.conf.
```
edc.service.dredd {
  thrift_auth_secret = "05FF04474A8B4049AB59579FA9469272"
}
```
The same for sessions-service, see:
```
edc.service.sessions {
  thrift_auth_secret = "wED6ff2yzJZXcAI99e9dB7+H64vG4la2"
}
```
* Start the service
```
./bin/sbt run
```

#### HLS API:

1. Find or create a valid axon session cookie
2. Run probe request, and receive streamingSessionToken 
3. Use cookie and the token for HLS and thumbnail requests

```
# Probe:
curl -v  --cookie "AXONSESSION=1jmhyqyo8s8qva2bum9hw9w5z8c1s013rnircigd44tvztz96j" 'http://localhost:9000/media/start?file_id=4f706842-e602-4287-b80b-a74b09d8995a&evidence_id=31b4f97f-20cd-40de-acb8-7a6fe95357eb&partner_id=f3d719bc-db2b-4b71-bfb1-436240fb9099&level=0&index=0&offset=0' 

# Master:
ffplay 'http://localhost:9000/media/hls/master?file_id=4f706842-e602-4287-b80b-a74b09d8995a&evidence_id=31b4f97f-20cd-40de-acb8-7a6fe95357eb&partner_id=f3d719bc-db2b-4b71-bfb1-436240fb9099&streamingSessionToken=u0YQazkQ0Y4OeCNhSaiRpW2RNmSPR46MsrkuNcJv5Oo='  -headers 'Cookie: AXONSESSION=2tffd7bp6pm6jz4ogbs9g7z9g7ttcx3gvscw5wcdnoq4ykullm'

# Variant:
ffplay 'http://localhost:9000/media/hls/variant?file_id=4f706842-e602-4287-b80b-a74b09d8995a&evidence_id=31b4f97f-20cd-40de-acb8-7a6fe95357eb&partner_id=f3d719bc-db2b-4b71-bfb1-436240fb9099&level=0&streamingSessionToken=u0YQazkQ0Y4OeCNhSaiRpW2RNmSPR46MsrkuNcJv5Oo='  -headers 'Cookie: AXONSESSION=2tffd7bp6pm6jz4ogbs9g7z9g7ttcx3gvscw5wcdnoq4ykullm'

# Segment:
curl -v --cookie "AXONSESSION=2ux6qd1b12z82rdr5ywjn695wv1pcv2c1ib3dm983z0aq69zh3" 'http://localhost:9000/media/hls/segment?file_id=4f706842-e602-4287-b80b-a74b09d8995a&evidence_id=31b4f97f-20cd-40de-acb8-7a6fe95357eb&partner_id=f3d719bc-db2b-4b71-bfb1-436240fb9099&level=0&index=0&offset=0&start=0&fast=1&label=AWESOME_LABEL&streamingSessionToken=u0YQazkQ0Y4OeCNhSaiRpW2RNmSPR46MsrkuNcJv5Oo=' | ffplay -

# Thumbnail:
ffplay 'http://localhost:9000/media/thumbnail?file_id=79aea236e59442ccae2c6cd95af937d8&evidence_id=2b111cf1-d42b-4edd-b0ed-1a5d63f81b23&partner_id=f3d719bc-db2b-4b71-bfb1-436240fb9099&time=57&width=120&height=52&autorotate=true&streamingSessionToken=u0YQazkQ0Y4OeCNhSaiRpW2RNmSPR46MsrkuNcJv5Oo=' -headers 'Cookie: AXONSESSION=2rvqb4wcqh3xubuaea8xqmvo8msuddq9pmxzckwx48djzdfptd'

```

## Heimdall Anatomy
```
app                        → Application sources
 └ controllers             → Heimdall controllers
 └ models                  → Application business layer
    └ auth                 → Axon request authorization model
 └ services                → Heimdall's dependencies clients 
    └ dredd                → [dredd-service](https://git.taservs.net/ecom/dredd-service) client 
    └ sessions             → [sessions](https://git.taservs.net/ecom/sessions) client 
build.sbt                  → Application build script
conf                       → Configurations files and other non-compiled resources (on classpath)
 └ env                     → Environment configurations directory
    └ key_manager.conf     → Defins edc key values, Id-s, and types
    └ localdev.conf        → Development configuration
    └ test.conf            → Currently unused, meant for unit testing configuration
 └ application.conf        → Main configuration file
 └ filters.conf            → Play framework filters, such as CSRFFilter, AllowedHostFilters, and SecurityHeadersFilters
 └ logback.xml             → Logs configuration file
 └ reference.conf          → Default settings, reference.conf files are aggregated together at runtime, and they are overridden by any settings defined in the application.conf
 └ routes                  → Routes definition
project                    → sbt configuration files
 └ build.properties        → Marker for sbt project
 └ Common.scala            → Common settings or code of the root projects
 └ ConfigurationHook.scala → Hooking into Play’s dev mode
 └ plugins.sbt             → sbt plugins including the declaration for Play itself
lib                        → Unmanaged libraries dependencies
logs                       → Logs folder
 └ application.log         → Default log file
target                     → Generated stuff
 └ scala-2.XX
    └ classes              → Compiled class files
    └ routes               → Sources generated from routes
    └ twirl                → Sources generated from templates
    └ ...
 └ universal               → Application packaging
test                       → source folder for unit or functional tests
```
