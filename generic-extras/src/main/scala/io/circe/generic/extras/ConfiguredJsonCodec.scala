package io.circe.generic.extras

import io.circe.generic.util.macros.JsonCodecMacros

import scala.annotation.nowarn
import scala.reflect.macros.blackbox

@nowarn("cat=unused")
class ConfiguredJsonCodec(
  encodeOnly: Boolean = false,
  decodeOnly: Boolean = false
) extends scala.annotation.StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro ConfiguredJsonCodecMacros.jsonCodecAnnotationMacro
}

private[generic] class ConfiguredJsonCodecMacros(val c: blackbox.Context) extends JsonCodecMacros {
  import c.universe._

  protected[this] def semiautoObj: Symbol = symbolOf[semiauto.type].asClass.module
  protected[this] def deriveMethodPrefix: String = "deriveConfigured"

  def jsonCodecAnnotationMacro(annottees: Tree*): Tree = constructJsonCodec(annottees: _*)
}
