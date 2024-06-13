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

package io.circe.generic.extras.util

import scala.collection.immutable.Map
import shapeless.{ ::, HList, HNil, Witness }
import shapeless.labelled.FieldType

abstract class RecordToMap[R <: HList] {
  def apply(r: R): Map[String, Any]
}

object RecordToMap {
  implicit val hnilRecordToMap: RecordToMap[HNil] = new RecordToMap[HNil] {
    def apply(r: HNil): Map[String, Any] = Map.empty
  }

  implicit def hconsRecordToMap[K <: Symbol, V, T <: HList](implicit
    wit: Witness.Aux[K],
    rtmT: RecordToMap[T]
  ): RecordToMap[FieldType[K, V] :: T] = new RecordToMap[FieldType[K, V] :: T] {
    def apply(r: FieldType[K, V] :: T): Map[String, Any] = rtmT(r.tail) + ((wit.value.name, r.head))
  }
}
