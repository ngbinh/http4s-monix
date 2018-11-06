package com.example.http4smonix

import java.util.concurrent.TimeUnit

import monix.eval.Task
import monix.execution.{ExecutionModel, Scheduler}
import org.http4s.client.blaze.BlazeClientBuilder
import monix.execution.Scheduler.Implicits.global
import org.http4s.Uri.{Authority, RegName, Scheme}

import scala.concurrent.Await
import scala.concurrent.duration.FiniteDuration

// scalastyle:off underscore.import
import org.http4s._
import org.scalatest._
// scalastyle:on underscore.import

final class Http4sMonixSpec extends FreeSpecLike with Matchers with EitherValues {

  private val log = org.log4s.getLogger

  def test(traverse: Seq[Task[String]] => Task[Seq[String]], timeout: FiniteDuration): Assertion = {
    val httpScheduler: Scheduler = Scheduler.fixedPool(
      name = "http4s-monix-scheduler",
      poolSize = 4,
      executionModel = ExecutionModel.AlwaysAsyncExecution
    )

    val task = BlazeClientBuilder[Task](httpScheduler)
      .withMaxTotalConnections(maxTotalConnections = 4)
      .withRequestTimeout(FiniteDuration(20, TimeUnit.SECONDS))
      .resource
      .use { client =>
        val list: Seq[Task[String]] = List.range(1, 30).map { i =>
          val uri = Uri(
            scheme = Some(Scheme.https),
            authority = Some(Authority(host = RegName("httpbin.org"))),
            path = s"status/${500 + i}"
          )
          val request = Request[Task](
            method = Method.GET,
            uri = uri
          )
          client.expect[String](request).onErrorHandleWith { error =>
            log.error(error)(s"request to $uri")
            Task.raiseError(error)
          }
        }

        val list2: Seq[Task[String]] = List.range(31, 40).map { i =>
          val uri = Uri(
            scheme = Some(Scheme.https),
            authority = Some(Authority(host = RegName("httpbin.org"))),
            path = s"status/${500 + i}"
          )
          val request = Request[Task](
            method = Method.GET,
            uri = uri
          )
          client.expect[String](request).onErrorHandleWith { error =>
            log.error(error)(s"request to $uri")
            Task.raiseError(error)
          }
        }

        for {
          _ <- traverse(list).onErrorFallbackTo(Task(Seq.empty[String]))
          _ <- traverse(list2).onErrorFallbackTo(Task(Seq.empty[String]))
        } yield ()

      }

    val res =
      Await.result(task.timeout(timeout).attempt.runToFuture, timeout)
    res should be('right)
  }

  "Monix scheduler" - {
    "should works with sequence()" in {
      test(Task.sequence, FiniteDuration(10, TimeUnit.SECONDS))
    }

    "but fails with gather()" in {
      test(Task.gatherUnordered, FiniteDuration(10, TimeUnit.SECONDS))
    }
  }

}
