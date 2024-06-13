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

import io.circe.Decoder
import io.circe.DecodingFailure
import io.circe.HCursor

/**
 * A derived decoder that includes additional functionality related to configuration.
 */
trait ExtrasDecoder[A] extends Decoder[A] {
  def isStrict: Boolean = false
  def decodeStrict(c: HCursor): ExtrasDecoder.StrictResult[A] = apply(c) match {
    case Right(value) => Right(value)
    case Left(df)     => Left((df, Nil))
  }
}

object ExtrasDecoder {

  /**
   * Includes a list of extraneous fields on failure if the decoder is configured to be strict.
   */
  type StrictResult[A] = Either[(DecodingFailure, List[String]), A]
}
