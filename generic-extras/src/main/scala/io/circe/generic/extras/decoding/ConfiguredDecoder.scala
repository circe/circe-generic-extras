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

import io.circe.{ Decoder, DecodingFailure, HCursor }
import io.circe.generic.decoding.DerivedDecoder
import io.circe.generic.extras.{ Configuration, ExtrasDecoder, JsonKey }
import io.circe.generic.extras.util.RecordToMap
import java.util.concurrent.ConcurrentHashMap
import scala.annotation.implicitNotFound
import scala.collection.immutable.Map
import shapeless.{ Annotations, Coproduct, Default, HList, LabelledGeneric, Lazy }
import shapeless.ops.hlist.ToTraversable
import shapeless.ops.record.Keys

@implicitNotFound(
  """Could not find ConfiguredDecoder for type ${A}.
Some possible causes for this:
- ${A} isn't a case class or sealed trait
- some of ${A}'s members don't have codecs of their own
- missing implicit Configuration"""
)
abstract class ConfiguredDecoder[A](config: Configuration) extends DerivedDecoder[A] with ExtrasDecoder[A] {
  private[this] val constructorNameCache: ConcurrentHashMap[String, String] =
    new ConcurrentHashMap[String, String]()

  protected[this] def constructorNameTransformer(value: String): String = {
    val current = constructorNameCache.get(value)

    if (current eq null) {
      val transformed = config.transformConstructorNames(value)
      constructorNameCache.put(value, transformed)
      transformed
    } else {
      current
    }
  }
}

object ConfiguredDecoder extends IncompleteConfiguredDecoders {

  /**
   * Includes a list of extraneous fields on failure if the decoder is configured to be strict.
   */
  type StrictResult[A] = Either[(DecodingFailure, List[String]), A]

  private[this] abstract class CaseClassConfiguredDecoder[A, R <: HList](
    config: Configuration,
    keyAnnotationMap: Map[String, String]
  ) extends ConfiguredDecoder[A](config) {
    private[this] val memberNameCache: ConcurrentHashMap[String, String] =
      new ConcurrentHashMap[String, String]()

    protected[this] def memberNameTransformer(value: String): String = {
      val current = memberNameCache.get(value)

      if (current eq null) {
        val transformed = if (keyAnnotationMap.nonEmpty) {
          keyAnnotationMap.getOrElse(value, config.transformMemberNames(value))
        } else {
          config.transformMemberNames(value)
        }

        memberNameCache.put(value, transformed)
        transformed
      } else {
        current
      }
    }
  }

  private[this] class NonStrictCaseClassConfiguredDecoder[A, R <: HList](
    gen: LabelledGeneric.Aux[A, R],
    decodeR: Lazy[ReprDecoder[R]],
    config: Configuration,
    defaultMap: Map[String, Any],
    keyAnnotationMap: Map[String, String]
  ) extends CaseClassConfiguredDecoder[A, R](config, keyAnnotationMap) {
    final def apply(c: HCursor): Decoder.Result[A] = decodeR.value.configuredDecode(c)(
      memberNameTransformer,
      constructorNameTransformer,
      defaultMap,
      None
    ) match {
      case Right(r)    => Right(gen.from(r))
      case l @ Left(_) => l.asInstanceOf[Decoder.Result[A]]
    }

    override final def decodeAccumulating(c: HCursor): Decoder.AccumulatingResult[A] =
      decodeR.value
        .configuredDecodeAccumulating(c)(
          memberNameTransformer,
          constructorNameTransformer,
          defaultMap,
          None
        )
        .map(gen.from)
  }

