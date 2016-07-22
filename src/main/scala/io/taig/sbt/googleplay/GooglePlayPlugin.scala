package io.taig.sbt.googleplay

import android.AndroidPlugin
import android.Keys._
import com.google.api.client.http.FileContent
import com.google.api.services.androidpublisher.model.{ ApkListing, Track }
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
        googlePlayPublishApk := { file ⇒
            if ( !file.exists() ) {
                sys.error {
                    s"""
                      |APK file does not exist:
                      |${file.getAbsolutePath}
                    """.stripMargin.trim
                }
            }

            val apk = new FileContent( "application/vnd.android.package-archive", file )

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

            val upload = edits.apks().upload( packageName, id, apk ).execute()

            val code = upload.getVersionCode

            streams.value.log.info( s"Version code $code has been uploaded" )

            val track = googlePlayTrack.value

            streams.value.log.info( s"Adding apk to $track track" )

            edits
                .tracks()
                .update(
                    packageName,
                    id,
                    track,
                    new Track().setVersionCodes( List( code ) )
                )
                .execute()

            googlePlayChangelog.value.foreach {
                case ( locale, changelog ) ⇒
                    streams.value.log.info( s"Uploading changelog for $locale" )

                    val listing = new ApkListing()
                    listing.setRecentChanges( changelog )

                    edits
                        .apklistings()
                        .update(
                            packageName,
                            id,
                            code,
                            locale,
                            listing
                        )
                        .execute()
            }

            streams.value.log.info( "Committing update!" )

            edits.commit( packageName, id ).execute()
        }
    )
}