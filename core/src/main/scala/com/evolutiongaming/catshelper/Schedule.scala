package com.evolutiongaming.catshelper

import cats.effect.implicits._
import cats.effect.{Concurrent, Resource, Timer}
import cats.implicits._

import scala.concurrent.duration.FiniteDuration

object Schedule {

  def apply[F[_]: Concurrent: Timer](
    initial: FiniteDuration,
    interval: FiniteDuration)(
    fa: F[Unit]
  ): Resource[F, Unit] = {

    val schedule = for {
      _ <- fa
      _ <- Timer[F].sleep(interval)
    } yield {}

    val daemon = for {
      _ <- Timer[F].sleep(initial)
      _ <- schedule.foreverM[Unit]
    } yield {}

    Resource
      .make { daemon.start } { _.cancel }
      .void
  }
}