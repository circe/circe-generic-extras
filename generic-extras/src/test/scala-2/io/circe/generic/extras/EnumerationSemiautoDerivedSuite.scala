/*
 * Copyright 2019 circe
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.circe.generic.extras

class EnumerationSemiautoDerivedSuite extends CirceSuite {
  test("deriveEnumerationDecoder should not compile on an ADT with case classes") {
    implicit val config: Configuration = Configuration.default
    val _ = config

    assertNoDiff(
      compileErrors("semiauto.deriveEnumerationDecoder[examples.ExtendedCardinalDirection]"),
      """|error:
         |Could not find EnumerationDecoder for type io.circe.generic.extras.examples.ExtendedCardinalDirection.
         |Some possible causes for this:
         |- io.circe.generic.extras.examples.ExtendedCardinalDirection isn't a case class or sealed trait
         |- some of io.circe.generic.extras.examples.ExtendedCardinalDirection's members don't have codecs of their own
         |- missing implicit Configuration
         |semiauto.deriveEnumerationDecoder[examples.ExtendedCardinalDirection]
         |                                 ^
         |""".stripMargin
    )
  }

  test("deriveEnumerationEncoder should not compile on an ADT with case classes") {
    implicit val config: Configuration = Configuration.default
    val _ = config

    assertNoDiff(
      compileErrors("semiauto.deriveEnumerationEncoder[examples.ExtendedCardinalDirection]"),
      """|error:
         |Could not find EnumerationEncoder for type io.circe.generic.extras.examples.ExtendedCardinalDirection.
         |Some possible causes for this:
         |- io.circe.generic.extras.examples.ExtendedCardinalDirection isn't a case class or sealed trait
         |- some of io.circe.generic.extras.examples.ExtendedCardinalDirection's members don't have codecs of their own
         |- missing implicit Configuration
         |semiauto.deriveEnumerationEncoder[examples.ExtendedCardinalDirection]
         |                                 ^
         |""".stripMargin
    )
  }
}
