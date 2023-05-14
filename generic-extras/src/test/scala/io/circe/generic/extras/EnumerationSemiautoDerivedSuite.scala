package io.circe.generic.extras

import io.circe.{ Codec, Decoder, Encoder }
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
    implicit val config: Configuration = Configuration.default
    assertNoDiff(
      compileErrors("deriveEnumerationDecoder[ExtendedCardinalDirection]"),
      """|error:
         |Could not find EnumerationDecoder for type io.circe.generic.extras.examples.ExtendedCardinalDirection.
         |Some possible causes for this:
         |- io.circe.generic.extras.examples.ExtendedCardinalDirection isn't a case class or sealed trait
         |- some of io.circe.generic.extras.examples.ExtendedCardinalDirection's members don't have codecs of their own
         |- missing implicit Configuration
         |deriveEnumerationDecoder[ExtendedCardinalDirection]
         |                        ^
         |""".stripMargin
    )
  }

  test("it should respect Configuration") {
    implicit val config: Configuration = Configuration.default.withSnakeCaseConstructorNames
    val decodeMary = deriveEnumerationDecoder[Mary]
    val expected = json""""little_lamb""""
    assertEquals(decodeMary.decodeJson(expected),  Right(LittleLamb))
  }

  test("deriveEnumerationEncoder should not compile on an ADT with case classes") {
    implicit val config: Configuration = Configuration.default
    assertNoDiff(
      compileErrors("deriveEnumerationEncoder[ExtendedCardinalDirection]"),
      """|error:
         |Could not find EnumerationEncoder for type io.circe.generic.extras.examples.ExtendedCardinalDirection.
         |Some possible causes for this:
         |- io.circe.generic.extras.examples.ExtendedCardinalDirection isn't a case class or sealed trait
         |- some of io.circe.generic.extras.examples.ExtendedCardinalDirection's members don't have codecs of their own
         |- missing implicit Configuration
         |deriveEnumerationEncoder[ExtendedCardinalDirection]
         |                        ^
         |""".stripMargin
    )
  }

  test("it should respect Configuration") {
    implicit val config: Configuration = Configuration.default.withSnakeCaseConstructorNames
    val encodeMary = deriveEnumerationEncoder[Mary]
    val expected = json""""little_lamb""""
    assertEquals(encodeMary(LittleLamb),  expected)
  }
}
