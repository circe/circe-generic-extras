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
