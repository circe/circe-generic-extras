package io.circe.generic.extras

import cats.data.Validated
import cats.kernel.Eq
import io.circe.{ Decoder, DecodingFailure, Encoder, Json }
import io.circe.CursorOp.DownField
import io.circe.generic.extras.auto._
import io.circe.literal._
import io.circe.testing.CodecTests
import org.scalacheck.{ Arbitrary, Gen }
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Prop.forAll

import examples._

object ConfiguredAutoDerivedSuite {

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

  val genConfiguration: Gen[Configuration] = for {
    transformMemberNames <- arbitrary[String => String]
    transformConstructorNames <- arbitrary[String => String]
    useDefaults <- arbitrary[Boolean]
    discriminator <- arbitrary[Option[String]]
  } yield Configuration(transformMemberNames, transformConstructorNames, useDefaults, discriminator)
  implicit val arbitraryConfiguration: Arbitrary[Configuration] = Arbitrary(genConfiguration)
}

class ConfiguredAutoDerivedSuite extends CirceSuite {
  import ConfiguredAutoDerivedSuite._

  {
    implicit val config: Configuration = Configuration.default
    checkAll("Codec[ConfigExampleBase] (default configuration)", CodecTests[ConfigExampleBase].codec)
  }

  property("Configuration#transformMemberNames should support member name transformation using snake_case") {
    forAll { foo: ConfigExampleFoo =>
      implicit val snakeCaseConfig: Configuration = Configuration.default.withSnakeCaseMemberNames

      import foo._
      val json = json"""{ "this_is_a_field": $thisIsAField, "a": $a, "b": $b}"""

      assert(Encoder[ConfigExampleFoo].apply(foo) === json)
      assert(Decoder[ConfigExampleFoo].decodeJson(json) === Right(foo))
    }
  }

  property("Configuration#transformMemberNames should support member name transformation using SCREAMING_SNAKE_CASE") {
    forAll { foo: ConfigExampleFoo =>
      implicit val snakeCaseConfig: Configuration = Configuration.default.withScreamingSnakeCaseMemberNames

      import foo._
      val json = json"""{ "THIS_IS_A_FIELD": $thisIsAField, "A": $a, "B": $b}"""

      assert(Encoder[ConfigExampleFoo].apply(foo) === json)
      assert(Decoder[ConfigExampleFoo].decodeJson(json) === Right(foo))
    }
  }

  property("Configuration#transformMemberNames should support member name transformation using kebab-case") {
    forAll { foo: ConfigExampleFoo =>
      implicit val kebabCaseConfig: Configuration = Configuration.default.withKebabCaseMemberNames

      import foo._
      val json = json"""{ "this-is-a-field": $thisIsAField, "a": $a, "b": $b}"""

      assert(Encoder[ConfigExampleFoo].apply(foo) === json)
      assert(Decoder[ConfigExampleFoo].decodeJson(json) === Right(foo))
    }
  }

  property("Configuration#transformMemberNames should support member name transformation using PascalCase")  {
    forAll { foo: ConfigExampleFoo =>
      implicit val pascalCaseConfig: Configuration = Configuration.default.withPascalCaseMemberNames

      import foo._
      val json = json"""{ "ThisIsAField": $thisIsAField, "a": $a, "b": $b}"""

      assert(Encoder[ConfigExampleFoo].apply(foo) === json)
      assert(Decoder[ConfigExampleFoo].decodeJson(json) === Right(foo))
    }
  }

  property("Configuration#useDefaults should support using default values during decoding") {
    forAll { (f: String, b: Double) =>
      implicit val withDefaultsConfig: Configuration = Configuration.default.withDefaults

      val foo: ConfigExampleFoo = ConfigExampleFoo(f, 0, b)
      val json = json"""{ "thisIsAField": $f, "b": $b }"""
      val expected = json"""{ "thisIsAField": $f, "a": 0, "b": $b}"""

      assert(Encoder[ConfigExampleFoo].apply(foo) === expected)
      assert(Decoder[ConfigExampleFoo].decodeJson(json) === Right(foo))
    }
  }

  {
    case class FooWithDefault(a: Option[Int] = Some(0), b: String = "b")
    object FooWithDefault {
      implicit val eqConfigExampleFoo: Eq[FooWithDefault] = Eq.fromUniversalEquals
    }

    case class FooNoDefault(a: Option[Int], b: String = "b")
    object FooNoDefault {
      implicit val eqConfigExampleFoo: Eq[FooNoDefault] = Eq.fromUniversalEquals
    }

    implicit val customConfig: Configuration = Configuration.default.withDefaults

    test("Option[T] without default should be None if null decoded") {
      val json = json"""{ "a": null }"""
      assert(Decoder[FooNoDefault].decodeJson(json) === Right(FooNoDefault(None, "b")))
    }

    test("Option[T] without default should be None if missing key decoded") {
      val json = json"""{}"""
      assert(Decoder[FooNoDefault].decodeJson(json) === Right(FooNoDefault(None, "b")))
    }

    test("Option[T] with default should be None if null decoded") {
      val json = json"""{ "a": null }"""
      assert(Decoder[FooWithDefault].decodeJson(json) === Right(FooWithDefault(None, "b")))
    }

    test("Option[T] with default should be default value if missing key decoded") {
      val json = json"""{}"""
      assert(Decoder[FooWithDefault].decodeJson(json) === Right(FooWithDefault(Some(0), "b")))
      assert(Decoder[FooWithDefault].decodeAccumulating(json.hcursor) === Validated.valid(FooWithDefault(Some(0), "b")))
    }

    test("Value with default should be default value if value is null") {
      val json = json"""{"b": null}"""
      assert(Decoder[FooWithDefault].decodeJson(json) === Right(FooWithDefault(Some(0), "b")))
      assert(Decoder[FooWithDefault].decodeAccumulating(json.hcursor) === Validated.valid(FooWithDefault(Some(0), "b")))
    }

    test("Option[T] with default should fail to decode if type in json is not correct") {
      val json = json"""{"a": "NotAnInt"}"""
      assert(Decoder[FooWithDefault].decodeJson(json) === Left(DecodingFailure("Int", List(DownField("a")))))
      assert(
        Decoder[FooWithDefault].decodeAccumulating(json.hcursor)
          === Validated.invalidNel(DecodingFailure("Int", List(DownField("a"))))
      )
    }

    test("Field with default should fail to decode it type in json is not correct") {
      val json = json"""{"b": 1}"""
      assertEquals(
        Decoder[FooWithDefault].decodeJson(json),
        Left(
          DecodingFailure(DecodingFailure.Reason.WrongTypeExpectation("string", Json.fromInt(1)), List(DownField("b")))
        )
      )
      assertEquals(
        Decoder[FooWithDefault].decodeAccumulating(json.hcursor),
        Validated.invalidNel(
          DecodingFailure(DecodingFailure.Reason.WrongTypeExpectation("string", Json.fromInt(1)), List(DownField("b")))
        )
      )
    }
  }

