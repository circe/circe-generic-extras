package io.circe.generic.extras

import cats.Eq
import io.circe._
import io.circe.generic.extras.semiauto._
import io.circe.testing.CodecTests
import org.scalacheck.{ Arbitrary, Gen }
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Prop.forAll

object UnwrappedSemiautoDerivedSuite {
  case class Foo(value: String)

  object Foo {
    implicit val eq: Eq[Foo] = Eq.fromUniversalEquals
    implicit val encoder: Encoder[Foo] = deriveUnwrappedEncoder
    implicit val decoder: Decoder[Foo] = deriveUnwrappedDecoder
    val codec: Codec[Foo] = deriveUnwrappedCodec

    val fooGen: Gen[Foo] = arbitrary[String].map(Foo(_))
    implicit val arbitraryFoo: Arbitrary[Foo] = Arbitrary(fooGen)
  }
}

class UnwrappedSemiautoDerivedSuite extends CirceSuite {
  import UnwrappedSemiautoDerivedSuite._

  checkAll("Codec[Foo]", CodecTests[Foo].codec)
  checkAll("Codec[Foo] via Codec", CodecTests[Foo](Foo.codec, Foo.codec).codec)
  checkAll("Codec[Foo] via Decoder and Codec", CodecTests[Foo](implicitly, Foo.codec).codec)
  checkAll("Codec[Foo] via Encoder and Codec", CodecTests[Foo](Foo.codec, implicitly).codec)

  property("Semi-automatic derivation should encode value classes") {
    forAll { (s: String) =>
      val foo = Foo(s)
      val expected = Json.fromString(s)

      assert(Encoder[Foo].apply(foo) === expected)
    }
  }

  property("it should decode value classes") {
    forAll { (s: String) =>
      val json = Json.fromString(s)
      val expected = Right(Foo(s))

      assert(Decoder[Foo].decodeJson(json) === expected)
    }
  }

  property("it should fail decoding incompatible JSON") {
    forAll { (i: Int) =>
      val json = Json.fromInt(i)
      val expected = Left(DecodingFailure(DecodingFailure.Reason.WrongTypeExpectation("string", json), List()))

      assertEquals(Decoder[Foo].decodeJson(json), expected)
    }
  }
}
