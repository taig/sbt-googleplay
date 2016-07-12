package io.taig.sbt.googleplay

import java.io.File

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.androidpublisher.{ AndroidPublisher, AndroidPublisherScopes }

import scala.collection.JavaConversions._

object Helper {
    lazy val HttpTransport = GoogleNetHttpTransport.newTrustedTransport

    lazy val JsonFactory = JacksonFactory.getDefaultInstance

    def authorize( applicationName: String, serviceAccount: String, serviceAccountPrivateKey: File ): AndroidPublisher = {
        val credentials = new GoogleCredential.Builder()
            .setTransport( HttpTransport )
            .setJsonFactory( JsonFactory )
            .setServiceAccountId( serviceAccount )
            .setServiceAccountScopes( List( AndroidPublisherScopes.ANDROIDPUBLISHER ) )
            .setServiceAccountPrivateKeyFromP12File( serviceAccountPrivateKey )
            .build

        new AndroidPublisher.Builder( HttpTransport, JsonFactory, credentials )
            .setApplicationName( applicationName )
            .build
    }
}