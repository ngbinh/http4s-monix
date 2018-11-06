package com.example.http4smonix

import java.util.concurrent.TimeUnit

import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import org.asynchttpclient.DefaultAsyncHttpClientConfig
import org.http4s.Uri.{Authority, RegName, Scheme}
import org.http4s.client.asynchttpclient.AsyncHttpClient
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.util.threads.threadFactory

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

// scalastyle:off underscore.import
import org.http4s._
import org.scalatest._
// scalastyle:on underscore.import

final class Http4sMonixSpec extends AsyncFreeSpecLike with Matchers with EitherValues {

  private val log = org.log4s.getLogger

  def testAsyncHttp(traverse: Seq[Task[String]] => Task[Seq[String]], timeout: FiniteDuration): Future[Assertion] = {

    val defaultConfig = new DefaultAsyncHttpClientConfig.Builder()
      .setMaxConnectionsPerHost(4)
      .setMaxConnections(4)
      .setRequestTimeout(60000)
      .setThreadFactory(threadFactory(name = { i =>
        s"http4s-async-http-client-worker-$i"
      }))
      .build()

    val task = AsyncHttpClient.resource[Task](defaultConfig)
      .use { client =>
        val list: Seq[Task[String]] = List.range(1, 30).map { i =>
          val uri = Uri(
            scheme = Some(Scheme.https),
            authority = Some(Authority(host = RegName("httpbin.org"))),
            path = s"/status/${500 + i}"
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
          _ <- traverse(list).onErrorFallbackTo(Task(Seq.empty[String]))
        } yield ()
      }


    task.timeout(timeout).attempt.runToFuture.map(_ should be ('right))
  }

  "Monix with AsyncHttpClient" - {
    "should works with sequence()" in {
      testAsyncHttp(Task.sequence, FiniteDuration(10, TimeUnit.SECONDS))
    }

    "still works with gather()" in {
      testAsyncHttp(Task.gather, FiniteDuration(10, TimeUnit.SECONDS))
    }

    "still works with gatherUnordered()" in {
      testAsyncHttp(Task.gatherUnordered, FiniteDuration(10, TimeUnit.SECONDS))
    }
  }

  def testBlaze(traverse: Seq[Task[String]] => Task[Seq[String]], timeout: FiniteDuration): Future[Assertion] = {

    val task = BlazeClientBuilder[Task](scala.concurrent.ExecutionContext.Implicits.global)
      .withMaxWaitQueueLimit(200)
      .withRequestTimeout(FiniteDuration(10, TimeUnit.SECONDS))
      .withMaxTotalConnections(4)
      .resource
      .use { client =>
        val list: Seq[Task[String]] = List.range(1, 30).map { i =>
          val uri = Uri(
            scheme = Some(Scheme.https),
            authority = Some(Authority(host = RegName("httpbin.org"))),
            path = s"/status/${500 + i}"
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
          _ <- traverse(list).onErrorFallbackTo(Task(Seq.empty[String]))
        } yield ()
      }


    task.timeout(timeout).attempt.runToFuture.map(_ should be ('right))
  }

  "Monix with Blaze" - {
    "should works with sequence()" in {
      testBlaze(Task.sequence, FiniteDuration(10, TimeUnit.SECONDS))
    }

    "should works with gatherUnordered()" in {
      testBlaze(Task.gatherUnordered, FiniteDuration(10, TimeUnit.SECONDS))
    }

    "but fails with gather()" in {
      testBlaze(Task.gather, FiniteDuration(10, TimeUnit.SECONDS))
    }
  }
}
