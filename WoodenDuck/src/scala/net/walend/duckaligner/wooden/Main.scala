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
  
  private def startPinger(client: DuckUpdateService[IO]) =
  //todo change to every 30 seconds
    Stream.repeatEval(ping(client)).meteredStartImmediately(10.seconds).debounce(5.seconds)
      .compile.drain

  private def ping(client: DuckUpdateService[IO]): IO[UpdatePositionOutput]  =
    val position:(Double,Double) = (42.338032, -71.211578) //(42.314081, -71.186448)
    for
      _ <- IO.println(s"Ping from ${position._1},${position._2}!")
      update: UpdatePositionOutput <- updatePosition(position,client)
    yield
      update

  private def updatePosition(position:(Double,Double),client: DuckUpdateService[IO]): IO[UpdatePositionOutput] =
    val duckUpdate: DuckUpdate = DuckUpdate(
      id = DuckId(-1), //todo from command line
      snapshot = 0, //todo from counter
      position = GeoPoint(position._1,position._2, timestamp = System.currentTimeMillis()) //todo from file?
    )
    client.updatePosition(duckUpdate)
