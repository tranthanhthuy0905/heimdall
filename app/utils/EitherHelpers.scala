package utils

trait EitherHelpers {
  // Behavior:
  // Convert l = List[Either[L, R]] to Either[L, List[R]] in such a way that:
  // - Left(x) if there is any valid Left(x) in l, first x to be left
  // - Right(List[R]) if there is no Left(x) in l
  def toEitherOfList[L,R](eithers: List[Either[L, R]]) : Either[L, List[R]] = {
    val acc: Either[L, List[R]] = Right(Nil)
    eithers.foldLeft(acc) { (acc, elem) =>
      for {
        successAcc <- acc
        successElem <- elem
      } yield successAcc :+ successElem
    }
  }
}
