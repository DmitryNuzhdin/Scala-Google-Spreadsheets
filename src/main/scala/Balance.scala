import cells.Cell.Data
import facade.spreadsheetapp._

import java.time.LocalDate
import java.util.{Calendar, Date}
import scala.scalajs.js
import scala.scalajs.js.annotation.JSExportTopLevel
import js.JSConverters._

object Balance {

  val namedRangesMap: Map[String, Range] = SpreadsheetApp
    .getActiveSpreadsheet()
    .getNamedRanges()
    .to(LazyList)
    .map(nr => nr.getName() -> nr.getRange())
    .toMap

  val date: Option[Range] = namedRangesMap.get("date")
  val amount: Option[Range] = namedRangesMap.get("amount")
  val comment: Option[Range] = namedRangesMap.get("comment")
  val balance: Option[Range] = namedRangesMap.get("balance")
  val commit: Option[Range] = namedRangesMap.get("commit")

  val deltaDate: Option[Range] = namedRangesMap.get("deltaDate")
  val deltaBalance: Option[Range] = namedRangesMap.get("deltaBalance")
  val delta: Option[Range] = namedRangesMap.get("delta")
  val fullBalance: Option[Range] = namedRangesMap.get("fullBalance")
  val balanceLeft: Option[Range] = namedRangesMap.get("balanceLeft")

  val statistics: Option[Range] = namedRangesMap.get("statistics")

  val balancesAndMargins: Option[Range] = namedRangesMap.get("balancesAndMargins")

  val balances: Map[String, Double] = balancesAndMargins
    .toList
    .flatMap(bnm => arrayArrayJsToScala(bnm.getValues()))
    .to(LazyList)
    .flatMap { v =>
      for {
        name <- v.headOption.flatMap(_.stringOpt)
        value <- v.drop(1).headOption.flatMap(_.doubleOpt)
      } yield name -> value
    }.toMap

  def dataSheet: Sheet = SpreadsheetApp.getActiveSpreadsheet().getSheetByName("data")

  def formSheet: Sheet = SpreadsheetApp.getActiveSpreadsheet().getSheetByName("form")

  case class RangeOpt(r: Range) {
    def getStringOpt: Option[String] = (r.getValue(): Any) match {
      case s: String => Some(s)
      case _ => None
    }

    def getDoubleOpt: Option[Double] = (r.getValue(): Any) match {
      case d: Double => Some(d)
      case _ => None
    }

    def getBooleanOpt: Option[Boolean] = (r.getValue(): Any) match {
      case b: Boolean => Some(b)
      case _ => None
    }

    def getDateOpt: Option[ScalaDate] = (r.getValue(): Any) match {
      case s: js.Date => Some(s.plusDays(1))
      case _ => None
    }

    def setValue(d: Data): Unit = r.setValues(js.Array(js.Array(d)))

    def setValues(a: js.Array[js.Array[Data]]): Unit = r.setValues(a)
  }

  implicit def rangeToRangeOpt(r: Range): RangeOpt = RangeOpt(r)

  //def dataSheet: Sheet = SpreadsheetApp.getActiveSpreadsheet().getSheetByName("data")

  //def formSheet: Sheet = SpreadsheetApp.getActiveSpreadsheet().getSheetByName("form")

  implicit def wrapArray(array: js.Array[js.Array[Data]]): ArrayWrap =
    ArrayWrap(array)

  def arrayJsToScala[T](a: js.Array[T]): Vector[T] = a.toVector

  def arrayScalaToJs[T](a: Iterable[T]): js.Array[T] = a.toJSArray

  def arrayArrayJsToScala[T](a: js.Array[js.Array[T]]): Vector[Vector[T]] = a.toVector.map(_.toVector)

  def arrayArrayScalaToJs[T](a: Iterable[Iterable[T]]): js.Array[js.Array[T]] = a.toJSArray.map(_.toJSArray)

  //implicit def dateJsToScala(d: js.Date): java.util.Date = new java.util.Date(d.getTime().toLong)

  //implicit def dateScalaToJs(d: )

  implicit def wrapData(d: Data): DataWrap = DataWrap(d)

  case class DataWrap(data: Data) {
    def doubleOpt: Option[Double] = (data: Any) match {
      case t: Double => Some(t)
      case _ => None
    }

    def stringOpt: Option[String] = (data: Any) match {
      case t: String => Some(t)
      case _ => None
    }

    def dateOpt: Option[ScalaDate] = (data: Any) match {
      case t: js.Date => Some(t.plusDays(1))
      case _ => None
    }

    def booleanOpt: Option[Boolean] = (data: Any) match {
      case t: Boolean => Some(t)
      case _ => None
    }
  }

  implicit def dateToScalaDate(d: js.Date): ScalaDate = ScalaDate(d)

  implicit def scalaDateToDate(d: ScalaDate): js.Date = d.toJs

  case class ScalaDate(day: Long) extends Ordered[ScalaDate] {
    override def compare(that: ScalaDate): Int = day.compare(that.day)

    def plusDays(d: Long): ScalaDate = copy(day = day + d)

    def toJs: js.Date = new js.Date((day * 1000D * 24D * 3600D))
  }

