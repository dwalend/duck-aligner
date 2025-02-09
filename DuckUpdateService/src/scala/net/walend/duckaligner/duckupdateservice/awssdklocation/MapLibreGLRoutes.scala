package net.walend.duckaligner.duckupdateservice.awssdklocation

import cats.effect.{Async, Resource}
import org.http4s.{Headers, HttpRoutes, Method, Request, Response, Uri}
import org.http4s.client.Client
import org.http4s.dsl.Http4sDsl
import org.http4s.ember.client.EmberClientBuilder
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

  private val awsRegion = "us-east-1"
  private val mapsLibreUrl = s"https://maps.geo.$awsRegion.amazonaws.com/v2"
  private val mapsLibreBaseUri: Uri = Uri.unsafeFromString(mapsLibreUrl)

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
      case GET -> Root / "apiKey" =>
        Ok(AwsSecrets.apiKey)

      //todo not used
        
      case req@GET -> "mapLibreGL" /: rest =>
        if (allowRequest(req))
          val responseResource:Resource[F,Response[F]] = for
            client: Client[F] <- EmberClientBuilder.default[F].build
            uri: Uri = mapsLibreBaseUri.addPath(rest.renderString).withQueryParam("key",AwsSecrets.apiKey)
            proxiedReq = req.withUri(uri).withHeaders(Headers("Origin" -> s"https://maps.geo.$awsRegion.amazonaws.com"))
            response:Response[F] <- client.run(proxiedReq)
          yield
            response

          responseResource.use{response =>
            response.toStrict(None) //buffer the whole response
          }
        else Forbidden("No mapLibreGL for you")


    }

  private def allowRequest[F[_]](req:Request[F]):Boolean = true

