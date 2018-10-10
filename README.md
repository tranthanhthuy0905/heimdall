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

* See the application in browser, for example:
```
http://localhost:9000/media/v0/hls/agencies/f3d719bc-db2b-4b71-bfb1-436240fb9099/evidence/31b4f97f-20cd-40de-acb8-7a6fe95357eb/files/4f706842-e602-4287-b80b-a74b09d8995a
http://localhost:9000/media/v0/sessions/create/token-type/2/agency-id/f3d719bc-db2b-4b71-bfb1-436240fb9099
http://localhost:9000/media/v0/sessions/get/token-type/2/token/http://localhost:9000/media/v0/sessions/get/token-type/2/token/1xf5zqfyzvr37pto9u3sjnk8h0zha0l2xa9y5w7hhe6o00eqcz
http://localhost:9000/media/v0/sessions/delete/token-type/2/token/http://localhost:9000/media/v0/sessions/get/token-type/2/token/1xf5zqfyzvr37pto9u3sjnk8h0zha0l2xa9y5w7hhe6o00eqcz
```
Note [2018-09-26]: agency, evidence, and file ID-s must be provided in UUID format, including '-'.<br/>
I.e. xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx.

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
