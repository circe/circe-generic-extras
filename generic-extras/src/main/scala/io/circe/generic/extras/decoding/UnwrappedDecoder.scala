package io.circe.generic.extras.decoding

import io.circe.{ Decoder, HCursor }
import scala.annotation.implicitNotFound
import shapeless.{ ::, Generic, HNil, Lazy }

@implicitNotFound(
  """Could not find UnwrappedDecoder for type ${A}.
Some possible causes for this:
- ${A} isn't a case class or sealed trat
- some of ${A}'s members don't have codecs of their own
- missing implicit Configuration"""
)
abstract class UnwrappedDecoder[A] extends Decoder[A]

object UnwrappedDecoder {
  implicit def decodeUnwrapped[A, R](
    implicit
    gen: Lazy[Generic.Aux[A, R :: HNil]],
    decodeR: Decoder[R]
  ): UnwrappedDecoder[A] = new UnwrappedDecoder[A] {
    override def apply(c: HCursor): Decoder.Result[A] =
      decodeR(c) match {
        case Right(unwrapped) => Right(gen.value.from(unwrapped :: HNil))
        case l @ Left(_)      => l.asInstanceOf[Decoder.Result[A]]
      }
  }
}
