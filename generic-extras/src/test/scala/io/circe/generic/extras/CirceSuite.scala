package io.circe.generic.extras

import cats.instances._
import cats.syntax._
import io.circe.testing.{ ArbitraryInstances, EqInstances }

import munit.DisciplineSuite

/**
 * An opinionated stack of traits to improve consistency and reduce boilerplate in circe tests.
 */
trait CirceSuite
    extends DisciplineSuite
    with AllInstances
    with AllInstancesBinCompat0
    with AllInstancesBinCompat1
    with AllInstancesBinCompat2
    with AllInstancesBinCompat3
    with AllInstancesBinCompat4
    with AllInstancesBinCompat5
    with AllInstancesBinCompat6
    with AllSyntax
    with AllSyntaxBinCompat0
    with AllSyntaxBinCompat1
    with AllSyntaxBinCompat2
    with AllSyntaxBinCompat3
    with AllSyntaxBinCompat4
    with AllSyntaxBinCompat5
    with AllSyntaxBinCompat6
    with ArbitraryInstances
    with EqInstances {

  implicit def prioritizedCatsSyntaxEither[A, B](eab: Either[A, B]): EitherOps[A, B] = new EitherOps(eab)
}
