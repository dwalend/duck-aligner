package net.walend.duck.front

//todo almost ready to delete this one
/*
trait GeoLocator:
  def geoLocate():Unit

object GeoLocator:
  def geolocator(document: Document, duckUpdateClient: DuckUpdateService[IO]): GeoLocator =
    new GeoLocator:
      private val geolocation = document.defaultView.navigator.geolocation

      def geoLocate(): Unit = geolocation.getCurrentPosition(onSuccess _, onError _)

      def onSuccess(p: Position): Unit =
        import cats.effect.unsafe.implicits.global
        IO.println("test IO.println in onSuccess").unsafeRunAsync {
          case Right(_) => println(s"Worked!")
          case Left(t) => println(s"errored with ${t.getMessage}")
        }
/*
        println(s"latitude=${p.coords.latitude}")
        println(s"longitude=${p.coords.longitude}")
        println(s"altitude=${p.coords.altitude}")
        println(s"speed=${p.coords.speed}")
        println(s"heading=${p.coords.heading}")
        println(s"accuracy=${p.coords.accuracy} m")
        println(s"altitudeAccuracy=${p.coords.altitudeAccuracy}")
        println(s"timestamp=${p.timestamp}")

        val geoPoint: GeoPoint = GeoPoint(
          latitude = p.coords.latitude,
          longitude = p.coords.longitude,
          timestamp = p.timestamp.toLong
        )
*/
        /*
                duckUpdateClient
          .updatePosition(duckUpdate)
          .flatTap(dl => IO.println(s"got duckLine $dl Do something with that."))
          .unsafeRunAsync {
            case Right(_) =>
            case Left(t) => println(s"duckLine error ${t.getMessage}")
          }

         */


        
        /* the duckUpdate looks like 
        UpdatePositionOutput(DuckSitRepUpdate(3,List(Track(DuckId(0),List(GeoPoint(42.33588581370238,-71.20792615771647,1731266038546), GeoPoint(42.33590111186763,-71.20790998133523,1731266024542), GeoPoint(42.33590111186763,-71.20790998133523,1731266015453)))),0))
        
         */

      def onError(p: PositionError): Unit = println(s"Error ${p.code} ${p.message}")
*/
