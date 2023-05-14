package io.circe.generic.extras

import io.circe.{ Codec, Decoder, Encoder }
import scala.deriving.Mirror

/**
 * Semi-automatic codec derivation.
 *
 * This object provides helpers for creating [[io.circe.Decoder]] and [[io.circe.ObjectEncoder]]
 * instances for case classes, "incomplete" case classes, sealed trait hierarchies, etc.
 *
 * Typical usage will look like the following:
 *
 * {{{
 *   import io.circe._, io.circe.generic.semiauto._
 *
 *   case class Foo(i: Int, p: (String, Double))
 *
 *   object Foo {
 *     implicit val decodeFoo: Decoder[Foo] = deriveDecoder[Foo]
 *     implicit val encodeFoo: Encoder.AsObject[Foo] = deriveEncoder[Foo]
 *   }
 * }}}
 */
object semiauto {
  inline final def deriveConfiguredDecoder[A](using inline A: Mirror.Of[A], configuration: Configuration): Decoder[A] = io.circe.derivation.ConfiguredDecoder.derived[A]
  inline final def deriveConfiguredEncoder[A](using inline A: Mirror.Of[A], configuration: Configuration): Encoder.AsObject[A] = io.circe.derivation.ConfiguredEncoder.derived[A]
  inline final def deriveConfiguredCodec[A](using inline A: Mirror.Of[A], configuration: Configuration): Codec.AsObject[A] = io.circe.derivation.ConfiguredCodec.derived[A]

  inline final def deriveExtrasDecoder[A](using inline A: Mirror.Of[A], configuration: Configuration): ExtrasDecoder[A] = ???
  inline final def deriveExtrasEncoder[A](using inline A: Mirror.Of[A], configuration: Configuration): Encoder.AsObject[A] = deriveConfiguredEncoder[A]
  inline final def deriveExtrasCodec[A](using inline A: Mirror.Of[A], configuration: Configuration): ExtrasAsObjectCodec[A] = ???

  /**
   * Derive a decoder for a sealed trait hierarchy made up of case objects.
   *
   * Note that this differs from the usual derived decoder in that the leaves of the ADT are represented as JSON
   * strings.
   */
  def deriveEnumerationDecoder[A](): Decoder[A] = ???

  /**
   * Derive an encoder for a sealed trait hierarchy made up of case objects.
   *
   * Note that this differs from the usual derived encoder in that the leaves of the ADT are represented as JSON
   * strings.
   */
  def deriveEnumerationEncoder[A](): Encoder[A] = ???

  /**
   * Derive a codec for a sealed trait hierarchy made up of case objects.
   *
   * Note that this differs from the usual derived encoder in that the leaves of the ADT are represented as JSON
   * strings.
   */
  def deriveEnumerationCodec[A](): Codec[A] = ???

  /**
   * Derive a decoder for a value class.
   */
  def deriveUnwrappedDecoder[A](): Decoder[A] = ???

  /**
   * Derive an encoder for a value class.
   */
  def deriveUnwrappedEncoder[A](): Encoder[A] = ???

  /**
   * Derive a codec for a value class.
   */
  def deriveUnwrappedCodec[A](): Codec[A] = ???

}
