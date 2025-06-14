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

import cats.data.Validated
import io.circe.ACursor
import io.circe.Decoder
import io.circe.DecodingFailure
import io.circe.HCursor
import io.circe.Json.JNull
import io.circe.generic.extras.ConfigurableDeriver
import shapeless.HNil

import scala.annotation.implicitNotFound
import scala.collection.immutable.Map

/**
 * A decoder for a generic representation of a case class or ADT.
 *
 * Note that users typically will not work with instances of this class, which contains unsafe methods (specifically the
 * two `configuredDecode` methods, which allow passing in an untyped map of default field values).
 */
@implicitNotFound(
  """Could not find ReprDecoder for type ${A}.
Some possible causes for this:
- ${A} isn't a case class or sealed trait
- some of ${A}'s members don't have codecs of their own
- missing implicit Configuration"""
)
abstract class ReprDecoder[A] extends Decoder[A] {
  def configuredDecode(c: HCursor)(
    transformMemberNames: String => String,
    transformConstructorNames: String => String,
    defaults: Map[String, Any],
    discriminator: Option[String]
  ): Decoder.Result[A]

  def configuredDecodeAccumulating(c: HCursor)(
    transformMemberNames: String => String,
    transformConstructorNames: String => String,
    defaults: Map[String, Any],
    discriminator: Option[String]
  ): Decoder.AccumulatingResult[A]

  final protected[this] def orDefault[B](
    c: ACursor,
    decoder: Decoder[B],
    name: String,
    defaults: Map[String, Any]
  ): Decoder.Result[B] =
    // We have to do extra gymnastics here because `Decoder[Option[A]]` is
    // designed to be decode `null` as `Right(None)`, even when a default is
    // present, but all other types differ. We might consider changing that
    // semantic.
    (c.focus.isEmpty, defaults.get(name)) match {
      case (true, Some(d: B @unchecked)) => Right(d)
      case (_, Some(d: B @unchecked))    =>
        decoder.tryDecode(c) match {
          case Left(_) if c.focus.contains(JNull) =>
            Right(d)
          case otherwise =>
            otherwise
        }
      case _ =>
        decoder.tryDecode(c)
    }

  final protected[this] def orDefaultAccumulating[B](
    c: ACursor,
    decoder: Decoder[B],
    name: String,
    defaults: Map[String, Any]
  ): Decoder.AccumulatingResult[B] =
    (c.focus.isEmpty, defaults.get(name)) match {
      case (true, Some(d: B @unchecked)) => Validated.valid(d)
      case (_, Some(d: B @unchecked))    =>
        decoder.tryDecodeAccumulating(c) match {
          case Validated.Invalid(_) if c.focus.contains(JNull) =>
            Validated.valid(d)
          case otherwise =>
            otherwise
        }
      case _ =>
        decoder.tryDecodeAccumulating(c)
    }

  final protected[this] def withDiscriminator[V](
    decode: Decoder[V],
    c: HCursor,
    name: String,
    discriminator: Option[String]
  ): Option[Decoder.Result[V]] = discriminator match {
    case None =>
      val result = c.downField(name)

      if (result.succeeded) Some(decode.tryDecode(result)) else None
    case Some(disc) =>
      c.get[String](disc) match {
        case Right(leafType) if leafType == name => Some(decode(c))
        case Right(_)                            => None
        case l @ Left(_)                         => Some(l.asInstanceOf[Decoder.Result[V]])
      }
  }

  final protected[this] def withDiscriminatorAccumulating[V](
    decode: Decoder[V],
    c: HCursor,
    name: String,
    discriminator: Option[String]
  ): Option[Decoder.AccumulatingResult[V]] = discriminator match {
    case None =>
      val result = c.downField(name)

      if (result.succeeded) Some(decode.tryDecodeAccumulating(result)) else None
    case Some(disc) =>
      c.get[String](disc) match {
        case Right(leafType) if leafType == name =>
          Some(decode.tryDecodeAccumulating(c))
        case Right(_)  => None
        case Left(err) => Some(Validated.invalidNel(err))
      }
  }

  final def apply(c: HCursor): Decoder.Result[A] =
    configuredDecode(c)(Predef.identity, Predef.identity, Map.empty, None)

  final override def decodeAccumulating(c: HCursor): Decoder.AccumulatingResult[A] =
    configuredDecodeAccumulating(c)(Predef.identity, Predef.identity, Map.empty, None)
}

object ReprDecoder {
  implicit def deriveReprDecoder[R]: ReprDecoder[R] = macro ConfigurableDeriver.deriveConfiguredDecoder[R]

  val hnilReprDecoder: ReprDecoder[HNil] = new ReprDecoder[HNil] {
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
  }
}
