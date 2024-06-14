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

package io.circe
package generic.extras
package benchmarks

import io.circe.generic.extras.semiauto._
import io.circe.syntax._
import org.openjdk.jmh.annotations._

import java.util.UUID
import java.util.concurrent.TimeUnit
import scala.annotation.tailrec

@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
class RetryDecoderWithDefault {
  import RetryDecoderWithDefault._

  private def decode[A: Decoder](value: String): Either[Error, A] =
    io.circe.parser.decode[A](value)

  @Benchmark
  def decodeCat: Seq[Cat] =
    decode[Seq[Cat]](testData).fold(
      e => throw e,
      identity
    )

  @Benchmark
  def decodeDog: Seq[Dog] =
    decode[Seq[Dog]](testData).fold(
      e => throw e,
      identity
    )
}

object RetryDecoderWithDefault {
  final case class Cat(id: UUID, friends: Seq[Cat] = Seq.empty)

  object Cat {
    private[this] implicit final val configuration: Configuration =
      Configuration.default.withDefaults

    implicit val catCodec: Codec[Cat] = deriveConfiguredCodec
  }

  final case class Dog(id: UUID, friends: Option[Seq[Dog]] = None)

  object Dog {
    private[this] implicit final val configuration: Configuration =
      Configuration.default.withDefaults

    implicit val dogCodec: Codec[Dog] = deriveConfiguredCodec
  }

  def createTestData(size: Long): String = {

    @tailrec
    def loop(i: Long, acc: List[UUID]): String =
      if (i <= 0L) {
        acc.map(uuid => Json.obj("id" -> uuid.asJson)).asJson.noSpaces
      } else {
        loop(i - 1L, new UUID(0L, i) +: acc)
      }

    loop(size, List.empty[UUID])
  }

  val testData: String =
    createTestData(5000)
}
