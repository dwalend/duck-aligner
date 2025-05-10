package net.walend.duckaligner.duckupdateservice

import cats.effect.{Async, Sync}
import net.walend.duckaligner.duckupdateservice.store.DucksState

import java.io.PrintWriter

/**
 *
 *
 * @author David Walend
 * @since v0.0.0
 */
object Log:
  private lazy val writer = new PrintWriter("/home/ec2-user/duckStateLog.txt", "UTF-8")
  
  def log[F[_] : Sync](message:String, exception:Option[Throwable] = None):F[Unit] =
    Sync[F].blocking {
      writer.println(message)
      exception.foreach{_.printStackTrace(writer)}
      writer.flush()
    }
    
  
