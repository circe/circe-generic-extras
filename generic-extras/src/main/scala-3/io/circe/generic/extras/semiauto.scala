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
import io.circe.derivation.{ Configuration as CoreConfiguration, * }

import scala.deriving.Mirror

object semiauto {
  inline final def deriveConfiguredCodec[A](using m: Mirror.Of[A], config: Configuration): Codec.AsObject[A] = {
    given CoreConfiguration = toCoreConfig(config)
    ConfiguredCodec.derived[A]
  }

  inline final def deriveConfiguredEncoder[A](using m: Mirror.Of[A], config: Configuration): Encoder.AsObject[A] = {
    given CoreConfiguration = toCoreConfig(config)
    ConfiguredEncoder.derived[A]
  }

  inline final def deriveConfiguredDecoder[A](using m: Mirror.Of[A], config: Configuration): Decoder[A] = {
    given CoreConfiguration = toCoreConfig(config)
    ConfiguredDecoder.derived[A]
  }

  inline final def deriveEnumerationCodec[A](using
    m: Mirror.SumOf[A],
    config: Configuration = Configuration.default
  ): Codec[A] = {
    given CoreConfiguration = toCoreConfig(config)
    ConfiguredEnumCodec.derived[A]
  }

  inline final def deriveEnumerationDecoder[A](using
    m: Mirror.SumOf[A],
    config: Configuration = Configuration.default
  ): Decoder[A] = {
    given CoreConfiguration = toCoreConfig(config)
    ConfiguredEnumDecoder.derived[A]
  }

  inline final def deriveEnumerationEncoder[A](using
    m: Mirror.SumOf[A],
    config: Configuration = Configuration.default
  ): Encoder[A] = {
    given CoreConfiguration = toCoreConfig(config)
    ConfiguredEnumEncoder.derived[A]
  }

  export UnwrappedDerivationMacros.{ deriveUnwrappedCodec, deriveUnwrappedDecoder, deriveUnwrappedEncoder }

  private def toCoreConfig(config: Configuration): CoreConfiguration =
    CoreConfiguration(
      transformMemberNames = config.transformMemberNames,
      transformConstructorNames = config.transformConstructorNames,
      useDefaults = config.useDefaults,
      discriminator = config.discriminator,
      strictDecoding = config.strictDecoding
    )
}
