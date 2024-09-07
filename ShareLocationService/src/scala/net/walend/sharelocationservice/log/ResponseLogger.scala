package net.walend.sharelocationservice.log

import cats.arrow.FunctionK
import cats.data.{Kleisli, OptionT}
import cats.effect.{Async, MonadCancelThrow, Outcome}
import cats.~>
import fs2.{Chunk, Pipe, Stream}
import org.http4s.{Headers, Response}
import org.http4s.server.middleware.Logger
import org.typelevel.ci.CIString
import org.typelevel.log4cats
import cats.effect.syntax.all.*
import cats.syntax.all.*

/**
 *
 *
 * @author David Walend
 * @since v0.0.0
 */
object ResponseLogger {
  def apply[G[_], F[_], A](
                            logHeaders: Boolean,
                            logBody: Boolean,
                            fk: F ~> G,
                            redactHeadersWhen: CIString => Boolean = Headers.SensitiveHeaders.contains,
                            logAction: Option[String => F[Unit]] = None,
                          )(
                            http: Kleisli[G, A, Response[F]]
                          )(implicit G: MonadCancelThrow[G], F: Async[F]): Kleisli[G, A, Response[F]] =
    impl[G, F, A](logHeaders, Left(logBody), fk, redactHeadersWhen, logAction)(http)

  private[log] def impl[G[_], F[_], A](
                                           logHeaders: Boolean,
                                           logBodyText: Either[Boolean, Stream[F, Byte] => Option[F[String]]],
                                           fk: F ~> G,
                                           redactHeadersWhen: CIString => Boolean,
                                           logAction: Option[String => F[Unit]],
                                         )(
                                           http: Kleisli[G, A, Response[F]]
                                         )(implicit G: MonadCancelThrow[G], F: Async[F]): Kleisli[G, A, Response[F]] = {
    val logger = log4cats.slf4j.Slf4jFactory.create[F].getLogger
    val fallback: String => F[Unit] = s => logger.info(s)
    val log = logAction.fold(fallback)(identity)

    def logMessage(resp: Response[F]): F[Unit] =
      logBodyText match {
        case Left(bool) =>
          Logger.logMessage[F, Response[F]](resp)(logHeaders, bool, redactHeadersWhen)(log(_))
        case Right(f) =>
          org.http4s.internal.Logger
            .logMessageWithBodyText(resp)(logHeaders, f, redactHeadersWhen)(log(_))
      }

    val logBody: Boolean = logBodyText match {
      case Left(bool) => bool
      case Right(_) => true
    }

    def logResponse(response: Response[F]): F[Response[F]] =
      if (!logBody)
        logMessage(response)
          .as(response)
      else
        F.ref(Vector.empty[Chunk[Byte]]).map { vec =>
          val newBody = Stream.eval(vec.get).flatMap(v => Stream.emits(v)).unchunks
          // Cannot Be Done Asynchronously - Otherwise All Chunks May Not Be Appended Previous to Finalization
          val logPipe: Pipe[F, Byte, Byte] =
            _.observe(_.chunks.flatMap(c => Stream.exec(vec.update(_ :+ c))))
              .onFinalizeWeak(logMessage(response.withBodyStream(newBody)))

          /** Copied this private method from org.http4s.Message
           * 
           * Applies the given pipe to the entity body (byte-stream) of this message.
           *
           * WARNING: this method does not modify the headers of the message, and as
           * a consequence headers may be incoherent with the body.
           */
          def pipeBodyThrough(response: Response[F])(pipe: Pipe[F, Byte, Byte]): Response[F] =
            response.withBodyStream(pipe(response.body))

          pipeBodyThrough(response:Response[F])(logPipe)
        }

    def unpackExceptionMessage(t:Throwable):String =
      s"${t.getClass} ${t.getMessage}".appendedAll(Option(t.getCause).map{c =>
        s"\n caused by ${unpackExceptionMessage(c)}"}.getOrElse(""))

    Kleisli[G, A, Response[F]] { req =>
      http(req)
        .flatMap((response: Response[F]) => fk(logResponse(response)))
        .guaranteeCase {
          case Outcome.Errored(t) => fk(log(s"service threw ${unpackExceptionMessage(t)}"))
          case Outcome.Canceled() => fk(log(s"service canceled response for request"))
          case Outcome.Succeeded(_) => G.unit
        }
    }
  }

  def httpApp[F[_]: Async, A](
                               logHeaders: Boolean,
                               logBody: Boolean,
                               redactHeadersWhen: CIString => Boolean = Headers.SensitiveHeaders.contains,
                               logAction: Option[String => F[Unit]] = None,
                             )(httpApp: Kleisli[F, A, Response[F]]): Kleisli[F, A, Response[F]] =
    apply(logHeaders, logBody, FunctionK.id[F], redactHeadersWhen, logAction)(httpApp)

  def httpAppLogBodyText[F[_]: Async, A](
                                          logHeaders: Boolean,
                                          logBody: Stream[F, Byte] => Option[F[String]],
                                          redactHeadersWhen: CIString => Boolean = Headers.SensitiveHeaders.contains,
                                          logAction: Option[String => F[Unit]] = None,
                                        )(httpApp: Kleisli[F, A, Response[F]]): Kleisli[F, A, Response[F]] =
    impl[F, F, A](logHeaders, Right(logBody), FunctionK.id[F], redactHeadersWhen, logAction)(
      httpApp
    )

  def httpRoutes[F[_]: Async, A](
                                  logHeaders: Boolean,
                                  logBody: Boolean,
                                  redactHeadersWhen: CIString => Boolean = Headers.SensitiveHeaders.contains,
                                  logAction: Option[String => F[Unit]] = None,
                                )(httpRoutes: Kleisli[OptionT[F, *], A, Response[F]]): Kleisli[OptionT[F, *], A, Response[F]] =
    apply(logHeaders, logBody, OptionT.liftK[F], redactHeadersWhen, logAction)(httpRoutes)

  def httpRoutesLogBodyText[F[_]: Async, A](
                                             logHeaders: Boolean,
                                             logBody: Stream[F, Byte] => Option[F[String]],
                                             redactHeadersWhen: CIString => Boolean = Headers.SensitiveHeaders.contains,
                                             logAction: Option[String => F[Unit]] = None,
                                           )(httpRoutes: Kleisli[OptionT[F, *], A, Response[F]]): Kleisli[OptionT[F, *], A, Response[F]] =
    impl[OptionT[F, *], F, A](
      logHeaders,
      Right(logBody),
      OptionT.liftK[F],
      redactHeadersWhen,
      logAction,
    )(httpRoutes)
}
