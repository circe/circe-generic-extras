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

import cats.kernel.Eq
import io.circe.Codec
import io.circe.Decoder
import io.circe.DecodingFailure
import io.circe.Encoder
import io.circe.Json
import io.circe.generic.extras.semiauto.*
import io.circe.literal.*
import io.circe.syntax.*
import org.scalacheck.Arbitrary
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalacheck.Prop.forAll

object ConfiguredSemiautoDerivedSuite {
  sealed trait ConfigExampleBase
  case class ConfigExampleFoo(thisIsAField: String, a: Int = 0, b: Double) extends ConfigExampleBase
  case object ConfigExampleBar extends ConfigExampleBase

  object ConfigExampleFoo {
    implicit val eqConfigExampleFoo: Eq[ConfigExampleFoo] = Eq.fromUniversalEquals
    val genConfigExampleFoo: Gen[ConfigExampleFoo] = for {
      thisIsAField <- arbitrary[String]
      a <- arbitrary[Int]
      b <- arbitrary[Double]
    } yield ConfigExampleFoo(thisIsAField, a, b)
    implicit val arbitraryConfigExampleFoo: Arbitrary[ConfigExampleFoo] = Arbitrary(genConfigExampleFoo)
  }

  object ConfigExampleBase {
    implicit val eqConfigExampleBase: Eq[ConfigExampleBase] = Eq.fromUniversalEquals
    val genConfigExampleBase: Gen[ConfigExampleBase] =
      Gen.oneOf(Gen.const(ConfigExampleBar), ConfigExampleFoo.genConfigExampleFoo)
    implicit val arbitraryConfigExampleBase: Arbitrary[ConfigExampleBase] = Arbitrary(genConfigExampleBase)
  }

  implicit val customConfig: Configuration =
    Configuration.default.withSnakeCaseMemberNames.withDefaults.withDiscriminator("type").withSnakeCaseConstructorNames

  implicit val decodeConfigExampleBase: Decoder[ConfigExampleBase] = deriveConfiguredDecoder
  implicit val encodeConfigExampleBase: Encoder.AsObject[ConfigExampleBase] = deriveConfiguredEncoder
  val codecForConfigExampleBase: Codec.AsObject[ConfigExampleBase] = deriveConfiguredCodec
}

class ConfiguredSemiautoDerivedSuite extends CirceSuite {
  import ConfiguredSemiautoDerivedSuite._

  property("it should support configured strict decoding") {
    forAll { (f: String, b: Double) =>
      implicit val customConfig: Configuration =
        Configuration.default.withSnakeCaseMemberNames.withDefaults
          .withDiscriminator("type_field")
          .withSnakeCaseConstructorNames
          .withStrictDecoding

      implicit val decodeConfigExampleBase: Decoder[ConfigExampleBase] = deriveConfiguredDecoder

      val json =
        json"""
            {"type_field": "config_example_foo", "this_is_a_field": $f, "b": $b, "stowaway_field": "I should not be here"}
        """

      val expectedError =
        DecodingFailure(
          "Strict decoding ConfigExampleFoo - unexpected fields: stowaway_field; valid fields: this_is_a_field, a, b, type_field.",
          Nil
        )

      assert(Decoder[ConfigExampleBase].decodeJson(json) === Left(expectedError))
    }
  }

  property("it should not transform discriminator for strict decoding checks") {
    forAll { (f: String, b: Double) =>
      implicit val customConfig: Configuration =
        Configuration.default.withSnakeCaseMemberNames.withDefaults
          .withDiscriminator("typeField")
          .withSnakeCaseConstructorNames
          .withStrictDecoding

      implicit val decodeConfigExampleBase: Decoder[ConfigExampleBase] = deriveConfiguredDecoder

      val json = json"""{"typeField": "config_example_foo", "this_is_a_field": $f, "b": $b}"""
      val jsonExtra = json.mapObject(_.add("stowaway_field", "I should not be here".asJson))

      val expectedError =
        DecodingFailure(
          "Strict decoding ConfigExampleFoo - unexpected fields: stowaway_field; valid fields: this_is_a_field, a, b, typeField.",
          Nil
        )

      // We should not transform `typeField` into snake case when checking strictly used fields.
      assert(clue(Decoder[ConfigExampleBase].decodeJson(json)).isRight)
      assert(Decoder[ConfigExampleBase].decodeJson(jsonExtra) === Left(expectedError))
    }
  }
}
