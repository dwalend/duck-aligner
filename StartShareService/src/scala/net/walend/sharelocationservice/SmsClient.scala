package net.walend.sharelocationservice

import cats.effect.{Async, Sync, Temporal}
import cats.syntax.all.*
import com.amazonaws.sms.SMS
import fs2.io.file.Files
import io.circe.Decoder.Result
import io.circe.DecodingFailure.Reason.CustomReason
import io.circe.{Decoder, DecodingFailure, HCursor}
import org.http4s.{EntityDecoder, ParseFailure, Request, Response, Uri}

import scala.util.Try
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.Method.GET
import org.http4s.circe.jsonOf
import smithy4s.aws.{AwsClient, AwsEnvironment, AwsRegion}

/**
 * Client for sending SMS messages via AWS' service.
 */

trait SmsClient[F[_]]:
  def send = ???

object SmsClient:
  def apply[F[_]](using ev:SmsClient[F]): SmsClient[F] = ev

  def smsClient[F[_]: Files : Temporal: Async](client: Client[F]):SmsClient[F] = new SmsClient[F] :
    AwsEnvironment.default(client,AwsRegion.US_EAST_1).use { env =>
      AwsClient(com.amazonaws.sms.SMS, env).use { sms =>
        sms.
      }
     ???
    }
    ???


