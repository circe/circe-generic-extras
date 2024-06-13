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

import io.circe.Codec
import io.circe.Decoder
import io.circe.DecodingFailure
import io.circe.HCursor
import io.circe.Json
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
  """Could not find EnumerationCodec for type ${A}.
Some possible causes for this:
- ${A} isn't a case class or sealed trait
- some of ${A}'s members don't have codecs of their own
- missing implicit Configuration"""
)
abstract class EnumerationCodec[A] extends Codec[A]

object EnumerationCodec {
  implicit val codecForEnumerationCNil: EnumerationCodec[CNil] = new EnumerationCodec[CNil] {
    def apply(c: HCursor): Decoder.Result[CNil] = Left(DecodingFailure("Enumeration", c.history))
    def apply(a: CNil): Json = sys.error("Cannot encode CNil")
  }

  implicit def codecForEnumerationCCons[K <: Symbol, V, R <: Coproduct](implicit
    witK: Witness.Aux[K],
    gen: LabelledGeneric.Aux[V, HNil],
    codecForR: EnumerationCodec[R],
    config: Configuration = Configuration.default
  ): EnumerationCodec[FieldType[K, V] :+: R] = new EnumerationCodec[FieldType[K, V] :+: R] {
    def apply(c: HCursor): Decoder.Result[FieldType[K, V] :+: R] =
      c.as[String] match {
        case Right(s) if s == config.transformConstructorNames(witK.value.name) =>
          Right(Inl(field[K](gen.from(HNil))))
        case Right(_) =>
          codecForR.apply(c) match {
            case Right(v)  => Right(Inr(v))
            case Left(err) => Left(err)
          }
        case Left(_) => Left(DecodingFailure("Enumeration", c.history))
      }
    def apply(a: FieldType[K, V] :+: R): Json = a match {
      case Inl(_) => Json.fromString(config.transformConstructorNames(witK.value.name))
      case Inr(r) => codecForR(r)
    }
  }

  implicit def codecForEnumeration[A, R <: Coproduct](implicit
    gen: LabelledGeneric.Aux[A, R],
    codecForR: EnumerationCodec[R]
  ): EnumerationCodec[A] =
    new EnumerationCodec[A] {
      def apply(c: HCursor): Decoder.Result[A] = codecForR(c) match {
        case Right(v)  => Right(gen.from(v))
        case Left(err) => Left(err)
      }
      def apply(a: A): Json = codecForR(gen.to(a))
    }
}
