# economic-crime-levy-account-frontend

This is the frontend microservice that enables customers to view information about their Economic Crime Levy account
and access other parts of the service such as returns and payments.

## Running the service

> `sbt run -Dplay.http.router=testOnlyDoNotUseInAppConf.Routes`

The service runs on port `14008` by default.

## Running dependencies

Using [service manager](https://github.com/hmrc/service-manager)
with the service manager profile `ECONOMIC_CRIME_LEVY_ALL` will start
all of the Economic Crime Levy microservices as well as the services
that they depend on.

> `sm --start ECONOMIC_CRIME_LEVY_ALL`

## Running tests

### Unit tests

> `sbt test`

### Integration tests

> `sbt it:test`

### All tests

This is an sbt command alias specific to this project. It will run a scala format
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