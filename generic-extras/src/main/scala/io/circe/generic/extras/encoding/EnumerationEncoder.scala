package io.circe.generic.extras.encoding

import io.circe.{ Encoder, Json }
import io.circe.generic.extras.Configuration
import scala.annotation.implicitNotFound
import shapeless.{ :+:, CNil, Coproduct, HNil, Inl, Inr, LabelledGeneric, Witness }
import shapeless.labelled.FieldType

@implicitNotFound(
  """Could not find EnumerationEncoder for type ${A}.
Some possible causes for this:
- ${A} isn't a case class or sealed trat
- some of ${A}'s members don't have codecs of their own
- missing implicit Configuration"""
)
abstract class EnumerationEncoder[A] extends Encoder[A]

object EnumerationEncoder {
  implicit val encodeEnumerationCNil: EnumerationEncoder[CNil] = new EnumerationEncoder[CNil] {
    def apply(a: CNil): Json = sys.error("Cannot encode CNil")
  }

  implicit def encodeEnumerationCCons[K <: Symbol, V, R <: Coproduct](
    implicit
    witK: Witness.Aux[K],
    gen: LabelledGeneric.Aux[V, HNil],
    encodeR: EnumerationEncoder[R],
    config: Configuration = Configuration.default
  ): EnumerationEncoder[FieldType[K, V] :+: R] = new EnumerationEncoder[FieldType[K, V] :+: R] {
    def apply(a: FieldType[K, V] :+: R): Json = a match {
      case Inl(l) => Json.fromString(config.transformConstructorNames(witK.value.name))
      case Inr(r) => encodeR(r)
    }
  }

  implicit def encodeEnumeration[A, Repr <: Coproduct](
    implicit
    gen: LabelledGeneric.Aux[A, Repr],
    encodeR: EnumerationEncoder[Repr]
  ): EnumerationEncoder[A] =
    new EnumerationEncoder[A] {
      def apply(a: A): Json = encodeR(gen.to(a))
    }
}
