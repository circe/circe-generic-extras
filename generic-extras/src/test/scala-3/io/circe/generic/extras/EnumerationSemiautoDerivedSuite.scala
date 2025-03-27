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
    given config: Configuration = Configuration.default
    val _ = config

    assertNoDiff(
      compileErrors("semiauto.deriveEnumerationDecoder[examples.ExtendedCardinalDirection]"),
      """|error: Enum "ExtendedCardinalDirection" contains non singleton case "NotACardinalDirectionAtAll"
         |      compileErrors("semiauto.deriveEnumerationDecoder[examples.ExtendedCardinalDirection]"),
         |                  ^
         |""".stripMargin.stripMargin
    )
  }

  test("deriveEnumerationEncoder should not compile on an ADT with case classes") {
    given config: Configuration = Configuration.default
    val _ = config

    assertNoDiff(
      compileErrors("semiauto.deriveEnumerationEncoder[examples.ExtendedCardinalDirection]"),
      """|error: Enum "ExtendedCardinalDirection" contains non singleton case "NotACardinalDirectionAtAll"
         |      compileErrors("semiauto.deriveEnumerationEncoder[examples.ExtendedCardinalDirection]"),
         |                  ^
         |""".stripMargin
    )
  }
}
