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

package io.circe.generic.extras.decoding

import io.circe.Decoder
import io.circe.HCursor
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.util.RecordToMap
import io.circe.generic.util.PatchWithOptions
import shapeless.Default
import shapeless.HList
import shapeless.LabelledGeneric
import shapeless.ops.function.FnFromProduct
import shapeless.ops.record.RemoveAll

import scala.collection.immutable.Map

private[circe] trait IncompleteConfiguredDecoders {
  implicit final def decodeIncompleteCaseClass[F, P <: HList, A, D <: HList, T <: HList, R <: HList](implicit
    ffp: FnFromProduct.Aux[P => A, F],
    gen: LabelledGeneric.Aux[A, T],
    removeAll: RemoveAll.Aux[T, P, (P, R)],
    decode: ReprDecoder[R],
    defaults: Default.AsRecord.Aux[A, D],
    defaultMapper: RecordToMap[D],
    config: Configuration
  ): ConfiguredDecoder[F] = new ConfiguredDecoder[F](config) {
    private[this] val defaultMap: Map[String, Any] = if (config.useDefaults) defaultMapper(defaults()) else Map.empty

    final def apply(c: HCursor): Decoder.Result[F] = decode.configuredDecode(c)(
      config.transformMemberNames,
      constructorNameTransformer,
      defaultMap,
      None
    ) match {
      case Right(r)    => Right(ffp(p => gen.from(removeAll.reinsert((p, r)))))
      case l @ Left(_) => l.asInstanceOf[Decoder.Result[F]]
    }

    override final def decodeAccumulating(c: HCursor): Decoder.AccumulatingResult[F] =
      decode
        .configuredDecodeAccumulating(c)(
          config.transformMemberNames,
          constructorNameTransformer,
          defaultMap,
          None
        )
        .map(r => ffp(p => gen.from(removeAll.reinsert((p, r)))))
  }

  implicit final def decodeCaseClassPatch[A, D <: HList, R <: HList, O <: HList](implicit
    gen: LabelledGeneric.Aux[A, R],
    patch: PatchWithOptions.Aux[R, O],
    decode: ReprDecoder[O],
    defaults: Default.AsRecord.Aux[A, D],
    defaultMapper: RecordToMap[D],
    config: Configuration
  ): ConfiguredDecoder[A => A] = new ConfiguredDecoder[A => A](config) {
    private[this] val defaultMap: Map[String, Any] = if (config.useDefaults) defaultMapper(defaults()) else Map.empty

    final def apply(c: HCursor): Decoder.Result[A => A] = decode.configuredDecode(c)(
      config.transformMemberNames,
      constructorNameTransformer,
      defaultMap,
      None
    ) match {
      case Right(o)    => Right(a => gen.from(patch(gen.to(a), o)))
      case l @ Left(_) => l.asInstanceOf[Decoder.Result[A => A]]
    }

    override final def decodeAccumulating(c: HCursor): Decoder.AccumulatingResult[A => A] =
      decode
        .configuredDecodeAccumulating(c)(
          config.transformMemberNames,
          constructorNameTransformer,
          defaultMap,
          None
        )
        .map(o => a => gen.from(patch(gen.to(a), o)))
  }
}
