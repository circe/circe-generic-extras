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

import io.circe.*
import scala.compiletime.*
import scala.quoted.*

private[extras] object UnwrappedDerivationMacros {
  inline final def deriveUnwrappedCodec[A]: Codec[A] =
    Codec.from(
      decodeA = deriveUnwrappedDecoder[A],
      encodeA = deriveUnwrappedEncoder[A]
    )

  inline final def deriveUnwrappedDecoder[A]: Decoder[A] = ${ deriveUnwrappedDecoderImpl[A] }

  private def deriveUnwrappedDecoderImpl[A: Type](using Quotes): Expr[Decoder[A]] = {
    import quotes.reflect.*

    val wrapperTypeRepr = TypeRepr.of[A]
    val wrapperSymbol = wrapperTypeRepr.typeSymbol
    val underlyingField = wrapperSymbol.declaredFields.head
    val underlyingTypeRepr = wrapperTypeRepr.memberType(underlyingField)

    // derive underlying reader and wrap it into a value class
    underlyingTypeRepr.asType match {
      case '[t] =>
        def wrap(expr: Expr[t]): Expr[A] =
          New(Inferred(wrapperTypeRepr)).select(wrapperSymbol.primaryConstructor).appliedTo(expr.asTerm).asExprOf[A]

        '{
          summonInline[Decoder[t]].map(a => ${ wrap('a) })
        }
    }
  }

  inline final def deriveUnwrappedEncoder[A]: Encoder[A] = ${ deriveUnwrappedEncoderImpl[A] }

  private def deriveUnwrappedEncoderImpl[A: Type](using Quotes): Expr[Encoder[A]] = {
    import quotes.reflect.*

    val wrapperTypeRepr = TypeRepr.of[A]
    val wrapperSymbol = wrapperTypeRepr.typeSymbol
    val underlyingField = wrapperSymbol.declaredFields.head
    val underlyingTypeRepr = wrapperTypeRepr.memberType(underlyingField)

    // derive underlying writer and unwrap it from a value class
    underlyingTypeRepr.asType match {
      case '[t] =>
        def unwrap(expr: Expr[A]): Expr[t] =
          expr.asTerm.select(underlyingField).appliedToArgss(Nil).asExprOf[t]

        '{
          summonInline[Encoder[t]].contramap[A](a => ${ unwrap('a) })
        }
    }
  }

}
