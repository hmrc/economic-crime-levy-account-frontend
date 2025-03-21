# economic-crime-levy-account-frontend

This is the frontend microservice that enables customers to view information about their Economic Crime Levy account
and access other parts of the service such as returns and payments.


## Running dependencies

Using [sm2](https://github.com/hmrc/sm2)
with the service manager profile `ECONOMIC_CRIME_LEVY_ALL` will start
all the Economic Crime Levy microservices as well as the services
that they depend on.

```
sm2 --start ECONOMIC_CRIME_LEVY_ALL
```

To stop the frontend microservice from running on service manager (e.g. to run your own version locally), you can run:

```
sm2 -stop ECONOMIC_CRIME_LEVY_ACCOUNT_FRONTEND 
```


### Using localhost

To run this microservice locally on the configured port **'14008'**, you can run:

```
sbt run -Dplay.http.router=testOnlyDoNotUseInAppConf.Routes
```

**NOTE:** Ensure that you are not running the microservice via service manager before starting your service locally (vice versa) or the service will fail to start


### Accessing the service

Access details can be found on
[DDCY Live Services Credentials sheet](https://docs.google.com/spreadsheets/d/1ecLTROmzZtv97jxM-5LgoujinGxmDoAuZauu2tFoAVU/edit?gid=1186990023#gid=1186990023)
for both staging and local url's or check the Tech Overview section in the
[service summary page ](https://confluence.tools.tax.service.gov.uk/display/ELSY/ECL+Service+Summary)


## Running tests

### Unit tests

> `sbt test`

### Integration tests

> `sbt it:test`

### All tests

This is n sbt command alias specific to this project. It will run a scala format
check, run a scala style check, run unit tests, run integration tests and produce a coverage report.
> `sbt runAllChecks`

## Scalafmt and Scalastyle

To check if all the scala files in the project are formatted correctly:
> `sbt scalafmtCheckAll`

To format all the scala files in the project correctly:
> `sbt scalafmtAll`

To check if there are any scalastyle errors, warnings or infos:
> `sbt scalastyle`

## Feature flags

The following features can be turned on or off by changing configuration flags as follows.
These flags must have a value of 'true' or 'false'.

> `amendRegistrationEnabled`

Enables/disables the amend registration feature.
If this is disabled then the registration tile on the My ECL Account home page will not be visible.

> `amendReturnsEnabled`

Enables/disables the amend returns feature.

> `deregisterEnabled`

Enables/disables the request to deregister link on the main dashboard page.

> `paymentsEnabled`

Enables/disables the payments feature.
If this is disabled then the payments tile on the My ECL Account home page will not be visible.

> `requestRefundEnabled`

Enables/disables the refund feature.
If this is disabled then users will not be able to request a refund.

> `returnsEnabled`

Enables/disables the returns feature.
If this is disabled then the returns tile on the My ECL Account home page will not be visible.

> `welsh-translation`

Enabled/disables Welsh translations.

### License

This code is open source software licensed under
the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").


## Monitoring

The following grafana and kibana dashboards are available for this service:

* [Grafana](https://grafana.tools.production.tax.service.gov.uk/d/economic-crime-levy-account-frontend/economic-crime-levy-account-frontend?orgId=1&from=now-24h&to=now&timezone=browser&var-ecsServiceName=ecs-economic-crime-levy-account-frontend-public-Service-hLAZ5bhk3bGW&var-ecsServicePrefix=ecs-economic-crime-levy-account-frontend-public&refresh=15m)
* [Kibana](https://kibana.tools.production.tax.service.gov.uk/app/dashboards#/view/economic-crime-levy-account-frontend?_g=(filters:!(),refreshInterval:(pause:!t,value:60000),time:(from:now-15m,to:now))

## Other helpful documentation

* [Service Runbook](https://confluence.tools.tax.service.gov.uk/display/ELSY/Economic+Crime+Levy+%28ECL%29+Runbook)

* [Architecture Links](https://confluence.tools.tax.service.gov.uk/pages/viewpage.action?pageId=859504759)