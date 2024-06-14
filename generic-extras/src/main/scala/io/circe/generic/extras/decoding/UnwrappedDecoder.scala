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
import shapeless.::
import shapeless.Generic
import shapeless.HNil
import shapeless.Lazy

import scala.annotation.implicitNotFound

@implicitNotFound(
  """Could not find UnwrappedDecoder for type ${A}.
Some possible causes for this:
- ${A} isn't a case class or sealed trait
- some of ${A}'s members don't have codecs of their own
- missing implicit Configuration"""
)
abstract class UnwrappedDecoder[A] extends Decoder[A]

object UnwrappedDecoder {
  implicit def decodeUnwrapped[A, R](implicit
    gen: Lazy[Generic.Aux[A, R :: HNil]],
    decodeR: Decoder[R]
  ): UnwrappedDecoder[A] = new UnwrappedDecoder[A] {
    override def apply(c: HCursor): Decoder.Result[A] =
      decodeR(c) match {
        case Right(unwrapped) => Right(gen.value.from(unwrapped :: HNil))
        case l @ Left(_)      => l.asInstanceOf[Decoder.Result[A]]
      }
  }
}
