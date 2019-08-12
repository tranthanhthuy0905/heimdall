package utils

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object Predicate {

  // TODO: delete the predicate after the nino.enforce is moved to an action builder.
  def apply(condition: Boolean)(fail: Exception): Future[Unit] =
    if (condition) Future(()) else Future.failed(fail)
}
