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

import cats.data.Validated
import io.circe.{ Decoder, DecodingFailure, HCursor, JsonObject }
import io.circe.generic.extras.ConfigurableDeriver
import io.circe.generic.extras.decoding.ReprDecoder
import io.circe.generic.extras.encoding.ReprAsObjectEncoder
import scala.annotation.implicitNotFound
import scala.collection.immutable.Map
import shapeless.HNil

/**
 * A codec for a generic representation of a case class or ADT.
 *
 * Note that users typically will not work with instances of this class.
 */
@implicitNotFound(
  """Could not find ReprAsObjectCodec for type ${A}.
Some possible causes for this:
- ${A} isn't a case class or sealed trait
- some of ${A}'s members don't have codecs of their own
- missing implicit Configuration"""
)
abstract class ReprAsObjectCodec[A] extends ReprDecoder[A] with ReprAsObjectEncoder[A]

object ReprAsObjectCodec {
  implicit def deriveReprAsObjectCodec[R]: ReprAsObjectCodec[R] = macro ConfigurableDeriver.deriveConfiguredCodec[R]

  val hnilReprCodec: ReprAsObjectCodec[HNil] = new ReprAsObjectCodec[HNil] {
    def configuredDecode(c: HCursor)(
      transformMemberNames: String => String,
      transformConstructorNames: String => String,
      defaults: Map[String, Any],
      discriminator: Option[String]
    ): Decoder.Result[HNil] =
      if (c.value.isObject) Right(HNil) else Left(DecodingFailure("HNil", c.history))

    def configuredDecodeAccumulating(c: HCursor)(
      transformMemberNames: String => String,
      transformConstructorNames: String => String,
      defaults: Map[String, Any],
      discriminator: Option[String]
    ): Decoder.AccumulatingResult[HNil] =
      if (c.value.isObject) Validated.valid(HNil) else Validated.invalidNel(DecodingFailure("HNil", c.history))

    def configuredEncodeObject(a: HNil)(
      transformMemberNames: String => String,
      transformDiscriminator: String => String,
      discriminator: Option[String]
    ): JsonObject = JsonObject.empty
  }
}
