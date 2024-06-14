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
import io.circe.DecodingFailure
import io.circe.HCursor
import io.circe.generic.extras.Configuration
import shapeless.:+:
import shapeless.CNil
import shapeless.Coproduct
import shapeless.HNil
import shapeless.Inl
import shapeless.Inr
import shapeless.LabelledGeneric
import shapeless.Witness
import shapeless.labelled.FieldType
import shapeless.labelled.field

import scala.annotation.implicitNotFound

@implicitNotFound(
  """Could not find EnumerationDecoder for type ${A}.
Some possible causes for this:
- ${A} isn't a case class or sealed trait
- some of ${A}'s members don't have codecs of their own
- missing implicit Configuration"""
)
abstract class EnumerationDecoder[A] extends Decoder[A]

object EnumerationDecoder {
  implicit val decodeEnumerationCNil: EnumerationDecoder[CNil] = new EnumerationDecoder[CNil] {
    def apply(c: HCursor): Decoder.Result[CNil] = Left(DecodingFailure("Enumeration", c.history))
  }

  implicit def decodeEnumerationCCons[K <: Symbol, V, R <: Coproduct](implicit
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
        case Left(_) => Left(DecodingFailure("Enumeration", c.history))
      }
  }

  implicit def decodeEnumeration[A, Repr <: Coproduct](implicit
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
