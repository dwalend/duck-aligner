package net.walend.duckaligner.duckupdateservice.awsskdlocation

import munit.CatsEffectSuite
import net.walend.duckaligner.duckupdateservice.awssdklocation.AwsLocationClient

import java.io.{BufferedOutputStream, FileOutputStream}

class AwsLocationClientSpec extends CatsEffectSuite:
  test("AwsLocationClient returns something") {
    assertIO(
      AwsLocationClient.requestStaticMap.flatMap {response =>
        val bytes = response.blob().asByteArray()

        val bos = new BufferedOutputStream(new FileOutputStream("map.jpeg"))
        bos.write(bytes)
        bos.close()

        null
      }
      ,null)
  }