  property("Configuration#discriminator should support a field indicating constructor") {
    forAll { foo: ConfigExampleFoo =>
      implicit val withDefaultsConfig: Configuration = Configuration.default.withDiscriminator("type")

      import foo._
      val json = json"""{ "type": "ConfigExampleFoo", "thisIsAField": $thisIsAField, "a": $a, "b": $b}"""

      assert(Encoder[ConfigExampleBase].apply(foo) === json)
      assert(Decoder[ConfigExampleBase].decodeJson(json) === Right(foo))
    }
  }

  property("Configuration#transformConstructorNames should support constructor name transformation with snake_case") {
    forAll { foo: ConfigExampleFoo =>
      implicit val snakeCaseConfig: Configuration =
        Configuration.default.withDiscriminator("type").withSnakeCaseConstructorNames

      import foo._
      val json = json"""{ "type": "config_example_foo", "thisIsAField": $thisIsAField, "a": $a, "b": $b}"""

      assert(Encoder[ConfigExampleBase].apply(foo) === json)
      assert(Decoder[ConfigExampleBase].decodeJson(json) === Right(foo))
    }
  }

  property(
    "Configuration#transformConstructorNames should support constructor name transformation with SCREAMING_SNAKE_CASE"
  ) {
    forAll { foo: ConfigExampleFoo =>
      implicit val snakeCaseConfig: Configuration =
        Configuration.default.withDiscriminator("type").withScreamingSnakeCaseConstructorNames

      import foo._
      val json = json"""{ "type": "CONFIG_EXAMPLE_FOO", "thisIsAField": $thisIsAField, "a": $a, "b": $b}"""

      assert(Encoder[ConfigExampleBase].apply(foo) === json)
      assert(Decoder[ConfigExampleBase].decodeJson(json) === Right(foo))
    }
  }

  property("Configuration#transformConstructorNames should support constructor name transformation with kebab-case") {
    forAll { foo: ConfigExampleFoo =>
      implicit val kebabCaseConfig: Configuration =
        Configuration.default.withDiscriminator("type").withKebabCaseConstructorNames

      import foo._
      val json = json"""{ "type": "config-example-foo", "thisIsAField": $thisIsAField, "a": $a, "b": $b}"""

      assert(Encoder[ConfigExampleBase].apply(foo) === json)
      assert(Decoder[ConfigExampleBase].decodeJson(json) === Right(foo))
    }
  }

  property("Configuration#transformConstructorNames should support constructor name transformation with PascalCase") {
    forAll { foo: ConfigExampleFoo =>
      implicit val pascalCaseConfig: Configuration =
        Configuration.default.withDiscriminator("type").withPascalCaseConstructorNames

      import foo._
      val json = json"""{ "type": "ConfigExampleFoo", "thisIsAField": $thisIsAField, "a": $a, "b": $b}"""

      assert(Encoder[ConfigExampleBase].apply(foo) === json)
      assert(Decoder[ConfigExampleBase].decodeJson(json) === Right(foo))
    }
  }

  property("Configuration options should work together") {
    forAll { (f: String, b: Double) =>
      implicit val customConfig: Configuration =
        Configuration.default.withSnakeCaseMemberNames.withDefaults.withDiscriminator("type")

      val foo: ConfigExampleBase = ConfigExampleFoo(f, 0, b)
      val json = json"""{ "type": "ConfigExampleFoo", "this_is_a_field": $f, "b": $b}"""
      val expected = json"""{ "type": "ConfigExampleFoo", "this_is_a_field": $f, "a": 0, "b": $b}"""

      assert(Encoder[ConfigExampleBase].apply(foo) === expected)
      assert(Decoder[ConfigExampleBase].decodeJson(json) === Right(foo))
    }
  }

  {
    import defaults._
    checkAll("Codec[Tuple1[Int]]", CodecTests[Tuple1[Int]].codec)
    checkAll("Codec[(Int, Int, Foo)]", CodecTests[(Int, Int, Foo)].codec)
    checkAll("Codec[Qux[Int]]", CodecTests[Qux[Int]].codec)
    checkAll("Codec[Foo]", CodecTests[Foo].codec)

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
  }
}
