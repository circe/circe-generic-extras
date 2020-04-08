package io.circe.generic.extras.encoding

import io.circe.{ Encoder, Json, JsonObject }
import io.circe.generic.extras.ConfigurableDeriver
import scala.annotation.implicitNotFound
import scala.language.experimental.macros

/**
 * An encoder for a generic representation of a case class or ADT.
 *
 * Note that users typically will not work with instances of this class.
 */
@implicitNotFound(
  """Could not found ConfiguredAsObjectCodec for type ${A}.
Some possible causes for this:
- ${A} isn't a case class or sealed trat
- some of ${A}'s members don't have codecs of their own
- missing implicit Configuration"""
)
trait ReprAsObjectEncoder[A] extends Encoder.AsObject[A] {
  def configuredEncodeObject(a: A)(
    transformMemberNames: String => String,
    transformDiscriminator: String => String,
    discriminator: Option[String]
  ): JsonObject

  final protected[this] def addDiscriminator[B](
    encode: Encoder[B],
    value: B,
    name: String,
    discriminator: Option[String]
  ): JsonObject = discriminator match {
    case None => JsonObject.singleton(name, encode(value))
    case Some(disc) =>
      encode match {
        case oe: Encoder.AsObject[B] @unchecked => oe.encodeObject(value).add(disc, Json.fromString(name))
        case _                                  => JsonObject.singleton(name, encode(value))
      }
  }

  final def encodeObject(a: A): JsonObject = configuredEncodeObject(a)(Predef.identity, Predef.identity, None)
}

object ReprAsObjectEncoder {
  implicit def deriveReprAsObjectEncoder[R]: ReprAsObjectEncoder[R] =
    macro ConfigurableDeriver.deriveConfiguredEncoder[R]
}
