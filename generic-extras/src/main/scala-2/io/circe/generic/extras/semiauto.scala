/*
 * Copyright 2019 circe
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.circe.generic.extras

import io.circe.Codec
import io.circe.Decoder
import io.circe.Encoder
import io.circe.generic.extras.codec.ConfiguredAsObjectCodec
import io.circe.generic.extras.codec.EnumerationCodec
import io.circe.generic.extras.codec.UnwrappedCodec
import io.circe.generic.extras.decoding.ConfiguredDecoder
import io.circe.generic.extras.decoding.EnumerationDecoder
import io.circe.generic.extras.decoding.ReprDecoder
import io.circe.generic.extras.decoding.UnwrappedDecoder
import io.circe.generic.extras.encoding.ConfiguredAsObjectEncoder
import io.circe.generic.extras.encoding.EnumerationEncoder
import io.circe.generic.extras.encoding.UnwrappedEncoder
import io.circe.generic.extras.util.RecordToMap
import io.circe.generic.util.PatchWithOptions
import shapeless.Default
import shapeless.HList
import shapeless.LabelledGeneric
import shapeless.Lazy
import shapeless.ops.function.FnFromProduct
import shapeless.ops.record.RemoveAll

/**
 * Semi-automatic codec derivation.
 *
 * This object provides helpers for creating [[io.circe.Decoder]] and [[io.circe.Encoder.AsObject]] instances for case
 * classes, "incomplete" case classes, sealed trait hierarchies, etc.
 *
 * Typical usage will look like the following:
 *
 * {{{
 *   import io.circe._, io.circe.generic.semiauto._
 *
 *   case class Foo(i: Int, p: (String, Double))
 *
 *   object Foo {
 *     implicit val decodeFoo: Decoder[Foo] = deriveConfiguredDecoder[Foo]
 *     implicit val encodeFoo: Encoder.AsObject[Foo] = deriveConfiguredEncoder[Foo]
 *   }
 * }}}
 */
object semiauto {
  @deprecated("Use deriveConfiguredDecoder", "0.12.0")
  final def deriveDecoder[A](implicit decode: Lazy[ConfiguredDecoder[A]]): Decoder[A] =
    deriveConfiguredDecoder
  @deprecated("Use deriveConfiguredEncoder", "0.12.0")
  final def deriveEncoder[A](implicit encode: Lazy[ConfiguredAsObjectEncoder[A]]): Encoder.AsObject[A] =
    deriveConfiguredEncoder
  @deprecated("Use deriveConfiguredCodec", "0.12.0")
  final def deriveCodec[A](implicit codec: Lazy[ConfiguredAsObjectCodec[A]]): Codec.AsObject[A] =
    deriveConfiguredCodec
  @deprecated("Use deriveConfiguredFor", "0.12.0")
  final def deriveFor[A]: DerivationHelper[A] = deriveConfiguredFor

  final def deriveConfiguredDecoder[A](implicit decode: Lazy[ConfiguredDecoder[A]]): Decoder[A] =
    decode.value
  final def deriveConfiguredEncoder[A](implicit encode: Lazy[ConfiguredAsObjectEncoder[A]]): Encoder.AsObject[A] =
    encode.value
  final def deriveConfiguredCodec[A](implicit codec: Lazy[ConfiguredAsObjectCodec[A]]): Codec.AsObject[A] =
    codec.value

  final def deriveExtrasDecoder[A](implicit decode: Lazy[ConfiguredDecoder[A]]): ExtrasDecoder[A] =
    decode.value
  final def deriveExtrasEncoder[A](implicit encode: Lazy[ConfiguredAsObjectEncoder[A]]): Encoder.AsObject[A] =
    encode.value
  final def deriveExtrasCodec[A](implicit codec: Lazy[ConfiguredAsObjectCodec[A]]): ExtrasAsObjectCodec[A] =
    codec.value

  final def deriveConfiguredFor[A]: DerivationHelper[A] = new DerivationHelper[A]

  /**
   * Derive a decoder for a sealed trait hierarchy made up of case objects.
   *
   * Note that this differs from the usual derived decoder in that the leaves of the ADT are represented as JSON
   * strings.
   */
  def deriveEnumerationDecoder[A](implicit decode: Lazy[EnumerationDecoder[A]]): Decoder[A] = decode.value

  /**
   * Derive an encoder for a sealed trait hierarchy made up of case objects.
   *
   * Note that this differs from the usual derived encoder in that the leaves of the ADT are represented as JSON
   * strings.
   */
  def deriveEnumerationEncoder[A](implicit encode: Lazy[EnumerationEncoder[A]]): Encoder[A] = encode.value

  /**
   * Derive a codec for a sealed trait hierarchy made up of case objects.
   *
   * Note that this differs from the usual derived encoder in that the leaves of the ADT are represented as JSON
   * strings.
   */
  def deriveEnumerationCodec[A](implicit codec: Lazy[EnumerationCodec[A]]): Codec[A] = codec.value

  /**
   * Derive a decoder for a value class.
   */
  def deriveUnwrappedDecoder[A](implicit decode: Lazy[UnwrappedDecoder[A]]): Decoder[A] = decode.value

  /**
   * Derive an encoder for a value class.
   */
  def deriveUnwrappedEncoder[A](implicit encode: Lazy[UnwrappedEncoder[A]]): Encoder[A] = encode.value

  /**
   * Derive a codec for a value class.
   */
  def deriveUnwrappedCodec[A](implicit codec: Lazy[UnwrappedCodec[A]]): Codec[A] = codec.value

  final class DerivationHelper[A] {
    final def incomplete[P <: HList, C, D <: HList, T <: HList, R <: HList](implicit
      ffp: FnFromProduct.Aux[P => C, A],
      gen: LabelledGeneric.Aux[C, T],
      removeAll: RemoveAll.Aux[T, P, (P, R)],
      decode: ReprDecoder[R],
      defaults: Default.AsRecord.Aux[C, D],
      defaultMapper: RecordToMap[D],
      config: Configuration
    ): Decoder[A] = ConfiguredDecoder.decodeIncompleteCaseClass[A, P, C, D, T, R]

    final def patch[D <: HList, R <: HList, O <: HList](implicit
      gen: LabelledGeneric.Aux[A, R],
      patch: PatchWithOptions.Aux[R, O],
      decode: ReprDecoder[O],
      defaults: Default.AsRecord.Aux[A, D],
      defaultMapper: RecordToMap[D],
      config: Configuration
    ): Decoder[A => A] = ConfiguredDecoder.decodeCaseClassPatch[A, D, R, O]
  }
}
