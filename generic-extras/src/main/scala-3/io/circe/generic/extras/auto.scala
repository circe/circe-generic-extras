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
import io.circe.`export`.Exported

import scala.deriving.Mirror

trait AutoDerivation {
  implicit inline final def deriveDecoder[A](using
    m: Mirror.Of[A],
    config: Configuration
  ): Exported[Decoder[A]] =
    Exported(semiauto.deriveConfiguredDecoder[A])

  implicit inline final def deriveEncoder[A](using
    m: Mirror.Of[A],
    config: Configuration
  ): Exported[Encoder.AsObject[A]] =
    Exported(semiauto.deriveConfiguredEncoder[A])
}

object auto extends AutoDerivation
