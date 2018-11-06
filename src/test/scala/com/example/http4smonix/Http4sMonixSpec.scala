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
import org.http4s.client.blaze.{BlazeClientConfig, Http1Client}
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
  private val timeout = FiniteDuration(30, TimeUnit.SECONDS)

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

    val client = AsyncHttpClient[F](defaultConfig)
    for {
      _ <- handleError(traverse(mkRequest(client, List.range(500, 550))))(_ =>
        Seq.empty[String])
      _ <- handleError(traverse(mkRequest(client, List.range(500, 550))))(_ =>
        Seq.empty[String])
      _ <- handleError(traverse(mkRequest(client, List.range(500, 550))))(_ =>
        Seq.empty[String])
      _ <- handleError(traverse(mkRequest(client, List.range(500, 550))))(_ =>
        Seq.empty[String])
      _ <- handleError(traverse(mkRequest(client, List.range(500, 550))))(_ =>
        Seq.empty[String])
      _ <- traverse(mkRequest(client, List.fill(5)(200)))
    } yield ()

  }


  "Monix with AsyncHttpClient" - {
    "should works with sequence()" in {
      testAsyncHttp[Task](Task.sequence)
        .timeout(timeout)
        .attempt
        .runAsync
        .map(_ should be('right))
    }

    "still works with gather()" in {
      testAsyncHttp[Task](Task.gather)
        .timeout(timeout)
        .attempt
        .runAsync
        .map(_ should be('right))
    }

    "still works with gatherUnordered()" in {
      testAsyncHttp[Task](Task.gatherUnordered)
        .timeout(timeout)
        .attempt
        .runAsync
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

    val config = BlazeClientConfig.defaultConfig.copy(
      maxTotalConnections = 4
    )

    Http1Client(config).flatMap { client =>
      for {
        _ <- handleError(traverse(mkRequest(client, List.range(500, 550))))(
          _ => Seq.empty[String])
        _ <- handleError(traverse(mkRequest(client, List.range(500, 550))))(
          _ => Seq.empty[String])
        _ <- handleError(traverse(mkRequest(client, List.range(500, 550))))(
          _ => Seq.empty[String])
        _ <- handleError(traverse(mkRequest(client, List.range(500, 550))))(
          _ => Seq.empty[String])
        _ <- handleError(traverse(mkRequest(client, List.range(500, 550))))(
          _ => Seq.empty[String])
        _ <- traverse(mkRequest(client, List.fill(5)(200)))
      } yield ()
    }
  }

  "Monix with Blaze[Task]" - {
    "should works with sequence()" in {
      testBlaze[Task](Task.sequence).attempt
        .timeout(timeout)
        .runAsync
        .map(_ should be('right))
    }

    "should works with gather()" in {
      testBlaze[Task](Task.gather).attempt
        .timeout(timeout)
        .runAsync
        .map(_ should be('right))
    }

    "but fails with gatherUnordered()" in {
      testBlaze[Task](Task.gatherUnordered).attempt
        .timeout(timeout)
        .runAsync
        .map(_ should be('right))
    }
  }
}
