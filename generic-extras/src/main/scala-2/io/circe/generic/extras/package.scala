package io.circe.generic

import io.circe.Codec

package object extras {
  type ExtrasAsObjectCodec[A] = Codec.AsObject[A] with ExtrasDecoder[A]
}
