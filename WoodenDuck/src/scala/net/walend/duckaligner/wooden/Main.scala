package net.walend.duckaligner.wooden

import cats.effect.{ExitCode, IO, IOApp}
import net.walend.duckaligner.duckupdates.v0.{DuckEvent, DuckId, DuckUpdateService, GeoPoint, ProposeEventsOutput}
import fs2.Stream
import net.walend.duckaligner.duckupdates.v0.DuckEvent.DuckPositionEvent

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

  private def ping(event: DuckEvent,client: DuckUpdateService[IO])  =
    for
      _ <- IO.println(s"Ping with $event")
      update: ProposeEventsOutput <- updatePosition(event,client)
      _ <- IO.println(update)
    yield
      update

  private def updatePosition(event: DuckEvent,client: DuckUpdateService[IO]): IO[ProposeEventsOutput] =
    client.proposeEvents(List(event))

  private lazy val positions: Seq[DuckPositionEvent] = {
    val library = (42.338032, -71.211578)
    val newtonSouth = (42.314081, -71.186448)
    val startTimeMs = 1728995663000L

    val stepCount = 10

    val lats = (0 to stepCount).map(i => i*(newtonSouth._1 - library._1)/stepCount).map(_ + library._1)
    val lons = (0 to stepCount).map(i => i*(newtonSouth._2 - library._2)/stepCount).map(_ + library._2)
    val times = (0 to stepCount).map(i => i*10*60*1000 + startTimeMs)
    val geoPoints = lats.zip(lons).zip(times).map(p => GeoPoint(p._1._1,p._1._2,p._2))
    geoPoints.zipWithIndex.map(p => DuckPositionEvent(p._2,DuckId(0),p._1))
  }
