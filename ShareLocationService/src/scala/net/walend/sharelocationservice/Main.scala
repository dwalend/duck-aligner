package net.walend.sharelocationservice

import cats.effect.{IO, IOApp}

object Main extends IOApp.Simple:
  val run = ShareLocationServiceServer.run[IO]
