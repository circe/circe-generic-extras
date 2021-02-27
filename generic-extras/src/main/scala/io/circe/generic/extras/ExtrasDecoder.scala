package io.circe.generic.extras

import io.circe.{ Decoder, DecodingFailure, HCursor }

/**
 * A derived decoder that includes additional functionality related to configuration.
 */
trait ExtrasDecoder[A] extends Decoder[A] {
  def isStrict: Boolean = false
  def decodeStrict(c: HCursor): ExtrasDecoder.StrictResult[A] = apply(c) match {
    case Right(value) => Right(value)
    case Left(df)     => Left((df, Nil))
  }
}

object ExtrasDecoder {

  /**
   * Includes a list of extraneous fields on failure if the decoder is configured to be strict.
   */
  type StrictResult[A] = Either[(DecodingFailure, List[String]), A]
}
