import mill._, mill.scalalib._, mill.scalajslib._
import $ivy.`com.github.lolgab::mill-scalablytyped::0.1.15`
import com.github.lolgab.mill.scalablytyped._

trait Base extends ScalaJSModule {
  def scalaVersion = "3.3.4"
  def scalaJSVersion = "1.18.2"
}

object `scalablytyped-module` extends Base with ScalablyTyped

object app extends Base {
  def moduleDeps = Seq(`scalablytyped-module`)
}