package io.circe.generic.extras

import cats.kernel.Eq
import io.circe.{ Codec, Decoder, DecodingFailure, Encoder, Json }
import io.circe.generic.extras.semiauto._
import io.circe.literal._
import io.circe.testing.CodecTests
import org.scalacheck.{ Arbitrary, Gen }
import org.scalacheck.Arbitrary.arbitrary
import shapeless.Witness
import shapeless.labelled.{ FieldType, field }
import org.scalacheck.Prop.forAll

import examples._
import cats.laws.discipline.ScalaVersionSpecific

object ConfiguredSemiautoDerivedScala2Suite {
  implicit val customConfig: Configuration =
    Configuration.default.withSnakeCaseMemberNames.withDefaults.withDiscriminator("type").withSnakeCaseConstructorNames

  implicit  val decodeJlessQux: Decoder[FieldType[Witness.`'j`.T, Int] => Qux[String]] =
    deriveConfiguredFor[FieldType[Witness.`'j`.T, Int] => Qux[String]].incomplete

  implicit val decodeIntlessQux: Decoder[Int => Qux[String]] =
    deriveConfiguredFor[Int => Qux[String]].incomplete

  implicit val decodeQuxPatch: Decoder[Qux[String] => Qux[String]] = deriveConfiguredFor[Qux[String]].patch  
}

class ConfiguredSemiautoDerivedScala2Suite extends CirceSuite {
  import ConfiguredSemiautoDerivedScala2Suite._
  
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
