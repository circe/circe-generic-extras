package io.circe.generic.extras

import io.circe.*
import io.circe.`export`.Exported
import scala.deriving.Mirror

trait AutoDerivation {
  implicit inline final def deriveDecoder[A](using m: Mirror.Of[A], config: Configuration): Exported[Decoder[A]] =
    Exported(semiauto.deriveConfiguredDecoder[A])

  implicit inline final def deriveEncoder[A](using m: Mirror.Of[A], config: Configuration): Exported[Encoder.AsObject[A]] =
    Exported(semiauto.deriveConfiguredEncoder[A])
}

object auto extends AutoDerivation
