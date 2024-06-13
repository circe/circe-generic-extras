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

package io.circe.generic.extras.encoding

import io.circe.{ Encoder, Json }
import io.circe.generic.extras.Configuration

import scala.annotation.{ implicitNotFound, nowarn }
import shapeless.{ :+:, CNil, Coproduct, HNil, Inl, Inr, LabelledGeneric, Witness }
import shapeless.labelled.FieldType

@implicitNotFound(
  """Could not find EnumerationEncoder for type ${A}.
Some possible causes for this:
- ${A} isn't a case class or sealed trait
- some of ${A}'s members don't have codecs of their own
- missing implicit Configuration"""
)
abstract class EnumerationEncoder[A] extends Encoder[A]

object EnumerationEncoder {
  implicit val encodeEnumerationCNil: EnumerationEncoder[CNil] = new EnumerationEncoder[CNil] {
    def apply(a: CNil): Json = sys.error("Cannot encode CNil")
  }

  implicit def encodeEnumerationCCons[K <: Symbol, V, R <: Coproduct](implicit
    witK: Witness.Aux[K],
    @nowarn gen: LabelledGeneric.Aux[V, HNil],
    encodeR: EnumerationEncoder[R],
    config: Configuration = Configuration.default
  ): EnumerationEncoder[FieldType[K, V] :+: R] = new EnumerationEncoder[FieldType[K, V] :+: R] {
    def apply(a: FieldType[K, V] :+: R): Json = a match {
      case Inl(_) => Json.fromString(config.transformConstructorNames(witK.value.name))
      case Inr(r) => encodeR(r)
    }
  }

  implicit def encodeEnumeration[A, Repr <: Coproduct](implicit
    gen: LabelledGeneric.Aux[A, Repr],
    encodeR: EnumerationEncoder[Repr]
  ): EnumerationEncoder[A] =
    new EnumerationEncoder[A] {
      def apply(a: A): Json = encodeR(gen.to(a))
    }
}
