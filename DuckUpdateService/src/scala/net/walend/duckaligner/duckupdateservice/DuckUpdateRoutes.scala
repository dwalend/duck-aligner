package net.walend.duckaligner.duckupdateservice

/**
 *
 *
 * @author David Walend
 * @since v0.0.0
 */
import net.walend.duckaligner.duckupdates.v0.{DuckUpdateService, DuckUpdateServiceGen}
import cats.effect.*
import cats.implicits.toFunctorOps
import org.http4s.*
import net.walend.duckaligner.duckupdateservice.store.DucksStateStore
import smithy4s.http4s.SimpleRestJsonBuilder

object DuckUpdateRoutes {

  private def duckStoreRoutesF[F[_]: Async]: F[Resource[F, HttpRoutes[F]]] = DucksStateStore.makeDuckStateStore[F].map { ducksStateStore =>
    SimpleRestJsonBuilder.routes(ducksStateStore).resource
  }

  private def docs[F[_]: Sync]: HttpRoutes[F] =
    smithy4s.http4s.swagger.docs[F](DuckUpdateService)  //todo remove this before running it for real - or block it 

  def allF[F[_] : Async ]: F[Resource[F, HttpRoutes[F]]] =
    import cats. syntax. all. toSemigroupKOps
    duckStoreRoutesF[F].map(_.map(_.combineK(docs)))
}