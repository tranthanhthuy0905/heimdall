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

#### Heimdall's test routes (will be removed before the launch):

Note [2018-09-26]: agency, evidence, and file ID-s must be provided in UUID format, including '-'.<br/>
I.e. xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx.
```
# With agencyId: f3d719bc-db2b-4b71-bfb1-436240fb9099

curl 'http://localhost:9000/media/v0/test/create-session/f3d719bc-db2b-4b71-bfb1-436240fb9099'

> {"status":"ok","response":"CreateSessionResponse(SessionAuthorization(2xrypzge7d570p94sw0n8dc05kh9868sclqelvdznew698e5pi,EntityDescriptor(Subscriber,DB20158D-E501-4F1E-9689-E2EDDA39068E,Some(F3D719BC-DB2B-4B71-BFB1-436240FB9099)),eyJraWQiOiI1ZjBlZTM4Y2YyYTdkZWVkY2Q5ZTFiNGFjZDYwYTQ3NSIsImFsZyI6IkhTMjU2In0.eyJzdWIiOiJkYjIwMTU4ZC1lNTAxLTRmMWUtOTY4OS1lMmVkZGEzOTA2OGUiLCJ2ZXIiOiIyIiwicm9sZXMiOltdLCJpc3MiOiJodHRwczpcL1wvYXV0aC5ldmlkZW5jZS5jb20iLCJzdWJfdHlwIjoiU3Vic2NyaWJlciIsImF1ZF90eXAiOiJQYXJ0bmVyIiwic3ViX2QiOiJmM2Q3MTliYy1kYjJiLTRiNzEtYmZiMS00MzYyNDBmYjkwOTkiLCJhdWQiOiJmM2Q3MTliYy1kYjJiLTRiNzEtYmZiMS00MzYyNDBmYjkwOTkiLCJzZCI6e30sIm5iZiI6MTU0MTgwNTE1OSwiYXVkX2QiOiJmM2Q3MTliYy1kYjJiLTRiNzEtYmZiMS00MzYyNDBmYjkwOTkiLCJzY29wZXMiOlsiZXZpZGVuY2UuYW55Lmxpc3QiLCJjYXNlcy5hbnkubW9kaWZ5Il0sImV4cCI6MTU0MTgxOTU1OCwiaWF0IjoxNTQxODA1MTU5LCJqdGkiOiI4OTdlMDUyOC0yYjhiLTQyYjItYjg2OS1iOGFiMGFlMDY0MDIifQ.FEzcBjlW9qBoqd9CO65a1pC1G0kLFaPOUtxBNsrQSsc,Bearer,2018-11-09T23:12:39Z,2018-11-10T03:12:38.984Z),AuthToken,Some({\"access_token\":\"2xrypzge7d570p94sw0n8dc05kh9868sclqelvdznew698e5pi\",\"token_type\":\"Bearer\",\"expires_in\":\"14399\",\"expires_on\":\"1541819558984\",\"not_before\":\"1541805159000\",\"version\":\"1\",\"entity\":{\"type\":\"Subscriber\",\"id\":\"db20158d-e501-4f1e-9689-e2edda39068e\",\"partner_id\":\"f3d719bc-db2b-4b71-bfb1-436240fb9099\"}}))"}%
```
```
# With token: 2xrypzge7d570p94sw0n8dc05kh9868sclqelvdznew698e5pi

curl 'http://localhost:9000/media/v0/test/delete-session/2xrypzge7d570p94sw0n8dc05kh9868sclqelvdznew698e5pi'
```
```
# With token: 2xrypzge7d570p94sw0n8dc05kh9868sclqelvdznew698e5pi

curl 'http://localhost:9000/media/v0/test/get-session/2xrypzge7d570p94sw0n8dc05kh9868sclqelvdznew698e5pi'

> {"status":"ok","response":"SessionAuthorization(2xrypzge7d570p94sw0n8dc05kh9868sclqelvdznew698e5pi,EntityDescriptor(Subscriber,DB20158D-E501-4F1E-9689-E2EDDA39068E,Some(F3D719BC-DB2B-4B71-BFB1-436240FB9099)),eyJraWQiOiI1ZjBlZTM4Y2YyYTdkZWVkY2Q5ZTFiNGFjZDYwYTQ3NSIsImFsZyI6IkhTMjU2In0.eyJzdWIiOiJkYjIwMTU4ZC1lNTAxLTRmMWUtOTY4OS1lMmVkZGEzOTA2OGUiLCJ2ZXIiOiIyIiwicm9sZXMiOltdLCJpc3MiOiJodHRwczpcL1wvYXV0aC5ldmlkZW5jZS5jb20iLCJzdWJfdHlwIjoiU3Vic2NyaWJlciIsImF1ZF90eXAiOiJQYXJ0bmVyIiwic3ViX2QiOiJmM2Q3MTliYy1kYjJiLTRiNzEtYmZiMS00MzYyNDBmYjkwOTkiLCJhdWQiOiJmM2Q3MTliYy1kYjJiLTRiNzEtYmZiMS00MzYyNDBmYjkwOTkiLCJzZCI6e30sIm5iZiI6MTU0MTgwNTM2NiwiYXVkX2QiOiJmM2Q3MTliYy1kYjJiLTRiNzEtYmZiMS00MzYyNDBmYjkwOTkiLCJzY29wZXMiOlsiZXZpZGVuY2UuYW55Lmxpc3QiLCJjYXNlcy5hbnkubW9kaWZ5Il0sImV4cCI6MTU0MTgxOTU1OCwiaWF0IjoxNTQxODA1MzY2LCJqdGkiOiIzY2ExYjYxNy1kNWNmLTQyNDAtYjBmMi03MjZhZDI2OTg4MmMifQ.fnYGZnx3Vh96tWwoeq7cbqoDH2gSFDsRI1GkGLmUnNk,Bearer,2018-11-09T23:16:06Z,2018-11-10T03:12:38.984Z)"}%
```
```
# With
#      agencyId: f3d719bc-db2b-4b71-bfb1-436240fb9099
#      evidenceId: 31b4f97f-20cd-40de-acb8-7a6fe95357eb
#      fileId: 4f706842-e602-4287-b80b-a74b09d8995a
  
curl 'http://localhost:9000/media/v0/test/get-url/f3d719bc-db2b-4b71-bfb1-436240fb9099/31b4f97f-20cd-40de-acb8-7a6fe95357eb/4f706842-e602-4287-b80b-a74b09d8995a'

> {"status":"ok","presignedUrlResponse":"https://sds-dev-us3-evidence-com.s3-us-west-2.amazonaws.com/d8995a/a74b09/pa-f3d719bcdb2b4b71bfb1436240fb9099-fi-4f706842e6024287b80ba74b09d8995a.dat?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20181109T231905Z&X-Amz-SignedHeaders=host&X-Amz-Expires=60&X-Amz-Credential=AKIAIMHAU656GYSUD7AQ%2F20181109%2Fus-west-2%2Fs3%2Faws4_request&X-Amz-Signature=0f2ddf1dd137c9de9f393dd5442a2a896ce24ea13e06999d50aed383430055a6"}
```
```
# Get a RTM zookeeper endpoint:

curl 'http://localhost:9000/media/v0/test/get-rtm/something'

> {"status":"ok","result":"ServiceEndpoint(dus1uw2lbox980.taservs.net,8900)"}
```

#### Stream HLS segment with `ffplay`:

```
curl 'http://localhost:9000/media/v0/hls/segment/agency/f3d719bc-db2b-4b71-bfb1-436240fb9099/evidence/31b4f97f-20cd-40de-acb8-7a6fe95357eb/file/4f706842-e602-4287-b80b-a74b09d8995a?level=0&index=0&offset=0&start=0' | ffplay - 
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
