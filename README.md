## Demonstrate the behavior of Monix `Task.sequence` and `Task.gather` on http4s

run `sbt test` to see how in `Task.sequence` failures, http4s can be cleanly clear out the connection pool. While in the case of `Task.gather`, failures continue to hold up http4s pool and leads to timing out.