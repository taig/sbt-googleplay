package io.taig.sbt.googleplay

import java.util.Collections

import android.AndroidPlugin
import android.Keys._
import com.google.api.client.http.FileContent
import com.google.api.services.androidpublisher.model.{ LocalizedText, Track, TrackRelease }
import sbt.Keys._
import sbt._
import sbt.complete.Parsers.spaceDelimited

import collection.JavaConverters._

object GooglePlayPlugin extends AutoPlugin {
    object autoImport extends Keys

    import autoImport._

    override def requires = AndroidPlugin

    override def projectSettings: Seq[Def.Setting[_]] = Seq(
        googlePlayApplication := {
            val path = ( applicationId in Android ).value
            val identifier = name.value
            val version = ( versionName in Android ).value

            s"$path-$identifier/$version"
        },
        googlePlayTrack := "beta",
        googlePlayChangelog := Map.empty,
        googlePlayServiceAccountEmail := {
            sys.error {
                """
                  |Please specify your Google Play API service account email:
                  |
                  |googlePlayServiceAccountEmail := "12345678910@developer.gserviceaccount.com"
                  |
                  |You can set up a service account or find the email address in the Google Play
                  |Developer Console (Settings > API access).
                """.stripMargin.trim
            }
        },
        googlePlayServiceAccountKey := {
            sys.error {
                """
                  |Please specify your Google Play API service account key (P12 file):
                  |
                  |googlePlayServiceAccountKey := file( "./path/to/key.p12" )
                  |
                  |You can set up a service account or find the email address in the Google Play
                  |Developer Console (Settings > API access).
                """.stripMargin.trim
            }
        },
        googlePlayPublish := googlePlayPublishApk.value( ( packageRelease in Android ).value ),
        googlePlayPublishFile := {
            val file = spaceDelimited( "<arg>" ).parsed match {
                case Seq( file ) ⇒ new File( file )
                case _ ⇒ sys.error {
                    """
                      |Invalid input arguments
                      |Valid usage: googlePlayPublishFile file
                      |  file: Path to APK file to publish
                    """.stripMargin.trim
                }
            }

            googlePlayPublishApk.value( file )
        },
        googlePlayPublishApk := { file ⇒
            if ( !file.exists() ) {
                sys.error {
                    s"""
                      |APK file does not exist:
                      |${file.getAbsolutePath}
                    """.stripMargin.trim
                }
            }

            val apkFile = new FileContent( "application/vnd.android.package-archive", file )

            val service = Helper.authorize(
                googlePlayApplication.value,
                googlePlayServiceAccountEmail.value,
                googlePlayServiceAccountKey.value
            )

            val packageName = ( applicationId in Android ).value

            val edits = service.edits()

            val insert = edits.insert( packageName, null ).execute()

            val id = insert.getId

            streams.value.log.info( "Uploading apk file ..." )

            val apk = edits.apks().upload( packageName, id, apkFile ).execute()

            val code = apk.getVersionCode

            streams.value.log.info( s"Version code $code has been uploaded" )

            val track = googlePlayTrack.value

            streams.value.log.info( s"Adding apk to $track track" )

            val changelogs = googlePlayChangelog.value.map {
                case ( locale, changelog ) ⇒ new LocalizedText()
                    .setLanguage( locale )
                    .setText( changelog )
            }

            val apkVersionCodes = List( java.lang.Long.valueOf( apk.getVersionCode.toLong ) )
            val updateTrackRequest = edits
                .tracks
                .update(
                    packageName,
                    id,
                    track,
                    new Track()
                        .setReleases(
                            Collections
                                .singletonList(
                                    new TrackRelease()
                                        .setName( "My Beta Release" )
                                        .setVersionCodes( apkVersionCodes.asJava )
                                        .setStatus( "completed" )
                                        .setReleaseNotes(
                                            changelogs.toList.asJava
                                        )
                                )
                        )
                )
            val updatedTrack = updateTrackRequest.execute
            streams.value.log.info( String.format( "Track %s has been updated.", updatedTrack.getTrack ) )

            streams.value.log.info( "Committing update!" )

            edits.commit( packageName, id ).execute()
        }
    )
}