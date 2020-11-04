package utils

trait ResponseHeaderHelpers {

  protected val blacklist = Seq("content-length", "content-type", "transfer-encoding")

  private def byBlackList(key: String): Boolean = blacklist.contains(key.toLowerCase)

  def withHeader(headers: Map[String, Seq[String]]): Map[String, String] = {
    headers
      .filterKeys(!byBlackList(_))
      .mapValues(_.headOption.getOrElse(""))
  }

}
