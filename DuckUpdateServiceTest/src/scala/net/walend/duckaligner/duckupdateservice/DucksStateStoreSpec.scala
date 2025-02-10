package net.walend.duckaligner.duckupdateservice

import cats.effect.IO
import munit.CatsEffectSuite
import net.walend.duckaligner.duckupdates.v0.{DuckId, DuckSitRepUpdate, DuckUpdate, GeoPoint, Track, UpdatePositionOutput}
import net.walend.duckaligner.duckupdateservice.store.DucksStateStore

class DucksStateStoreSpec extends CatsEffectSuite:

  test("mapKey is available") {
    assertIO(
      DucksStateStore.makeDuckStateStore[IO].flatMap{
        duckStateStore =>
          duckStateStore.mapLibreGlKey().map(_.key).map(_ => true)
      },
      true
    )
  }

  private val duckUpdate1 = DuckUpdate(
    id = DuckId(1),
    snapshot = 1,
    position = GeoPoint(0.0, 1.1, 0L)
  )

  private val expected1 = UpdatePositionOutput(DuckSitRepUpdate(
    snapshot = 1,
    tracks = List(Track(DuckId(1), List(GeoPoint(0.0, 1.1, 0L))))
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
    tracks = List(Track(DuckId(1), List(
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

  private val duckUpdate3 = DuckUpdate(
    id = DuckId(2),
    snapshot = 1,
    position = GeoPoint(10.0, 2.1, 6000L)
  )

  private val expected3 = UpdatePositionOutput(DuckSitRepUpdate(
    snapshot = 3,
    tracks = List(
      Track(DuckId(2), List(
        GeoPoint(10.0, 2.1, 6000L)
      )),
      Track(DuckId(1), List(
        GeoPoint(1.0, 2.1, 10000L),
        GeoPoint(0.0, 1.1, 0L),
      )),
    )
  ))

  test("Second duck starts a second track") {
    assertIO(
      for{
        duckStateStore <- DucksStateStore.makeDuckStateStore[IO]
        _ <- duckStateStore.updatePosition(duckUpdate1)
        _ <- duckStateStore.updatePosition(duckUpdate2)
        duckState <- duckStateStore.updatePosition(duckUpdate3)
      } yield {
        duckState
      },
      expected3
    )
  }