object Dictionary {

  sealed trait Direction
  case object EnToNo extends Direction
  case object NoToEn extends Direction

  case class Word(en: String, no: String, rank: Int, scoreMap: Map[Direction, Int]) {
    def score(d: Direction): Int = scoreMap.getOrElse(d, 0)

    def changeScore(d: Direction, change: Int): Word = {
      val newScore = Math.max(score(d) + change, 0)
      copy(scoreMap = scoreMap + (d -> newScore))
    }
  }

}
