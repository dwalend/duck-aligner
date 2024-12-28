package net.walend.duckaligner.duckupdateservice.awsskdlocation

import munit.CatsEffectSuite
import net.walend.duckaligner.duckupdateservice.awssdklocation.AwsLocationClient

class AwsLocationClientSpec extends CatsEffectSuite:
  test("AwsLocationClient returns something") {
    assertIO(
      AwsLocationClient.getTile()
      ,null)
  }
