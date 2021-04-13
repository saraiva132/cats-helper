package com.evolutiongaming

import cats.{ApplicativeError, MonadError}
import cats.effect.MonadCancel

package object catshelper {

  type ApplicativeThrowable[F[_]] = ApplicativeError[F, Throwable]

  object ApplicativeThrowable {

    @deprecated("use cats.ApplicativeThrow instead", "3.0.0")
    def apply[F[_]](implicit F: ApplicativeThrowable[F]): ApplicativeThrowable[F] = F

    @deprecated("use cats.ApplicativeThrow instead", "3.0.0")
    def summon[F[_]](implicit F: ApplicativeThrowable[F]): ApplicativeThrowable[F] = F
  }

  type MonadThrowable[F[_]] = MonadError[F, Throwable]

  object MonadThrowable {

    @deprecated("use cats.MonadThrow instead", "3.0.0")
    def apply[F[_]](implicit F: MonadThrowable[F]): MonadThrowable[F] = F

    @deprecated("use cats.MonadThrow instead", "3.0.0")
    def summon[F[_]](implicit F: MonadThrowable[F]): MonadThrowable[F] = F
  }

  type BracketThrowable[F[_]] = MonadCancel[F, Throwable]


  object BracketThrowable {

    def apply[F[_]](implicit F: BracketThrowable[F]): BracketThrowable[F] = F

    def summon[F[_]](implicit F: BracketThrowable[F]): BracketThrowable[F] = F
  }
}
