import mill._, mill.scalalib._, mill.scalajslib._
import $ivy.`com.github.lolgab::mill-scalablytyped::0.1.15`  //0.4.0
import com.github.lolgab.mill.scalablytyped._

//what causes no default parameter for TArrayBuffer extends std.ArrayBufferLike ?
trait Base extends ScalaJSModule {
  def scalaVersion = "3.3.6"   //3.3.7
  def scalaJSVersion = "1.18.2" //1.20.2
}

object `scalablytyped-module` extends Base with ScalablyTyped

object app extends Base {
  def moduleDeps = Seq(`scalablytyped-module`)
}