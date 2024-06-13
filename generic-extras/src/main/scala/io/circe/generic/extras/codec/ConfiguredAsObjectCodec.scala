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

package io.circe.generic.extras.codec

import io.circe.Decoder
import io.circe.Encoder
import io.circe.HCursor
import io.circe.JsonObject
import io.circe.generic.codec.DerivedAsObjectCodec
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.ExtrasDecoder
import io.circe.generic.extras.JsonKey
import io.circe.generic.extras.decoding.ConfiguredDecoder
import io.circe.generic.extras.encoding.ConfiguredAsObjectEncoder
import io.circe.generic.extras.util.RecordToMap
import shapeless.Annotations
import shapeless.Coproduct
import shapeless.Default
import shapeless.HList
import shapeless.LabelledGeneric
import shapeless.Lazy
import shapeless.ops.hlist.ToTraversable
import shapeless.ops.record.Keys

import scala.annotation.implicitNotFound

@implicitNotFound(
  """Could not find ConfiguredAsObjectCodec for type ${A}.
Some possible causes for this:
- ${A} isn't a case class or sealed trait
- some of ${A}'s members don't have codecs of their own
- missing implicit Configuration
- ambiguous implicit Configurations"""
)
abstract class ConfiguredAsObjectCodec[A] extends DerivedAsObjectCodec[A] with ExtrasDecoder[A]

object ConfiguredAsObjectCodec {
  implicit def codecForCaseClass[A, R <: HList, D <: HList, F <: HList, K <: HList](implicit
    gen: LabelledGeneric.Aux[A, R],
    codec: Lazy[ReprAsObjectCodec[R]],
    defaults: Default.AsRecord.Aux[A, D],
    defaultMapper: RecordToMap[D],
    config: Configuration,
    fields: Keys.Aux[R, F],
    fieldsToList: ToTraversable.Aux[F, List, Symbol],
    keys: Annotations.Aux[JsonKey, A, K],
    keysToList: ToTraversable.Aux[K, List, Option[JsonKey]]
  ): ConfiguredAsObjectCodec[A] = new ConfiguredAsObjectCodec[A] {
    private[this] val decodeA: ConfiguredDecoder[A] =
      ConfiguredDecoder.decodeCaseClass[A, R, D, F, K](
        gen,
        codec,
        defaults,
        defaultMapper,
        config,
        fields,
        fieldsToList,
        keys,
        keysToList
      )

    private[this] val encodeA: Encoder.AsObject[A] =
      ConfiguredAsObjectEncoder.encodeCaseClass[A, R, F, K](gen, codec, config, fields, fieldsToList, keys, keysToList)

    final def apply(c: HCursor): Decoder.Result[A] = decodeA.apply(c)
    final override def decodeAccumulating(c: HCursor): Decoder.AccumulatingResult[A] = decodeA.decodeAccumulating(c)

    final def encodeObject(a: A): JsonObject = encodeA.encodeObject(a)

    override final def isStrict: Boolean = decodeA.isStrict
    override final def decodeStrict(c: HCursor): ConfiguredDecoder.StrictResult[A] = decodeA.decodeStrict(c)
  }

  implicit def codecForAdt[A, R <: Coproduct](implicit
    gen: LabelledGeneric.Aux[A, R],
    codec: Lazy[ReprAsObjectCodec[R]],
    config: Configuration
  ): ConfiguredAsObjectCodec[A] = new ConfiguredAsObjectCodec[A] {
    private[this] val decodeA: Decoder[A] =
      ConfiguredDecoder.decodeAdt[A, R](gen, codec, config)

    private[this] val encodeA: Encoder.AsObject[A] =
      ConfiguredAsObjectEncoder.encodeAdt[A, R](gen, codec, config)

    final def apply(c: HCursor): Decoder.Result[A] = decodeA.apply(c)
    final override def decodeAccumulating(c: HCursor): Decoder.AccumulatingResult[A] = decodeA.decodeAccumulating(c)

    final def encodeObject(a: A): JsonObject = encodeA.encodeObject(a)
  }
}
