package io.circe.generic.extras.codec

import io.circe.{ Codec, Decoder, Encoder, HCursor, Json }
import scala.annotation.implicitNotFound
import shapeless.{ ::, Generic, HNil, Lazy }

@implicitNotFound(
  """Could not find UnwrappedCodec for type ${A}.
Some possible causes for this:
- ${A} isn't a case class or sealed trait
- some of ${A}'s members don't have codecs of their own
- missing implicit Configuration"""
)
abstract class UnwrappedCodec[A] extends Codec[A]

object UnwrappedCodec {
  implicit def codecForUnwrapped[A, R](implicit
    gen: Lazy[Generic.Aux[A, R :: HNil]],
    decodeR: Decoder[R],
    encodeR: Encoder[R]
  ): UnwrappedCodec[A] = new UnwrappedCodec[A] {

    override def apply(c: HCursor): Decoder.Result[A] =
      decodeR(c) match {
        case Right(unwrapped) => Right(gen.value.from(unwrapped :: HNil))
        case l @ Left(_)      => l.asInstanceOf[Decoder.Result[A]]
      }
    override def apply(a: A): Json =
      encodeR(gen.value.to(a).head)
  }
}
