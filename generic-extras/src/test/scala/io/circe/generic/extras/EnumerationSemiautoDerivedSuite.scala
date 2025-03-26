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

import io.circe.Codec
import io.circe.Decoder
import io.circe.Encoder
import io.circe.generic.extras.semiauto._
import io.circe.literal._
import io.circe.testing.CodecTests

import examples._

class EnumerationSemiautoDerivedSuite extends CirceSuite {
  implicit val decodeCardinalDirection: Decoder[CardinalDirection] = deriveEnumerationDecoder
  implicit val encodeCardinalDirection: Encoder[CardinalDirection] = deriveEnumerationEncoder
  val codecForCardinalDirection: Codec[CardinalDirection] = deriveEnumerationCodec

  checkAll("Codec[CardinalDirection]", CodecTests[CardinalDirection].codec)
  checkAll(
    "Codec[CardinalDirection] via Codec",
    CodecTests[CardinalDirection](codecForCardinalDirection, codecForCardinalDirection).codec
  )
  checkAll(
    "Codec[CardinalDirection] via Decoder and Codec",
    CodecTests[CardinalDirection](implicitly, codecForCardinalDirection).codec
  )
  checkAll(
    "Codec[CardinalDirection] via Encoder and Codec",
    CodecTests[CardinalDirection](codecForCardinalDirection, implicitly).codec
  )

  test("deriveEnumerationDecoder should not compile on an ADT with case classes") {
    assert(
      compileErrors(
        """
        implicit val config: Configuration = Configuration.default
        val _ = config
        deriveEnumerationDecoder[ExtendedCardinalDirection]
        """
      ).nonEmpty
    )
  }

  test("it should respect Configuration snake-case") {
    implicit val config: Configuration = Configuration.default.withSnakeCaseConstructorNames
    val _ = config
    val decodeMary = deriveEnumerationDecoder[Mary]
    val expected = json""""little_lamb""""
    assert(decodeMary.decodeJson(expected) === Right(LittleLamb))
  }

  test("deriveEnumerationEncoder should not compile on an ADT with case classes") {
    assert(
      compileErrors(
        """
        implicit val config: Configuration = Configuration.default
        val _ = config
        deriveEnumerationEncoder[ExtendedCardinalDirection]
        """
      ).nonEmpty
    )
  }

  test("it should respect Configuration kebab-case") {
    implicit val config: Configuration = Configuration.default.withKebabCaseConstructorNames
    val _ = config
    val encodeMary = deriveEnumerationEncoder[Mary]
    val expected = json""""little-lamb""""
    assert(encodeMary(LittleLamb) === expected)
  }
}
