package net.walend.duckaligner.duckupdateservice

import cats.effect.IO
import munit.CatsEffectSuite
import net.walend.duckaligner.duckupdates.v0.{DuckId, DuckSitRepUpdate, DuckUpdate, GeoPoint, Track, UpdatePositionOutput}
import net.walend.duckaligner.duckupdateservice.store.DucksStateStore
import org.http4s.implicits.uri
import org.http4s.{HttpRoutes, Method, Request, Response, Status}
import smithy4s.http4s.SimpleRestJsonBuilder
import smithy4s.json.{Json, JsonPayloadCodecCompiler}

class DucksStateStoreSpec extends CatsEffectSuite:
  private val duckUpdate1 = DuckUpdate(
    id = DuckId(1),
    snapshot = 1,
    position = GeoPoint(0.0, 1.1, 0L)
  )

  test("DuckUpdate shows up") {
    val expected = UpdatePositionOutput(DuckSitRepUpdate(
      snapshot = 1,
      tracks = Map[String, Track](DuckId(1).v.toString -> Track(DuckId(1),List(GeoPoint(0.0, 1.1, 0L))))
    ))

    assertIO(DucksStateStore.updatePosition(duckUpdate1),expected)
  }