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
import io.circe.Encoder
import io.circe.Json
import io.circe.generic.extras.semiauto._
import io.circe.literal._
import io.circe.testing.CodecTests
import org.scalacheck.Arbitrary
import org.scalacheck.Gen
import org.scalacheck.Prop.forAll

object SharedConfiguredSemiautoDerivedSuite {

  case class InnerExample(a: Double)
  object InnerExample {
    implicit val eqInnerExample: Eq[InnerExample] = Eq.fromUniversalEquals
    val genInnerExample: Gen[InnerExample] = Gen.resultOf(InnerExample.apply _)
    implicit val arbitraryInnerExample: Arbitrary[InnerExample] = Arbitrary(genInnerExample)
  }
  sealed trait ConfigExampleBase
  case class ConfigExampleFoo(thisIsAField: String, a: Int = 0, b: InnerExample) extends ConfigExampleBase
  case object ConfigExampleBar extends ConfigExampleBase

  object ConfigExampleFoo {
    implicit val eqConfigExampleFoo: Eq[ConfigExampleFoo] = Eq.fromUniversalEquals
    val genConfigExampleFoo: Gen[ConfigExampleFoo] = Gen.resultOf(ConfigExampleFoo.apply _)
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

}

class SharedConfiguredSemiautoDerivedSuite extends CirceSuite {
  import SharedConfiguredSemiautoDerivedSuite._

  property("Semi-automatic decoder derivation does NOT derive inner types automatically") {
    val errors = clue(compileErrors("deriveConfiguredDecoder[ConfigExampleBase]"))
    assert(
      List(
        "members don't have codecs of their own", // Scala 2.13, 3
        "error: Failed to find an instance of Decoder[io.circe.generic.extras.SharedConfiguredSemiautoDerivedSuite.InnerExample]" // Scala 2.12
      ).exists(errors.contains)
    )
  }
  property("Semi-automatic encoder derivation does NOT derive inner types automatically") {
    val errors = clue(compileErrors("deriveConfiguredEncoder[ConfigExampleBase]"))
    assert(
      List(
        "members don't have codecs of their own", // Scala 2.13, 3
        "error: Failed to find an instance of Encoder[io.circe.generic.extras.SharedConfiguredSemiautoDerivedSuite.InnerExample]" // Scala 2.12
      ).exists(errors.contains)
    )
  }
  property("Semi-automatic codec derivation does NOT derive inner types automatically") {
    val errors = clue(compileErrors("deriveConfiguredCodec[ConfigExampleBase]"))
    assert(
      List(
        "members don't have codecs of their own", // Scala 2.13, 3
        "error: Failed to find an instance of Decoder[io.circe.generic.extras.SharedConfiguredSemiautoDerivedSuite.InnerExample]" // Scala 2.12
      ).exists(errors.contains)
    )
  }

  {
    implicit val innerExample: Codec[InnerExample] = deriveConfiguredCodec
    implicit val decodeConfigExampleBase: Decoder[ConfigExampleBase] = deriveConfiguredDecoder
    implicit val encodeConfigExampleBase: Encoder.AsObject[ConfigExampleBase] = deriveConfiguredEncoder
    val codecForConfigExampleBase: Codec.AsObject[ConfigExampleBase] = deriveConfiguredCodec

    checkAll("Codec[ConfigExampleBase]", CodecTests[ConfigExampleBase].codec)
    checkAll(
      "Codec[ConfigExampleBase] via Codec",
      CodecTests[ConfigExampleBase](codecForConfigExampleBase, codecForConfigExampleBase).codec
    )
    checkAll(
      "Codec[ConfigExampleBase] via Decoder and Codec",
      CodecTests[ConfigExampleBase](implicitly, codecForConfigExampleBase).codec
    )
    checkAll(
      "Codec[ConfigExampleBase] via Encoder and Codec",
      CodecTests[ConfigExampleBase](codecForConfigExampleBase, implicitly).codec
    )

    property("Semi-automatic derivation should support configuration") {
      forAll { (f: String, b: InnerExample) =>
        val foo: ConfigExampleBase = ConfigExampleFoo(f, 0, b)
        val json = json"""{ "type": "config_example_foo", "this_is_a_field": $f, "b": $b}"""
        val expected = json"""{ "type": "config_example_foo", "this_is_a_field": $f, "a": 0, "b": $b}"""

        assert(Encoder[ConfigExampleBase].apply(foo) === expected)
        assert(Decoder[ConfigExampleBase].decodeJson(json) === Right(foo))
      }
    }

    property("A generically derived codec for an empty case class should not accept non-objects") {
      forAll { (j: Json) =>
        case class EmptyCc()

        assert(deriveConfiguredDecoder[EmptyCc].decodeJson(j).isRight == j.isObject)
        assert(deriveConfiguredCodec[EmptyCc].decodeJson(j).isRight == j.isObject)
      }
    }
  }
}
