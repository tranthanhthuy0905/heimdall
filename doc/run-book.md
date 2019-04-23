# Heimdall Run Book

_Document Revision Date: 2019-04-23_ 

## Service overview

**Service name:** Heimdall.

**Description:** Gatekeeper & Load Balancer for On Demand Media Streaming.

### Business overview

> What business need is met by this service or system? What expectations do we have about availability and performance?

Replaces [existing load balancer](https://git.taservs.net/ecom/ecomsaas/blob/master/wc/com.evidence/com.evidence/transcode/Odt2BackendResolver.cs) for RTM, aiming to reduce fleet and operational costs 
by moving toward microservices architecture. 
Heimdall is meant to serve as a proxy for on-demand media streaming service.

### Technical overview

> What kind of system is this? Web-connected order processing? Back-end batch system? Internal HTTP-based API? ETL control system?

Internal REST API for on-demand HLS, thumbnails generation, and submitting streaming related audit events. 
Heimdall is written in Scala, based on Play Framework (https://www.playframework.com).

Heimdall determines which ODT2/RTM URL should be used for a given key, where the key is an evidence `file_id`.<br/>
It discovers available RTM servers via zookeepers.<br/>
Heimdall employs [Consistent Hashing load balancing algorithm](https://git.taservs.net/ecom/service-common/blob/master/service-common-zookeeper/src/main/scala/com/evidence/service/common/zookeeper/ConsistentHashLb.scala) implemented in the service-common library.

The basic idea is that each RTM server is mapped to a point on a circle with a hash function.<br/>
This allows to dynamically add and remove RTM servers without any or major interruptions. 

Heimdall is not forced to apply any advanced load balancing techniques. 
The reason for it is that RTM uses range cacher for downloading media. 
This means that RTM does not download complete files, but instead downloads only ranges of interest for transcoding, 
which gives its gatekeeper freedom to use any reasonable load balancing technique.

### Service owner

> Which team owns and runs this service or system?

**Slack:** _#video-pipeline-ask, #platform-ask_

**Engineering:** _Aaron Boushley, Olga Kroshkina_ 

### Contributing applications, daemons, services, middleware

> Which distinct software applications, daemons, services, etc. make up the service or system? What external dependencies does it have?

#### Internal Dependencies

* **audit-service** for recording audit events in Cassandra.
GitHub: https://git.taservs.net/ecom/audit-service
* **dredd-service** for blob-storage presigned media URL retrieval.
GitHub: https://git.taservs.net/ecom/dredd-service
* **komrade-service** for retrieval of user and partner information for watermark
 generation. GitHub: https://git.taservs.net/ecom/komrade-service
* **nino** for `View` and `Stream` permissions verification.
GitHub: https://git.taservs.net/ecom/nino
* **real-time media (RTM)** service for transcoding, watermark generation, 
previews/thumbnails extraction, and media metadata retrieval.
 GitHub: https://git.taservs.net/ecom/rtm
* **sessions** for request authorization. GitHub: https://git.taservs.net/ecom/sessions
* **service-common** https://git.taservs.net/ecom/service-common
* **service-thrift** https://git.taservs.net/ecom/service-thrift 

![heimdall-dependencies](https://git.taservs.net/ecom/heimdall/blob/master/doc/heimdall-dependencies.svg)

The Heimdall Dependencies diagram uses [The DOT Language](https://www.graphviz.org/doc/info/lang.html) syntax.<br/>
 
#### External Dependencies

* **axon-ui** requests for media playback using `/api/v1/media/*` routes. 
GitHub: https://git.taservs.net/axon/axon-ui  
* **lantern** supplies Axon UI with dial value defining percentage of traffic going to Heimdall. 
GitHub: https://git.taservs.net/ecom/lantern 
* **nginx** routes traffic directly to Heimdall from Axon UI. See puppet [configuration](https://git.taservs.net/ops/puppet/blob/844a753b5cd82b9dd09522fa3d8fb79db9d545f5/modules/nginx/templates/normal.erb#L571).
* **puppet** defines Heimdall version and configuration per environment, 
in charge of service deployments and updates. GitHub: https://git.taservs.net/ops/puppet

## System characteristics

### Hours of operation

> During what hours does the service or system actually need to operate? Can portions or features of the system be unavailable at times if needed?

Heimdall is expected to work 24/7 with no interruptions.

### Data and processing flows

> How and where does data flow through the system? What controls or triggers data flows?

Heimdall processes HLS, thumbnails, audit requests passed by Axon UI to nginx.

![heimdall-hls-work-flow](https://git.taservs.net/ecom/heimdall/blob/master/doc/heimdall-hls-work-flow.svg)

The Heimdall HLS Work Flow diagram uses syntax and powered by https://www.websequencediagrams.com/.
 
### Infrastructure and network overview

|                  |QA|US2|BR1|EU1|
|:-----------------|:---:|:---:|:---:|:---:|
|activation date   |[2019/03/28](https://git.taservs.net/ops/puppet/pull/7272)|[2019/04/16](https://git.taservs.net/ops/puppet/pull/7556)|ETA: 2019/04/26-2019/05/03|
|hostname          |qus1uw2lhdl001|sus2uw1lhdl001|pbr1se1lhdl001|-|
|num of instances  |1|1|1|-|
|cloud             |AWS|Azure|AWS|AWS|
|instance type     |m3.large|Standard_DS3_v2|[TBD](https://taserintl.atlassian.net/browse/SP1-1827)|-|
|terraform PR #     |[1180](https://git.taservs.net/ops/terraform/pull/1180)|[1250](https://git.taservs.net/ops/terraform/pull/1250)|-|-|
|port #            |9438|9438|9438|9438|
|debug port #      |9448|9448|9448|9438|

### Expected traffic and load

> Details of the expected throughput/traffic: call volumes, peak periods, quiet periods. What factors drive the load: bookings, page views, number of items in Basket, etc.

Max: 210 requests per second - Weekdays @ 06:00 to 15:00 PST.<br/>
See [Splunk on-demand media requests stats](https://splunk.taservs.net/en-US/app/search/odt_ecomsaas_stats?earliest=-3d&latest=now&form.environment=pus1) 
for more information.

#### Hot or peak periods

Weekdays @ 07:00 to 14:00, Pacific Time.

#### Warm periods

Weekdays @ 03:00 to 07:00 and 15:00 to 18:00, Pacific Time.

#### Cool or quiet periods

Weekends and weekdays at the afternoon @ 18:00 to 03:00, Pacific Time.

## System configuration

### Configuration management

> How is configuration managed for the system?

Puppet drives all system and application level configuration 
`/opt/heimdall/heimdall/conf/application.conf`
and log configuration
`/opt/heimdall/heimdall/etc/logback.xml`.

### Secrets management

> How are configuration secrets managed?

Secrets are managed per environment by 
[puppet](https://git.taservs.net/ops/puppet/search?q=heimdall_app_secret&unscoped_q=heimdall_app_secret).

## Monitoring and alerting

### Log aggregation solution, metrics and health checks

|                  |QA|US2|
|:-----------------|:---:|:---:|
|nagios/thruk board|_[qus1uw2lhdl001](https://thruk.taservs.net/thruk/cgi-bin/extinfo.cgi?type=1&host=qus1uw2lhdl001)_|_[sus2uw1lhdl001](https://thruk.us2.taservs.net/thruk/cgi-bin/extinfo.cgi?type=1&host=sus2uw1lhdl001)_|
|datadog board     |_[heimdall board](https://app.datadoghq.com/dashboard/vhv-v2h-3ty/heimdall)_||
|splunk board      |_[heimdall qa info](https://dus1uw2lspm001.taservs.net/en-US/app/search/heimdall_qa)_|_[heimdall us2 info](https://splunk.taservs.net/en-US/app/search/heimdall?earliest=-24h&latest=now&form.environment=sus2)_|

## Operational tasks

### Deployment

> How is the software deployed? How does roll-back happen?

Heimdall follows the Axon standard process described in https://axon.quip.com/dntdA2C4L3tw/Evidencecom-Release-Procedure.

### Routine and sanity checks

> What kind of checks need to happen on a regular basis?

:white_check_mark: Nagios dashboards are clear. See the list of checks performed by Nagios:
* Query `/media/alive` endpoint to confirm that the service is up and running ([heimdall_heartbeat](https://git.taservs.net/ops/puppet/blob/72f82ad44ee77ca5bed71d476b03403aa0fa8c9f/modules/nagios/templates/services.cfg#L3638)).
* Check that heimdall process is up ([heimdall_process](https://git.taservs.net/ops/puppet/blob/72f82ad44ee77ca5bed71d476b03403aa0fa8c9f/modules/nagios/templates/services.cfg#L3630)).
* Check that heimdall's port is open ([heimdall_service_port](https://git.taservs.net/ops/puppet/blob/72f82ad44ee77ca5bed71d476b03403aa0fa8c9f/modules/nagios/templates/services.cfg#L3615)).

:white_check_mark: Load testing.

Basic load testing for heimdall is introduced with the [heimdall_loadtest.py](https://git.taservs.net/ecom/lantern/blob/master/integrationtests/loadtests/heimdall_loadtest.py) script.
See also [ecom_LoadTests_HeimdallLoadTest TeamCity job](https://teamcity.taservs.net/viewLog.html?buildId=635243&buildTypeId=ecom_LoadTests_HeimdallLoadTest).

Currently, load testing is under development. However, the script allows to test the service in QA while being executed locally.

:white_check_mark: audit-service is up, can be discovered and connected to.

:white_check_mark: dredd-service is up, can be discovered and connected to.

:white_check_mark: komrade-service is up, can be discovered and connected to.

:white_check_mark: nino is up, can be discovered and connected to.

:white_check_mark: RTM is up, can be discovered and connected to.

:white_check_mark: sessions-service is up, can be discovered and connected to.

:x: **TODO** Implement and set-up integration nightly tests.

### Troubleshooting

> How should troubleshooting happen? What tools are available?

* Check nagios dashboards.
* Check the `Error Rate` splunk dashboard in [qa](https://dus1uw2lspm001.taservs.net/en-US/app/search/heimdall_qa) or in [staging and prod](https://splunk.taservs.net/en-US/app/search/heimdall).
* Check DataDog [dashboard](https://app.datadoghq.com/dashboard/vhv-v2h-3ty/heimdall).
* While being connected to VPN, `ssh` to the host and check logs `/var/log/heimdall/heimdall`.

## Maintenance tasks

### Patching

> How should patches be deployed and tested?

#### Normal patch cycle

Use the standard [Evidence.com release cycle](https://axon.quip.com/dntdA2C4L3tw/Evidencecom-Release-Procedure) together with deployment via puppet.
 
### Log rotation

> Is log rotation needed? How is it controlled? 

* Logs are rotated and removed automatically. Heimdall uses Axon Scala services [standard](https://git.taservs.net/ops/puppet/blob/af1a4c2c35f4457cbc6d5ab68432c91bb0777110/modules/heimdall/manifests/init.pp#L33) configuration:
    * Logs roll over at [500MB](https://git.taservs.net/ops/puppet/blob/master/modules/armory/templates/logback.xml.erb#L15).
    * Logs have a size cap of [2GB](https://git.taservs.net/ops/puppet/blob/master/modules/armory/templates/logback.xml.erb#L18) per service per instance.
    * Logs use the new JSON format.
    * Logs are ingested into Splunk.
    * Warnings are not logged as part of normal operation.
    * Debug logging is `DEBUG` level.

## Operational procedures

### Disabling Heimdall

Heimdall usage is controlled by puppet. There are two variables defining if the service is enabled:

* `heimdall_webservers` which defaults to an [empty array](https://git.taservs.net/ops/puppet/blob/d6a0e2adc114a2b3995056390da09437f01d5737/hieradata/defaults.yaml#L552).<br/>
* `heimdall_usage_dial` which defaults to [0](https://git.taservs.net/ops/puppet/blob/d6a0e2adc114a2b3995056390da09437f01d5737/hieradata/defaults.yaml#L551).

**In order to disabled heimdall, `heimdall_usage_dial` must be set to `0` in puppet master and/or release branches and deployed to wanted environments.**

Note, that the `heimdall_webservers` being empty will disable heimdall as well.
It is *not* recommended to employ this option for controlling the service.

See also [Evidencecom-Release-Approval-Process](https://axon.quip.com/1eBHACxZsbqf/Evidencecom-Release-Approval-Process) quip document for information about releases outside of the standard cycle.<br/>

If heimdall presents in environment and enabled, there is another mechanism to avoid using it.<br/>
The [AVOIDHEIMDALLUSAGE](https://git.taservs.net/ecom/migrations/blob/9f530a9ae94ac2e41de4e475175c0d89e8135bc3/migrations/tvs_partner/V20190300.0.20190306015225__add_heimdall_feature_flag.sql#L10) feature flag dictates to Axon UI for which agencies heimdall should not be enabled.<br/>

The `heimdall_usage_dial` and `heimdall_usage_dial` variables are available during heimdall's production launch and stabilization stages.
Once the service is stable and fully functional, currently existing EcomSaaS load balancer will be deprecated together with the flags.

### Forcible flags

Axon UI provides `forceHeimdall` flag, which can accept two values: `on` or `off`. 

The `forceHeimdall` flag is helpful for testing with or without Heimdall.
By adding `&forceHeimdall=off` to the URL in QA or development environment, it allows to ensure that no regressions are introduced.
 
When initially deploying the service or a fix while `heimdall_usage_dial` is set to 0, addition of the `&forceHeimdall=on` 
flag to the URL allows to use the UI to test Heimdall before shifting user traffic to it.

Once confirmed that Heimdall works as expected, `heimdall_usage_dial` can be adjusted.
 
### Testing checklist

:white_check_mark: Locally executed unit tests succeed (`./bin/sbt test`).<br/>
:white_check_mark: TeamCity build and tests succeed: https://teamcity.taservs.net/viewType.html?buildTypeId=ecom_Services_Heimdall.<br/>
:white_check_mark: `media/alive` returns `200` http status.<br/>
:white_check_mark: `media/start` for single evidence works as expected.<br/>
:white_check_mark: `media/hls/master` for single evidence works as expected.<br/>
:white_check_mark: `media/hls/variant` for single evidence works as expected.<br/>
:white_check_mark: `media/hls/segment` for single evidence works as expected.<br/>
:white_check_mark: `media/thumbnail` for single evidence works as expected.<br/>
:white_check_mark: `media/streamed` works as expected.<br/>
:white_check_mark: `media/start` for multicam mode works as expected.<br/>
:white_check_mark: `media/hls/master` for multicam mode works as expected.<br/>
:white_check_mark: `media/hls/variant` for multicam mode works as expected.<br/>
:white_check_mark: `media/hls/segment` for multicam mode works as expected.<br/>
:white_check_mark: `media/thumbnail` for multicam mode works as expected.<br/>
