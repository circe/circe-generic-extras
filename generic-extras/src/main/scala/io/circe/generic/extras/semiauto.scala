package io.circe.generic.extras

/**
 * Semi-automatic codec derivation.
 *
 * This object provides helpers for creating [[io.circe.Decoder]] and [[io.circe.Encoder.AsObject]] instances for case
 * classes, "incomplete" case classes, sealed trait hierarchies, etc.
 *
 * Typical usage will look like the following:
 *
 * {{{
 *   import io.circe._, io.circe.generic.semiauto._
 *
 *   case class Foo(i: Int, p: (String, Double))
 *
 *   object Foo {
 *     implicit val decodeFoo: Decoder[Foo] = deriveConfiguredDecoder[Foo]
 *     implicit val encodeFoo: Encoder.AsObject[Foo] = deriveConfiguredEncoder[Foo]
 *   }
 * }}}
 */
object semiauto extends SemiAutoDerivation
