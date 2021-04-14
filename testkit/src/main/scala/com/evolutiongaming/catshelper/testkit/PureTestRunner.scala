package com.evolutiongaming.catshelper.testkit

import cats.effect.syntax.all._
import cats.effect.testkit.{TestContext, TestInstances}
import cats.effect.{Async, IO, Sync, Temporal}
import cats.syntax.all._
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

private[testkit] object PureTestRunner {
  type TestBody[F[A], A] = PureTest.Env[F] => F[A]

  def doRunTest[F[_] : Async : UnLiftIO, A](body: TestBody[F, A], config: PureTest.Config): A = {
    val env = new EnvImpl[F]
    val singleRun = wrap(env, body(env), config)

    val fullTestIO = config.flakinessCheckIterations match {
      case n if n > 1 => singleRun.replicateA(n).map(_.head)
      case _ => singleRun
    }



    env
            .unsafeRun(fullTestIO)(env.ticker)
            .fold(
              throw new Exception("cancelled potatoes"),
              e => throw e,
              _.getOrElse(throw new Exception("no potatoes"))
            )
  }

  private def wrap[F[_] : Async  : UnLiftIO, A](env: EnvImpl[F], body: F[A], config: PureTest.Config): IO[A] = {

      @volatile var outcome: Option[Either[Throwable, A]] = None

      val bodyFiber  =  body
              .evalOn(env.testContext)
              .attempt
              .flatMap { r =>
                Sync[F].delay {
                  outcome = Some(r)
                }
              }
              .start

      val testThread = Thread.currentThread()

      val stopHotLoop = Sync[F].delay {
        val err = new IllegalStateException("Still running")
        err.setStackTrace(testThread.getStackTrace)
        outcome = Some(Left(err))
      } *> bodyFiber.flatMap(_.cancel)

      val timeoutFiber =
        (Temporal[F].sleep(config.hotLoopTimeout) *> stopHotLoop)
                .evalOn(config.backgroundEc)
                .start


      while (outcome.isEmpty && env.testContext.state.tasks.nonEmpty) {
        val step = env.testContext.state.tasks.iterator.map(_.runsAt).min
        env.testContext.tick(step)
      }

    UnLiftIO[F].unLiftIO(timeoutFiber.flatMap(_.cancel)) *> config.testFrameworkApi.completeWith(outcome, env.testContext.state)

 }

  private class EnvImpl[F[_] : Async] extends PureTest.Env[F] with TestInstances {
    val testContext: TestContext = TestContext()

    implicit val ec: ExecutionContext = testContext

    implicit val ticker: Ticker = Ticker(ctx = testContext)

    implicit val testRuntime: TestRuntime[F] = new TestRuntime[F] {

      /** NB: We exploit the fact that TestContext starts from 0. */
      def getTimeSinceStart: F[FiniteDuration] = Sync[F].delay(testContext.state.clock)

      def sleepUntil(dt: FiniteDuration): F[Unit] =
        getTimeSinceStart.flatMap(t => Temporal[F].sleep(dt - t).whenA(dt > t))
    }
  }

}


