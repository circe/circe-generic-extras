package io.circe.generic.extras.decoding

import io.circe.{ Decoder, DecodingFailure, HCursor }
import io.circe.generic.extras.Configuration
import scala.annotation.implicitNotFound
import shapeless.{ :+:, CNil, Coproduct, HNil, Inl, Inr, LabelledGeneric, Witness }
import shapeless.labelled.{ FieldType, field }

@implicitNotFound(
  """Could not find EnumerationDecoder for type ${A}.
Some possible causes for this:
- ${A} isn't a case class or sealed trat
- some of ${A}'s members don't have codecs of their own
- missing implicit Configuration"""
)
abstract class EnumerationDecoder[A] extends Decoder[A]

object EnumerationDecoder {
  implicit val decodeEnumerationCNil: EnumerationDecoder[CNil] = new EnumerationDecoder[CNil] {
    def apply(c: HCursor): Decoder.Result[CNil] = Left(DecodingFailure("Enumeration", c.history))
  }

  implicit def decodeEnumerationCCons[K <: Symbol, V, R <: Coproduct](
    implicit
    witK: Witness.Aux[K],
    gen: LabelledGeneric.Aux[V, HNil],
    decodeR: EnumerationDecoder[R],
    config: Configuration = Configuration.default
  ): EnumerationDecoder[FieldType[K, V] :+: R] = new EnumerationDecoder[FieldType[K, V] :+: R] {
    def apply(c: HCursor): Decoder.Result[FieldType[K, V] :+: R] =
      c.as[String] match {
        case Right(s) if s == config.transformConstructorNames(witK.value.name) =>
          Right(Inl(field[K](gen.from(HNil))))
        case Right(_) =>
          decodeR.apply(c) match {
            case Right(v)  => Right(Inr(v))
            case Left(err) => Left(err)
          }
        case Left(err) => Left(DecodingFailure("Enumeration", c.history))
      }
  }

  implicit def decodeEnumeration[A, Repr <: Coproduct](
    implicit
    gen: LabelledGeneric.Aux[A, Repr],
    decodeR: EnumerationDecoder[Repr]
  ): EnumerationDecoder[A] =
    new EnumerationDecoder[A] {
      def apply(c: HCursor): Decoder.Result[A] = decodeR(c) match {
        case Right(v)  => Right(gen.from(v))
        case Left(err) => Left(err)
      }
    }
}
