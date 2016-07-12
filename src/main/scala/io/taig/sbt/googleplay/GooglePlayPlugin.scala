package io.taig.sbt.googleplay

import android.AndroidPlugin
import android.Keys._
import com.google.api.client.http.FileContent
import com.google.api.services.androidpublisher.model.Track
import sbt.Keys._
import sbt._

import scala.collection.JavaConversions._

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
        googlePlayServiceAccountEmail := {
            sys.error {
                """
                  |Please specify your Google Play API service account email:
                  |
                  |googlePlayServiceAccountEmail := "12345678910@developer.gserviceaccount.com"
                  |
                  |You can set up a service account or find the email address in the Google Play Developer
                  |Console (Settings > API access).
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
                  |You can set up a service account or find the email address in the Google Play Developer
                  |Console (Settings > API access).
                """.stripMargin.trim
            }
        },
        googlePlayPublish := {
            val service = Helper.authorize(
                googlePlayApplication.value,
                googlePlayServiceAccountEmail.value,
                googlePlayServiceAccountKey.value
            )

            val edits = service.edits()

            val insert = edits
                .insert( ( applicationId in Android ).value, null )
                .execute()

            val id = insert.getId

            streams.value.log.info( s"Created Google Play edit with id $id" )

            val file = ( packageRelease in Android ).value
            val apk = new FileContent( "application/vnd.android.package-archive", file )

            streams.value.log.info( "Uploading apk file ..." )

            val upload = edits
                .apks()
                .upload( ( applicationId in Android ).value, id, apk )
                .execute()

            val versionCode = upload.getVersionCode

            streams.value.log.info( s"Version code $versionCode has been uploaded" )

            val track = googlePlayTrack.value

            edits
                .tracks()
                .update(
                    ( applicationId in Android ).value,
                    id,
                    track,
                    new Track().setVersionCodes( List( versionCode ) )
                )
                .execute()

            streams.value.log.info( s"Added apk to $track track" )

            edits.commit( ( applicationId in Android ).value, id ).execute()
        }
    )
}