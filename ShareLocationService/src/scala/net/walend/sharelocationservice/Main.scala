package net.walend.sharelocationservice

import cats.effect.{IO, IOApp}

object Main extends IOApp.Simple:
  val run = SharelocationserviceServer.run[IO]
