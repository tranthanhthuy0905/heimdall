# Heimdall - Gatekeeper &amp; Load Balancer for On Demand Media Streaming

## Naming
Heimdall the Gatekeeper is a servant of All-Father and ally of Thor. He was the guardian of Asgard, standing on the rainbow bifrost bridge and denying access to any unauthorised beings.

## Documentation

- [Quip Heimdall folder](https://axon.quip.com/folder/heimdall)
- [Heimdall runbook](https://git.taservs.net/ecom/heimdall/wiki/Heimdall-Run-Book)  

### Run Book

Run Book is located in https://git.taservs.net/ecom/heimdall/wiki/Heimdall-Run-Book and its duplicate is stored in Quip https://axon.quip.com/hSYKA9Y9c09T/Heimdall-Run-Book.

### Heimdall dependencies

![heimdall-dependencies](https://git.taservs.net/ecom/heimdall/blob/master/docs/diagrams/heimdall-dependencies.svg)

#### Updating diagrams

Sources and generated diagrams can be found in [`./docs/diagrams`](https://git.taservs.net/ecom/heimdall/tree/master/docs/diagrams) of the repository.<br/>

There are two diagrams:
* Heimdall Dependencies diagram reflecting Heimdall's direct relationships (`heimdall-dependencies.dot`).
* Heimdall HLS sequence diagram reflecting flow of Heimdall calls to other services during HLS (`heimdall-hls-work-flow.sequence`).

The Heimdall Dependencies diagram uses [The DOT Language](https://www.graphviz.org/doc/info/lang.html) syntax.<br/>
 `heimdall-dependencies.svg` is generated out of `heimdall-dependencies.dot` by running:
```
dot -Tsvg heimdall-dependencies.dot > heimdall-dependencies.svg
``` 

HLS work flow diagram uses syntax and powered by https://www.websequencediagrams.com/. 
In order to update `heimdall-hls-work-flow`, use WebWequenceDiagrams' editor and SVG generator.

### API Documentation
Heimdall has [`./docs/heimdall.yaml`](https://git.taservs.net/ecom/heimdall/tree/master/docs/diagrams/heimdall.yaml) is a template for using the SwaggerUI to dynamically generate beautiful API documentations. The API specification of Heimdall can seen hosted [here](https://git.taservs.net/pages/ecom/heimdall).  

## Run and Debug Heimdall Locally

This instruction explains how to hit Heimdall directly and have it to contact QA/Dev services.
I.e. the requests will not hit `nginx` and have it forward traffic to the local Heimdall.
Forwarding requests though `nginx` will require changes to `nginx` config in Dev or QA.

**1. Update application config**

Copy `/opt/heimdall/heimdall/conf/application.conf` from heimdall host of an environment that the local 
instance of heimdall is going to connect to.<br/>
Replace `./conf/env/localdev.conf` with `application.conf`.<br/>
Update `api_prefix` in `./conf/env/localdev.conf` as following: `heimdall.api_prefix = ""`.

**2. Run heimdall**

```
./bin/sbt run
```
Endpoint named `/media/alive` can be requested by `curl` or simply through the browser: <http://localhost:9000/media/alive>.

**3. Using Heimdall API**

```
export FILEID=815fc051-e956-4ea8-aa08-f634338d3a95
export EVIDENCEID=edd113b6-74ef-4f58-919a-38567f8ff88c
export PARTNERID=48f4901b-3d7b-4fb9-9ed3-54c596d052bf
export USERID=f081396a-b496-40e7-bfdd-1f33f9f8bcef

# Log into your agency and copy AXONSESSION cookie:
#
export COOKIE=vqdzw5fx0folp8ljdmlqpsnp0fb64b6sttdx7exgwtfclxd67

# Run probe request to generate streamingSessionToken: 
#
curl -v --cookie "AXONSESSION=$COOKIE" "http://localhost:9000/media/start?file_id=$FILEID&evidence_id=$EVIDENCEID&partner_id=$PARTNERID&level=0&index=0&offset=0"

# Save streamingSessionToken:
#
export SSTOKEN=db6cMxwE7m6ALKsfBByG7mNpCEZZsKCGvxBohfOUn9c=

# Use cookie and streamingSessionToken to start a HLS playback session:
#
ffplay "http://localhost:9000/media/hls/master?file_id=$FILEID&evidence_id=$EVIDENCEID&partner_id=$PARTNERID&offset=0&streamingSessionToken=$SSTOKEN" -headers "Cookie: AXONSESSION=$COOKIE"


# Variant:
#
ffplay "http://localhost:9000/media/hls/variant?file_id=$FILEID&evidence_id=$EVIDENCEID&partner_id=$PARTNERID&offset=0&level=0&streamingSessionToken=$SSTOKEN" -headers "Cookie: AXONSESSION=$COOKIE"

# Segment:
#
curl -v --cookie "AXONSESSION=$COOKIE" "http://localhost:9000/media/hls/segment?file_id=$FILEID&evidence_id=$EVIDENCEID&partner_id=$PARTNERID&level=0&index=0&offset=0&start=0&fast=1&label=AWESOME_LABEL&streamingSessionToken=$SSTOKEN" | ffplay -

# Thumbnail:
#
ffplay "http://localhost:9000/media/thumbnail?file_id=$FILEID&evidence_id=$EVIDENCEID&partner_id=$PARTNERID&time=57&width=120&height=52&autorotate=true&streamingSessionToken=$SSTOKEN" -headers "Cookie: AXONSESSION=$COOKIE"

```
## Heimdall Anatomy
```
├── app                         → Application sources
│   ├── controllers             → Heimdall controllers
│   ├── filters                 → Heimdall's custom filters 
│   ├── models                  → Application business layer
│   │   ├── auth                → Axon request authorization model
│   │   ├── common
│   │   └── hls
│   ├── services                → Heimdall's dependencies 
│   │   ├── audit               → Audit client
│   │   ├── dredd               → Dredd client
│   │   ├── global              → Heimdall application lifecycle
│   │   ├── komrade             → Komrade client
│   │   ├── rtm                 → RTM client
│   │   ├── sessions            → Sessions-service client 
│   │   └── zookeeper
│   └── utils
├── build.sbt                   → Application build script
├── build.sh                    → TeamCity build, test, and packing script
├── common.sbt                  → Common settings
├── conf                        → Configuration files and other non-compiled resources (on classpath)
│   ├── application.conf        → Main configuration file
│   ├── env                     → Environment configuration directory
│   │   ├── key_manager.conf    → Definitions of edc key values, IDs, and types
│   │   ├── localdev.conf       → Development configuration
│   │   └── test.conf           → Currently unused, meant for unit testing configuration
│   ├── filters.conf            → Play framework filters, such as CSRFFilter, AllowedHostFilters, and SecurityHeadersFilters
│   ├── logback.xml             → Logs configuration file
│   ├── reference.conf          → Default settings, reference.conf files are aggregated together at runtime, and they are overridden by any settings defined in the application.conf
│   └── routes                  → Routes definitions
├── doc
│   └── run-book.md             → Heimdall's Run Book
├── project
│   ├── Common.scala            → Common settings or code of the root projects
│   ├── ConfigurationHook.scala → Hooking into Play’s dev mode
│   ├── build.properties        → Marker for sbt project
│   └── plugins.sbt             → sbt plugins including the declaration for Play itself
├── test                        → Source folder for unit or functional tests
├── tests.sh
└── version.sbt                 → Definition of the service version
```
