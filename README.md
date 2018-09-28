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
Copy thrift secret from a dredd-service linux box (/opt/dredd-service/dredd-service/conf/application.conf),
that heimdall is going to communicate with.
```
edc.service.dredd {
  thrift_auth_secret = "05FF04474A8B4049AB59579FA9469272" # secret from dbox980
...
}
```

* Start the service
```
./bin/sbt run
```

* See the application in browser, for example:
```
http://localhost:9000/api/v1/hls/agencies/f3d719bc-db2b-4b71-bfb1-436240fb9099/evidence/31b4f97f-20cd-40de-acb8-7a6fe95357eb/files/4f706842-e602-4287-b80b-a74b09d8995a
```
Note [2018-09-26]: agency, evidence, and file ID-s must be provided in UUID format, including '-'.<br/>
I.e. XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX.


