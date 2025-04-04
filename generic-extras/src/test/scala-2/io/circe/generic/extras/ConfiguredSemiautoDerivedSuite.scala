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
import io.circe.generic.extras.semiauto._
import io.circe.literal._
import io.circe.syntax._
import org.scalacheck.Arbitrary
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalacheck.Prop.forAll
import shapeless.Witness
import shapeless.labelled.FieldType
import shapeless.labelled.field

import examples._

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

  implicit val decodeIntlessQux: Decoder[Int => Qux[String]] =
    deriveConfiguredFor[Int => Qux[String]].incomplete

  implicit val decodeJlessQux: Decoder[FieldType[Witness.`'j`.T, Int] => Qux[String]] =
    deriveConfiguredFor[FieldType[Witness.`'j`.T, Int] => Qux[String]].incomplete

  implicit val decodeQuxPatch: Decoder[Qux[String] => Qux[String]] = deriveConfiguredFor[Qux[String]].patch

  implicit val decodeConfigExampleBase: Decoder[ConfigExampleBase] = deriveConfiguredDecoder
  implicit val encodeConfigExampleBase: Encoder.AsObject[ConfigExampleBase] = deriveConfiguredEncoder
  val codecForConfigExampleBase: Codec.AsObject[ConfigExampleBase] = deriveConfiguredCodec
}

class ConfiguredSemiautoDerivedSuite extends CirceSuite {
  import ConfiguredSemiautoDerivedSuite._

  test("it should call field modification times equal to field count") {
    var transformMemberNamesCallCount, transformConstructorCallCount = 0
    implicit val customConfig: Configuration =
      Configuration.default.copy(
        transformMemberNames = v => {
          transformMemberNamesCallCount = transformMemberNamesCallCount + 1
          Configuration.snakeCaseTransformation(v)
        },
        transformConstructorNames = v => {
          transformConstructorCallCount = transformConstructorCallCount + 1
          Configuration.snakeCaseTransformation(v)
        }
      )

    val fieldCount = 3
    val decodeConstructorCount = 2
    val encodeConstructorCount = 1

    val encoder: Encoder[ConfigExampleBase] = deriveConfiguredEncoder
    val decoder: Decoder[ConfigExampleBase] = deriveConfiguredDecoder
    for {
      _ <- 1 until 100
    } {
      val foo: ConfigExampleBase = ConfigExampleFoo("field_value", 0, 100)
      val encoded = encoder.apply(foo)
      val decoded = decoder.decodeJson(encoded)
      assert(decoded === Right(foo))
      assert(transformMemberNamesCallCount === fieldCount * 2)
      assert(transformConstructorCallCount === decodeConstructorCount + encodeConstructorCount)
    }
  }

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
        DecodingFailure("Unexpected field: [stowaway_field]; valid fields: this_is_a_field, a, b, type_field", Nil)

      assert(Decoder[ConfigExampleBase].decodeJson(json) === Left(expectedError))
    }
  }

  property("it should support configured strict decoding with decodeStrict") {
    forAll { (f: String, b: Double) =>
      implicit val customConfig: Configuration =
        Configuration.default.withSnakeCaseMemberNames.withDefaults
          .withDiscriminator("type_field")
          .withSnakeCaseConstructorNames
          .withStrictDecoding

      implicit val decodeConfigExampleFoo: ExtrasDecoder[ConfigExampleFoo] = deriveExtrasDecoder

      val json =
        json"""
            {
              "type_field": "config_example_foo",
              "this_is_a_field": $f,
              "stowaway_field": "I should not be here",
              "b": $b,
              "also_bad_but_null_valued": null
            }
        """

      val expectedError =
        DecodingFailure(
          "Unexpected field: [stowaway_field]; valid fields: this_is_a_field, a, b, type_field",
          Nil
        )
      val expectedExtraneous = List("stowaway_field", "also_bad_but_null_valued")

      assert(
        implicitly[ExtrasDecoder[ConfigExampleFoo]].decodeStrict(json.hcursor) === Left(
          (expectedError, expectedExtraneous)
        )
      )
    }
  }

  property("it should support configured strict decoding with decodeStrict on a configured codec") {
    forAll { (f: String, b: Double) =>
      implicit val customConfig: Configuration =
        Configuration.default.withSnakeCaseMemberNames.withDefaults
          .withDiscriminator("type_field")
          .withSnakeCaseConstructorNames
          .withStrictDecoding

      implicit val codecForConfigExampleFoo: ExtrasAsObjectCodec[ConfigExampleFoo] = deriveExtrasCodec

      val json =
        json"""
            {
              "type_field": "config_example_foo",
              "this_is_a_field": $f,
              "stowaway_field": "I should not be here",
              "b": $b,
              "also_bad_but_null_valued": null
            }
        """

      val expectedError =
        DecodingFailure(
          "Unexpected field: [stowaway_field]; valid fields: this_is_a_field, a, b, type_field",
          Nil
        )
      val expectedExtraneous = List("stowaway_field", "also_bad_but_null_valued")

      assert(
        implicitly[ExtrasAsObjectCodec[ConfigExampleFoo]].decodeStrict(json.hcursor) === Left(
          (expectedError, expectedExtraneous)
        )
      )
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
        DecodingFailure("Unexpected field: [stowaway_field]; valid fields: this_is_a_field, a, b, typeField", Nil)

      // We should not transform `typeField` into snake case when checking strictly used fields.
      assert(clue(Decoder[ConfigExampleBase].decodeJson(json)).isRight)
      assert(Decoder[ConfigExampleBase].decodeJson(jsonExtra) === Left(expectedError))
    }
  }

  property("Decoder[Int => Qux[String]] should decode partial JSON representations") {
    forAll { (i: Int, s: String, j: Int) =>
      val result = Json
        .obj(
          "a" -> Json.fromString(s),
          "j" -> Json.fromInt(j)
        )
        .as[Int => Qux[String]]
        .map(_(i))

      assert(result === Right(Qux(i, s, j)))
    }
  }

  property("Decoder[FieldType[Witness.`'j`.T, Int] => Qux[String]] should decode partial JSON representations") {
    forAll { (i: Int, s: String, j: Int) =>
      val result = Json
        .obj(
          "i" -> Json.fromInt(i),
          "a" -> Json.fromString(s)
        )
        .as[FieldType[Witness.`'j`.T, Int] => Qux[String]]
        .map(
          _(field(j))
        )

      assert(result === Right(Qux(i, s, j)))
    }
  }

  property("Decoder[Qux[String] => Qux[String]] should decode patch JSON representations") {
    forAll { (q: Qux[String], i: Option[Int], a: Option[String], j: Option[Int]) =>
      val json = Json.obj(
        "i" -> Encoder[Option[Int]].apply(i),
        "a" -> Encoder[Option[String]].apply(a),
        "j" -> Encoder[Option[Int]].apply(j)
      )

      val expected = Qux[String](i.getOrElse(q.i), a.getOrElse(q.a), j.getOrElse(q.j))

      assert(json.as[Qux[String] => Qux[String]].map(_(q)) === Right(expected))
    }
  }
}
