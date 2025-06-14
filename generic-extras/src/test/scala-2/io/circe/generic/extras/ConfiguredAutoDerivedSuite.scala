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

import io.circe.Json
import io.circe.generic.extras.auto._
import org.scalacheck.Prop.forAll

import examples._
import defaults._

class ConfiguredAutoDerivedSuite extends CirceSuite {
  property("Decoder[Int => Qux[String]] should decode partial JSON representations") {
    forAll { (i: Int, s: String, j: Int) =>
      val result = Json
        .obj(
          "a" -> Json.fromString(s),
          "j" -> Json.fromInt(j)
        )
        .as[Int => Qux[String]]
        .map(_(i))

      assert(result === Right(Qux(i, s, j)))
    }
  }
}
