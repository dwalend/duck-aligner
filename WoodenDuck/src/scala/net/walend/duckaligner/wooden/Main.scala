package net.walend.duckaligner.wooden

import cats.effect.{ExitCode, IO, IOApp}
import net.walend.duckaligner.duckupdates.v0.{DuckId, DuckUpdate, DuckUpdateService, GeoPoint, UpdatePositionOutput}
import fs2.Stream

import scala.concurrent.duration.DurationInt

object Main extends IOApp:
  override def run(args: List[String]): IO[ExitCode] =
    DuckUpdateClient.duckUpdateClient.use(
      client => startPinger(client)
    ).map(_ => ExitCode.Success)
  
  private def startPinger(client: DuckUpdateService[IO]): IO[Unit] =
  //todo change to every 30 seconds
    Stream.emits(positions).evalTap(IO.println(_)).evalMap(ping(_,client))
      .meteredStartImmediately[IO](10.seconds)
      .compile.drain

  private def ping(position:(Double,Double),client: DuckUpdateService[IO]): IO[UpdatePositionOutput]  =
    for
      _ <- IO.println(s"Ping from ${position._1},${position._2}!")
      update: UpdatePositionOutput <- updatePosition(position,client)
      _ <- IO.println(update.sitRep)
    yield
      update

  private def updatePosition(position:(Double,Double),client: DuckUpdateService[IO]): IO[UpdatePositionOutput] =
    val duckUpdate: DuckUpdate = DuckUpdate(
      id = DuckId(1), //todo from command line
      duckName = "Wooden duck", //todo from counter
      position = GeoPoint(position._1,position._2, timestamp = System.currentTimeMillis()) 
    )
    client.updatePosition(duckUpdate)

  private lazy val positions: Seq[(Double, Double)] = {
    val library = (42.338032, -71.211578)
    val newtonSouth = (42.314081, -71.186448)

    val stepCount = 10
    val lats = (0 to stepCount).map(i => i*(newtonSouth._1 - library._1)/stepCount).map(_ + library._1)
    val lons = (0 to stepCount).map(i => i*(newtonSouth._2 - library._2)/stepCount).map(_ + library._2)
    lats.zip(lons)
  }
