package net.walend.sharelocationservice.log

import cats.arrow.FunctionK
import cats.data.OptionT
import cats.effect.{Async, MonadCancelThrow}
import cats.~>
import org.http4s.{Headers, Http, HttpApp, HttpRoutes}
import org.http4s.server.middleware.RequestLogger
import org.typelevel.ci.CIString
import org.typelevel.log4cats
import cats.syntax.all.*

object Logger :

  private def defaultRedactHeadersWhen(name: CIString): Boolean =
    Headers.SensitiveHeaders.contains(name) || name.toString.toLowerCase.contains("token")

  def apply[G[_], F[_]](
                         logHeaders: Boolean,
                         logBody: Boolean,
                         fk: F ~> G,
                         redactHeadersWhen: CIString => Boolean = defaultRedactHeadersWhen,
                         logAction: Option[String => F[Unit]] = None,
                       )(http: Http[G, F])(implicit G: MonadCancelThrow[G], F: Async[F]): Http[G, F] = {
    val logger = log4cats.slf4j.Slf4jFactory.create[F].getLogger
    val log: String => F[Unit] = logAction.getOrElse { s =>
      logger.info(s)
    }
    ResponseLogger(logHeaders, logBody, fk, redactHeadersWhen, log.pure[Option])(
      RequestLogger(logHeaders, logBody, fk, redactHeadersWhen, log.pure[Option])(http)
    )
  }

  def httpApp[F[_]: Async](
                            logHeaders: Boolean,
                            logBody: Boolean,
                            redactHeadersWhen: CIString => Boolean = defaultRedactHeadersWhen,
                            logAction: Option[String => F[Unit]] = None,
                          )(httpApp: HttpApp[F]): HttpApp[F] =
    apply(logHeaders, logBody, FunctionK.id[F], redactHeadersWhen, logAction)(httpApp)

  def httpRoutes[F[_]: Async](
                               logHeaders: Boolean,
                               logBody: Boolean,
                               redactHeadersWhen: CIString => Boolean = defaultRedactHeadersWhen,
                               logAction: Option[String => F[Unit]] = None,
                             )(httpRoutes: HttpRoutes[F]): HttpRoutes[F] =
    apply(logHeaders, logBody, OptionT.liftK[F], redactHeadersWhen, logAction)(httpRoutes)