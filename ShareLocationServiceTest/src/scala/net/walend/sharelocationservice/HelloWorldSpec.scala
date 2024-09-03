package net.walend.sharelocationservice

import cats.effect.IO
import munit.CatsEffectSuite
import org.http4s.implicits.uri
import org.http4s.{Method, Request, Response, Status}

class HelloWorldSpec extends CatsEffectSuite:
  private[this] val retHelloWorld: IO[Response[IO]] =
    val getHW = Request[IO](Method.GET, uri"/hello/world")
    val helloWorld = HelloWorld.impl[IO]
    ShareLocationServiceRoutes.helloWorldRoutes(helloWorld).orNotFound(getHW)
    
  test("HelloWorld returns status code 200") {
    assertIO(retHelloWorld.map(_.status) ,Status.Ok)
  }

  test("HelloWorld returns hello world message") {
    assertIO(retHelloWorld.flatMap(_.as[String]), "{\"message\":\"Hello, world\"}")
  }