  object ScalaDate {
    def apply(a: js.Date): ScalaDate = {
      a.setHours(1)
      ScalaDate((a.getTime() / (24D * 3600D * 1000D)).round)
    }
  }

  case class ArrayWrap(array: js.Array[js.Array[Data]]) {

    val a: Vector[Vector[Data]] = arrayArrayJsToScala(array)

    def getOpt(x: Int, y: Int): Option[Data] = {
      if (x < array.size && y < array.apply(x).size)
        Some(array.apply(x).apply(y))
      else None
    }

    def getOptDouble(x: Int, y: Int): Option[Double] = {
      getOpt(x, y).flatMap((d: Any) => d match {
        case t: Double => Some(t)
        case _ => None
      })
    }

    def getOptDate(x: Int, y: Int): Option[ScalaDate] = {
      getOpt(x, y).flatMap((d: Any) => d match {
        case t: js.Date => Some(t.plusDays(1))
        case _ => None
      })
    }

    def getOptString(x: Int, y: Int): Option[String] = {
      getOpt(x, y).flatMap((d: Any) => d match {
        case t: String => Some(t)
        case _ => None
      })
    }
  }

  def addBalanceRow(): Unit = {
    for {
      dateV <- date.flatMap(_.getDateOpt)
      amountV <- amount.flatMap(_.getDoubleOpt)
      commentV <- comment.flatMap(_.getStringOpt)
      balanceV <- balance.flatMap(_.getStringOpt)
    } {
      dataSheet.getRange(dataSheet.getLastRow() + 1, 1, 1, 4)
        .setValues(js.Array(js.Array(dateV.toJs, amountV, commentV, balanceV)))

      refreshStatistics(balanceV)

      amount.foreach(_.clear())
      comment.foreach(_.clear())
      balance.foreach(_.clear())
    }
  }

  def refreshStatistics(balance: String): Unit = {
    balances.get(balance).foreach { balanceMargin =>

      val todayJs: js.Date = new js.Date()

      val today: ScalaDate = todayJs

      val day = todayJs.getUTCDate().toInt
      val month0 = todayJs.getMonth().toInt - (if (day < 20) 1 else 0)
      val year = todayJs.getUTCFullYear().toInt - (if (month0 < 0) 1 else 0)
      val month = if (month0 < 0) month0 + 12 else month0

      val dataRange = dataSheet.getRange(2, 1, dataSheet.getLastRow(), 4)
      dataRange.sort(1)

      val statisticsRange = dataSheet.getRange(2, 9, 31, 3)
      statisticsRange.clear()

      val dateFrom: ScalaDate = new js.Date(year, month, 20)
      val dateTo: ScalaDate = new js.Date(if (month >= 11) year + 1 else year, (month + 1) % 12, 20)

      val allDates: IndexedSeq[ScalaDate] = (0L to 31L)
        .map(dateFrom.plusDays)
        .filter(_ < dateTo)
        .toVector

      case class Record(date: ScalaDate, amount: Double, balance: String)

      val values = dataRange.getValues()

      val records: List[Record] = (0 to values.size).flatMap(row =>
        for {
          date <- values.getOptDate(row, 0)
          amount <- values.getOptDouble(row, 1)
          balance <- values.getOptString(row, 3)
        } yield Record(date, amount, balance))
        .filter(r => r.date >= dateFrom && r.date < dateTo)
        .filter(_.balance == balance)
        .toList

      val balanceData: Seq[(ScalaDate, Double, Double)] = allDates.zipWithIndex.map { case (date, index) =>
        val expected = balanceMargin - ((index + 1) * balanceMargin / allDates.size)
        val actual = balanceMargin - records.to(LazyList).filter(_.date <= date).foldLeft(0D)(_ + _.amount)
        (date, actual, expected)
      }

      val deltaToday = balanceData.find(_._1 == today).map{d => d._2 - d._3}.getOrElse(Double.NaN)

      val cutDay: ScalaDate = (today :: records.map(_.date).maxOption.toList).max

      val outputData = balanceData
        .map(r =>
          if (r._1 > cutDay) (r._1, Double.NaN, r._3) else r
        )
        .map(r => js.Array[Data](r._1.toJs, r._2, r._3))
        .toJSArray

      deltaDate.foreach(_.setValue(today.toJs))
      deltaBalance.foreach(_.setValue(balance))
      delta.foreach(_.setValue(deltaToday))
      fullBalance.foreach(_.setValue(balanceMargin))

      for{
        l <- balanceData.lastOption
        bf <- balanceLeft
      } bf.setValue(l._2)

      dataSheet.getRange(2, 9, outputData.size, 3).setValues(outputData)
    }
  }

  @JSExportTopLevel("onEdit")
  def onEdit(e: Event): Unit = {
    if (commit.exists(c => e.range.getA1Notation() == c.getA1Notation() && c.getStringOpt.contains("yes"))) {
      addBalanceRow()
      commit.foreach(_.setValue("commit?"))
    } else if (deltaBalance.exists(r => e.range.getA1Notation() == r.getA1Notation())) {
      deltaBalance.flatMap(_.getStringOpt).foreach(refreshStatistics)
    }
  }
}
