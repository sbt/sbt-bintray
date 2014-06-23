package bintray

private[bintray] object Cache {
  private val underlying =
    new java.util.concurrent.ConcurrentHashMap[String, String]()
  def removeMulti(keys: String*) =
    keys.foreach(underlying.remove)
  def get(key: String) = Option(underlying.get(key))
  def getMulti(keys: String*): Map[String, Option[String]] =
    keys.map { k =>
      (k, get(k))
    }.toMap
  def put(entry: (String, String)) =
    underlying.put(entry._1, entry._2)
  def putMulti(entries: (String, String)*) =
    entries.foreach(put)
}
