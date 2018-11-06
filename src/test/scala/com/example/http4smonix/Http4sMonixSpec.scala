package com.example.http4smonix

import java.util.concurrent.TimeUnit

import cats.Monad
import cats.effect.ConcurrentEffect
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import org.asynchttpclient.DefaultAsyncHttpClientConfig
import org.http4s.Uri.{Authority, RegName, Scheme}
import org.http4s.client.Client
import org.http4s.client.asynchttpclient.AsyncHttpClient
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.util.threads.threadFactory

import scala.concurrent.duration.FiniteDuration

// scalastyle:off underscore.import
import org.http4s._
import org.scalatest._
import cats.syntax.all._
// scalastyle:on underscore.import

final class Http4sMonixSpec
    extends AsyncFreeSpecLike
    with Matchers
    with EitherValues {

  private val log = org.log4s.getLogger

  def testAsyncHttp[F[_]: Monad](traverse: Seq[F[String]] => F[Seq[String]])(
      implicit concurrentEffect: ConcurrentEffect[F],
      monad: Monad[F]): F[Unit] = {

    import concurrentEffect._

    val defaultConfig = new DefaultAsyncHttpClientConfig.Builder()
      .setMaxConnectionsPerHost(4)
      .setMaxConnections(4)
      .setRequestTimeout(60000)
      .setThreadFactory(threadFactory(name = { i =>
        s"http4s-async-http-client-worker-$i"
      }))
      .build()

    def mkRequest(client: Client[F], list: List[Int]) = {
      list.map { i =>
        val uri = Uri(
          scheme = Some(Scheme.https),
          authority = Some(Authority(host = RegName("httpbin.org"))),
          path = s"/status/$i"
        )
        val request = Request[F](
          method = Method.GET,
          uri = uri
        )
        handleErrorWith(client.expect[String](request)) { error =>
          log.error(error)(s"request to $uri")
          raiseError(error)
        }
      }
    }

    AsyncHttpClient
      .resource[F](defaultConfig)
      .use { client =>
        for {
          _ <- handleError(traverse(mkRequest(client, List.range(520, 530))))(
            _ => Seq.empty[String])
          _ <- handleError(traverse(mkRequest(client, List.range(531, 540))))(
            _ => Seq.empty[String])
          _ <- handleError(traverse(mkRequest(client, List.range(541, 550))))(
            _ => Seq.empty[String])
          _ <- handleError(traverse(mkRequest(client, List.range(551, 560))))(
            _ => Seq.empty[String])
          _ <- handleError(traverse(mkRequest(client, List.range(561, 570))))(
            _ => Seq.empty[String])
          _ <- traverse(mkRequest(client, List.fill(20)(200)))
        } yield ()
      }
  }

  private val timeout = FiniteDuration(10, TimeUnit.SECONDS)

  "Monix with AsyncHttpClient" - {
    "should works with sequence()" in {
      testAsyncHttp[Task](Task.sequence)
        .timeout(timeout)
        .attempt
        .runToFuture
        .map(_ should be('right))
    }

    "still works with gather()" in {
      testAsyncHttp[Task](Task.gather)
        .timeout(timeout)
        .attempt
        .runToFuture
        .map(_ should be('right))
    }

    "still works with gatherUnordered()" in {
      testAsyncHttp[Task](Task.gatherUnordered)
        .timeout(timeout)
        .attempt
        .runToFuture
        .map(_ should be('right))
    }
  }

  def testBlaze[F[_]](
      traverse: Seq[F[String]] => F[Seq[String]]
  )(implicit concurrentEffect: ConcurrentEffect[F]): F[Unit] = {
    import concurrentEffect._

    def mkRequest(client: Client[F], list: List[Int]) = {
      list.map { i =>
        val uri = Uri(
          scheme = Some(Scheme.https),
          authority = Some(Authority(host = RegName("httpbin.org"))),
          path = s"/status/$i"
        )
        val request = Request[F](
          method = Method.GET,
          uri = uri
        )
        handleErrorWith(client.expect[String](request)) { error =>
          log.error(error)(s"request to $uri")
          raiseError(error)
        }
      }
    }

    BlazeClientBuilder[F](scala.concurrent.ExecutionContext.Implicits.global)
      .withMaxWaitQueueLimit(200)
      .withRequestTimeout(FiniteDuration(10, TimeUnit.SECONDS))
      .withMaxTotalConnections(4)
      .resource
      .use { client =>
        for {
          _ <- handleError(traverse(mkRequest(client, List.range(520, 530))))(
            _ => Seq.empty[String])
          _ <- handleError(traverse(mkRequest(client, List.range(531, 540))))(
            _ => Seq.empty[String])
          _ <- handleError(traverse(mkRequest(client, List.range(541, 550))))(
            _ => Seq.empty[String])
          _ <- handleError(traverse(mkRequest(client, List.range(551, 560))))(
            _ => Seq.empty[String])
          _ <- handleError(traverse(mkRequest(client, List.range(561, 570))))(
            _ => Seq.empty[String])
          _ <- traverse(mkRequest(client, List.fill(20)(200)))
        } yield ()
      }
  }

  "Monix with Blaze[Task]" - {
    "should works with sequence()" in {
      testBlaze[Task](Task.sequence).attempt
        .timeout(timeout)
        .runToFuture
        .map(_ should be('right))
    }

    "should works with gather()" in {
      testBlaze[Task](Task.gather).attempt
        .timeout(timeout)
        .runToFuture
        .map(_ should be('right))
    }

    "but fails with gatherUnordered()" in {
      testBlaze[Task](Task.gatherUnordered).attempt
        .timeout(timeout)
        .runToFuture
        .map(_ should be('right))
    }
  }
}
