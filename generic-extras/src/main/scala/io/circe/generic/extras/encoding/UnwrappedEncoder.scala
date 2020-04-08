package io.circe.generic.extras.encoding

import io.circe.{ Encoder, Json }
import scala.annotation.implicitNotFound
import shapeless.{ ::, Generic, HNil, Lazy }

@implicitNotFound(
  """Could not found ConfiguredAsObjectCodec for type ${A}.
Some possible causes for this:
- ${A} isn't a case class or sealed trat
- some of ${A}'s members don't have codecs of their own
- missing implicit Configuration"""
)
abstract class UnwrappedEncoder[A] extends Encoder[A]

object UnwrappedEncoder {
  implicit def encodeUnwrapped[A, R](
    implicit
    gen: Lazy[Generic.Aux[A, R :: HNil]],
    encodeR: Encoder[R]
  ): UnwrappedEncoder[A] = new UnwrappedEncoder[A] {
    override def apply(a: A): Json =
      encodeR(gen.value.to(a).head)
  }
}
