package io.circe.generic

import io.circe.Codec
import java.util.regex.Pattern

package object extras {
  type ExtrasAsObjectCodec[A] = Codec.AsObject[A] with ExtrasDecoder[A]
  type Configuration = io.circe.derivation.Configuration

  object Configuration {
    def apply(
      transformMemberNames: String => String = Predef.identity,
      transformConstructorNames: String => String = Predef.identity,
      useDefaults: Boolean = false,
      discriminator: Option[String] = None,
      strictDecoding: Boolean = false
    ): Configuration = Configuration(
      transformMemberNames = transformMemberNames,
      transformConstructorNames = transformConstructorNames,
      useDefaults = useDefaults,
      discriminator = discriminator,
      strictDecoding = strictDecoding
    )

    val default: Configuration = io.circe.derivation.Configuration.default
    private val basePattern: Pattern = Pattern.compile("([A-Z]+)([A-Z][a-z])")
    private val swapPattern: Pattern = Pattern.compile("([a-z\\d])([A-Z])")

    val snakeCaseTransformation: String => String = s => {
      val partial = basePattern.matcher(s).replaceAll("$1_$2")
      swapPattern.matcher(partial).replaceAll("$1_$2").toLowerCase
    }

    val screamingSnakeCaseTransformation: String => String = s => {
      val partial = basePattern.matcher(s).replaceAll("$1_$2")
      swapPattern.matcher(partial).replaceAll("$1_$2").toUpperCase
    }

    val kebabCaseTransformation: String => String = s => {
      val partial = basePattern.matcher(s).replaceAll("$1-$2")
      swapPattern.matcher(partial).replaceAll("$1-$2").toLowerCase
    }

    val pascalCaseTransformation: String => String = s => {
      s"${s.charAt(0).toUpper}${s.substring(1)}"
    }
  }
  object defaults {
    implicit val defaultGenericConfiguration: Configuration = Configuration.default
  }
}
