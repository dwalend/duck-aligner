package net.walend.duckaligner.duckupdateservice.awssdklocation

import cats.effect.Async
import org.http4s.{HttpRoutes, Method, Request, Response, Uri}
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.Origin
import org.http4s.server.middleware.{CORS, CORSPolicy}

import scala.concurrent.duration.DurationInt

/**
 * A pass-through for maplibregl requests to https://maps.geo.${awsRegion}.amazonaws.com/v2
 *
 * @author David Walend
 * @since v0.0.0
 */
object MapLibreGLRoutes:

  //todo clean out this cors foolishness, too
  private val awsRegion = "us-east-1"
//  private val mapsLibreUrl = s"https://maps.geo.$awsRegion.amazonaws.com/v2"

  private val mapsLibreHostName = s"maps.geo.$awsRegion.amazonaws.com"

  private val corsMethodSvc: CORSPolicy = CORS.policy
    .withAllowOriginHost(Set(
      Origin.Host(Uri.Scheme.https, Uri.RegName(mapsLibreHostName), None)
    ))
    .withAllowMethodsIn(Set(Method.GET))
    .withAllowCredentials(false)
    .withMaxAge(1.day)

  def mapLibreGLProxy[F[_]:Async]: HttpRoutes[F] = corsMethodSvc(mapLibreGLRoutes)

  private def mapLibreGLRoutes[F[_]:Async]:HttpRoutes[F] =
    val dsl = new Http4sDsl[F]{}
    import dsl.*
    HttpRoutes.of[F] {
      case req@GET -> Root / "apiKey" =>
        if (allowRequest(req))
          Ok(AwsSecrets.apiKey)
        else Forbidden("No mapLibreGL for you")
    }

  private def allowRequest[F[_]](req:Request[F]):Boolean = true

