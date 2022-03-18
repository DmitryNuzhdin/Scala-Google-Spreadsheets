import cells.Cell.Data
import facade.spreadsheetapp._

import java.util.Date
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

  val date = namedRangesMap("date")
  val amount = namedRangesMap("amount")
  val comment = namedRangesMap("comment")
  val balance = namedRangesMap("balance")
  val commit = namedRangesMap("commit")

  val deltaDate = namedRangesMap("deltaDate")
  val deltaBalance = namedRangesMap("deltaBalance")
  val delta = namedRangesMap("delta")

  val statistics = namedRangesMap("statistics")

  val balancesAndMargins = namedRangesMap("balancesAndMargins")


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
      case s: js.Date => Some(s)
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

    def dateOpt: Option[Date] = (data: Any) match {
      case t: js.Date => Some(new java.util.Date(t.getTime().toLong))
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

    def getOptDate(x: Int, y: Int): Option[js.Date] = {
      getOpt(x, y).flatMap((d: Any) => d match {
        case t: js.Date => Some(t)
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
      dateV <- date.getDateOpt
      amountV <- amount.getDoubleOpt
      commentV <- comment.getStringOpt
      balanceV <- balance.getStringOpt
    } {
      dataSheet.getRange(dataSheet.getLastRow() + 1, 1, 1, 4)
        .setValues(js.Array(js.Array(dateV.toJs, amountV, commentV, balanceV)))

      refreshStatistics(2022, 1, balanceV)

      amount.clear()
      comment.clear()
      balance.clear()

      formSheet.getRange("a3").setValue(dateV.toJs)
      formSheet.getRange("a4").setValue(date.getValue())
    }
  }

  def refreshStatistics(year: Int, month: Int, balance: String): Unit = {
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

    val initialBalance = 7500D
    val today: ScalaDate = new js.Date()

    val records: List[Record] = (0 to values.size).flatMap(row =>
      for {
        date <- values.getOptDate(row, 0)
        amount <- values.getOptDouble(row, 1)
        balance <- values.getOptString(row, 3)
      } yield Record(date, amount, balance))
      .filter(r => r.date.getTime() >= dateFrom.getTime() && r.date.getTime() < dateTo.getTime())
      .filter(_.balance == balance)
      .toList

    val balanceData = allDates.zipWithIndex.map { case (date, index) =>
      val expected = initialBalance - ((index + 1) * initialBalance / allDates.size)
      val actual = initialBalance - records.to(LazyList).filter(_.date <= date).foldLeft(0D)(_ + _.amount)
      (date, actual, expected)
    }

    val deltaToday = balanceData.find(_._1 == today).map(d => d._2 - d._3).getOrElse(Double.NaN)

    val cutDay: ScalaDate = (today :: records.map(_.date).maxOption.toList).max

    val outputData = balanceData
      .map(r =>
        if (r._1 > cutDay) (r._1, Double.NaN, r._3) else r
      )
      .map(r => js.Array[Data](r._1.toJs, r._2, r._3))
      .toJSArray


    formSheet.getRange("b6:e6").setValues(js.Array(js.Array[Data](cutDay.toJs, balance, today.toJs, deltaToday)))
    dataSheet.getRange(2, 9, outputData.size, 3).setValues(outputData)
  }

  @JSExportTopLevel("onEdit")
  def onEdit(e: Event): Unit = {
    if (e.range.getA1Notation() == commit.getA1Notation() && commit.getStringOpt.contains("yes")) {
      addBalanceRow()
      commit.setValue("commit?")
    }
  }
}