  private[this] class StrictCaseClassConfiguredDecoder[A, R <: HList](
    gen: LabelledGeneric.Aux[A, R],
    decodeR: Lazy[ReprDecoder[R]],
    config: Configuration,
    defaultMap: Map[String, Any],
    keyNames: List[String],
    keyAnnotationMap: Map[String, String]
  ) extends CaseClassConfiguredDecoder[A, R](config, keyAnnotationMap) {
    private[this] val expectedFields =
      keyNames.map(memberNameTransformer) ++ config.discriminator

    private[this] val expectedFieldsStr = expectedFields.mkString(", ")
    private[this] val expectedFieldsSet = expectedFields.toSet

    private[this] def extraneousFields(c: HCursor): List[String] =
      c.keys.map(_.toList.filterNot(expectedFieldsSet)).getOrElse(Nil)

    private[this] val wrapped: Decoder[A] =
      new NonStrictCaseClassConfiguredDecoder[A, R](gen, decodeR, config, defaultMap, keyAnnotationMap).validate(c =>
        extraneousFields(c).map { unexpectedField =>
          s"Unexpected field: [$unexpectedField]; valid fields: $expectedFieldsStr"
        }
      )

    final def apply(c: HCursor): Decoder.Result[A] = wrapped.apply(c)

    override final def decodeAccumulating(c: HCursor): Decoder.AccumulatingResult[A] = wrapped.decodeAccumulating(c)

    override final def isStrict: Boolean = true
    override final def decodeStrict(c: HCursor): ConfiguredDecoder.StrictResult[A] = apply(c) match {
      case Right(value) => Right(value)
      case Left(df)     => Left((df, extraneousFields(c)))
    }
  }

  private[this] class AdtConfiguredDecoder[A, R <: Coproduct](
    gen: LabelledGeneric.Aux[A, R],
    decodeR: Lazy[ReprDecoder[R]],
    config: Configuration
  ) extends ConfiguredDecoder[A](config) {
    final def apply(c: HCursor): Decoder.Result[A] = decodeR.value.configuredDecode(c)(
      Predef.identity,
      constructorNameTransformer,
      Map.empty,
      config.discriminator
    ) match {
      case Right(r)    => Right(gen.from(r))
      case l @ Left(_) => l.asInstanceOf[Decoder.Result[A]]
    }

    override final def decodeAccumulating(c: HCursor): Decoder.AccumulatingResult[A] =
      decodeR.value
        .configuredDecodeAccumulating(c)(
          Predef.identity,
          constructorNameTransformer,
          Map.empty,
          config.discriminator
        )
        .map(gen.from)
  }

  implicit def decodeCaseClass[A, R <: HList, D <: HList, F <: HList, K <: HList](implicit
    gen: LabelledGeneric.Aux[A, R],
    decodeR: Lazy[ReprDecoder[R]],
    defaults: Default.AsRecord.Aux[A, D],
    defaultMapper: RecordToMap[D],
    config: Configuration,
    fields: Keys.Aux[R, F],
    fieldsToList: ToTraversable.Aux[F, List, Symbol],
    keys: Annotations.Aux[JsonKey, A, K],
    keysToList: ToTraversable.Aux[K, List, Option[JsonKey]]
  ): ConfiguredDecoder[A] = {
    val defaultMap: Map[String, Any] =
      if (config.useDefaults) defaultMapper(defaults()) else Map.empty

    val keyNames: List[String] = fieldsToList(fields()).map(_.name)

    val keyAnnotationMap: Map[String, String] =
      keyNames
        .zip(keysToList(keys()))
        .collect { case (field, Some(keyAnnotation)) =>
          (field, keyAnnotation.value)
        }
        .toMap

    if (config.strictDecoding) {
      new StrictCaseClassConfiguredDecoder[A, R](gen, decodeR.value, config, defaultMap, keyNames, keyAnnotationMap)
    } else {
      new NonStrictCaseClassConfiguredDecoder[A, R](gen, decodeR.value, config, defaultMap, keyAnnotationMap)
    }
  }

  implicit def decodeAdt[A, R <: Coproduct](implicit
    gen: LabelledGeneric.Aux[A, R],
    decodeR: Lazy[ReprDecoder[R]],
    config: Configuration
  ): ConfiguredDecoder[A] = new AdtConfiguredDecoder[A, R](gen, decodeR, config)
}
