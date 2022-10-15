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

class ConfiguredAutoDerivedScala2Suite extends CirceSuite {

    import defaults._

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
