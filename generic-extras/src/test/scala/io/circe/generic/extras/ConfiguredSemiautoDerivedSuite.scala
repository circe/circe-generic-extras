package io.circe.generic.extras

import cats.kernel.Eq
import io.circe.{ Codec, Decoder, DecodingFailure, Encoder, Json }
import io.circe.generic.extras.semiauto._
import io.circe.literal._
import io.circe.testing.CodecTests
import io.circe.syntax._
import org.scalacheck.{ Arbitrary, Gen }
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Prop.forAll

import examples._
import cats.laws.discipline.ScalaVersionSpecific

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
    forAll { (f: String, b: Double) =>
      val foo: ConfigExampleBase = ConfigExampleFoo(f, 0, b)
      val json = json"""{ "type": "config_example_foo", "this_is_a_field": $f, "b": $b}"""
      val expected = json"""{ "type": "config_example_foo", "this_is_a_field": $f, "a": 0, "b": $b}"""

      assert(Encoder[ConfigExampleBase].apply(foo) === expected)
      assert(Decoder[ConfigExampleBase].decodeJson(json) === Right(foo))
    }
  }

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

  property("A generically derived codec for an empty case class should not accept non-objects") {
    forAll { (j: Json) =>
      case class EmptyCc()

      assert(deriveConfiguredDecoder[EmptyCc].decodeJson(j).isRight == j.isObject)
      assert(deriveConfiguredCodec[EmptyCc].decodeJson(j).isRight == j.isObject)
    }
  }
}
