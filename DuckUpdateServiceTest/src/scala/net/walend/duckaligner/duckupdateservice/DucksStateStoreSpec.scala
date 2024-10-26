package net.walend.duckaligner.duckupdateservice

import cats.effect.IO
import munit.CatsEffectSuite
import net.walend.duckaligner.duckupdates.v0.{DuckId, DuckSitRepUpdate, DuckUpdate, GeoPoint, Track, UpdatePositionOutput}
import net.walend.duckaligner.duckupdateservice.store.DucksStateStore

class DucksStateStoreSpec extends CatsEffectSuite:
  private val duckUpdate1 = DuckUpdate(
    id = DuckId(1),
    snapshot = 1,
    position = GeoPoint(0.0, 1.1, 0L)
  )

  private val expected1 = UpdatePositionOutput(DuckSitRepUpdate(
    snapshot = 1,
    tracks = Map[String, Track](DuckId(1).v.toString -> Track(DuckId(1), List(GeoPoint(0.0, 1.1, 0L))))
  ))


  test("DuckUpdate shows up") {
    assertIO(
      DucksStateStore.makeDuckStateStore[IO].flatMap {
        duckStateStore =>
          duckStateStore.updatePosition(duckUpdate1)
      }
      ,expected1)
  }

  private val duckUpdate2 = DuckUpdate(
    id = DuckId(1),
    snapshot = 2,
    position = GeoPoint(1.0, 2.1, 10000L)
  )

  private val expected2 = UpdatePositionOutput(DuckSitRepUpdate(
    snapshot = 2,
    tracks = Map[String, Track](DuckId(1).v.toString -> Track(DuckId(1), List(
      GeoPoint(1.0, 2.1, 10000L),
      GeoPoint(0.0, 1.1, 0L),
    )))
  ))

  test("DuckUpdates show up") {
     assertIO(
       DucksStateStore.makeDuckStateStore[IO].flatMap{
         duckStateStore =>
           duckStateStore.updatePosition(duckUpdate1).flatMap(_ => duckStateStore.updatePosition(duckUpdate2))
       },
       expected2
     )
  }