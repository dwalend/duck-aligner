package net.walend.fatjar

import cats.effect.{IO, IOApp}

/**
 *
 *
 * @author David Walend
 * @since v0.0.0
 */
object Main extends IOApp.Simple:

  val run =
    IO.println("Hello World!")
