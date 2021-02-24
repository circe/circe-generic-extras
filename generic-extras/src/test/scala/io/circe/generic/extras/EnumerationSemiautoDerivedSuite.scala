package io.circe.generic.extras

import io.circe.{ Codec, Decoder, Encoder }
import io.circe.generic.extras.semiauto._
import io.circe.literal._
import io.circe.testing.CodecTests
import shapeless.test.illTyped

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
    implicit val config: Configuration = Configuration.default
    illTyped("deriveEnumerationDecoder[ExtendedCardinalDirection]")
  }

  test("it should respect Configuration") {
    implicit val config: Configuration = Configuration.default.withSnakeCaseConstructorNames
    val decodeMary = deriveEnumerationDecoder[Mary]
    val expected = json""""little_lamb""""
    assert(decodeMary.decodeJson(expected) === Right(LittleLamb))
  }

  test("deriveEnumerationEncoder should not compile on an ADT with case classes") {
    implicit val config: Configuration = Configuration.default
    illTyped("deriveEnumerationEncoder[ExtendedCardinalDirection]")
  }

  test("it should respect Configuration") {
    implicit val config: Configuration = Configuration.default.withSnakeCaseConstructorNames
    val encodeMary = deriveEnumerationEncoder[Mary]
    val expected = json""""little_lamb""""
    assert(encodeMary(LittleLamb) === expected)
  }
}
