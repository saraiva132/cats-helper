package com.evolutiongaming.catshelper.testkit

import cats.effect.IO

private[testkit] trait UnLiftIO[F[_]] {

  def unLiftIO[A](fa: F[A]): IO[A]

}
private[testkit] object UnLiftIO {

  def apply[F[_]](implicit F: UnLiftIO[F]): UnLiftIO[F] = F

  implicit val ioUnliftIO: UnLiftIO[IO] = new UnLiftIO[IO] {
    def unLiftIO[A](fa: IO[A]) : IO[A] = fa
  }

}
