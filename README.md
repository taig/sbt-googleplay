# sbt Google Play plugin

[![CircleCI](https://circleci.com/gh/Taig/sbt-googleplay/tree/master.svg?style=shield)](https://circleci.com/gh/Taig/sbt-googleplay/tree/master)

> Upload signed applications to Google Play with a single command

This sbt plugin relies on [sbt-android][1] to package and sign an `apk` file to then
publish the app via the _Google Play Developer API_.

## Installation

````
addSbtPlugin( "io.taig" % "sbt-googleplay" % "1.1.0-SNAPSHOT" )
````

## Usage

### Authentication

In order to authenticate with the _Google Play Developer API_, you have to create
a service account in the _Google Play Developer Console_ and obtain a P12 service
account key.

> Please note that the permission system for service accounts does not work as 
> expected, so you have to at least grant the following permissions:
> - Edit store listing, pricing & distribution
> - Manage Production APKs
> - Manage Alpha & Beta APKs
> - Manage Alpha & Beta users

```scala
lazy val app = project( ... )
    .enablePlugins( GooglePlayPlugin )
    .settings(
        googlePlayTrack := "alpha",
        googlePlayServiceAccountEmail := System.getenv( "GOOGLE_PLAY_SERVICE_ACCOUNT_EMAIL" ),
        googlePlayServiceAccountKey := file( System.getenv( "GOOGLE_PLAY_SERVICE_ACCOUNT_KEY" ) )
    )
```

### Publishing

When properly configured it is now possible to build, package and publish the app
with a single command:

```
sbt googlePlayPublish
```

### Changelog

It is optionally possible to submit a changelog via the `googlePlayChangelog` task
while publishing the apk.

```scala
googlePlayChangelog := Map(
    "en-US" -> "Fixed all the bugs"
)
```

> Keep in mind to not exceed the 500 character limit

[1]: https://github.com/scala-android/sbt-android/