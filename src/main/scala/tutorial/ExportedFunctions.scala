package tutorial

import java.time.LocalDateTime

import cells.Cell
import cells.Cell._
import cells.customfunctions.{Encoder, Input}
import exceptions.WrongDataTypeException
import facade.spreadsheetapp.{Event, Sheet, SpreadsheetApp}

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}
import js.JSConverters._

import java.util.Date

/**
 * This object contains examples of functions that are exported to Google custom functions.
 */
object ExportedFunctions {

  def dataSheet: Sheet = SpreadsheetApp.getActiveSpreadsheet().getSheetByName("data")
  def formSheet: Sheet = SpreadsheetApp.getActiveSpreadsheet().getSheetByName("form")

  implicit def wrapArray(array: js.Array[js.Array[Data]]): ArrayWrap =
    ArrayWrap(array)

  implicit def arrayJsToScala[T](a: js.Array[T]):Vector[T] = a.toVector

  implicit def arrayScalaToJs[T](a: Iterable[T]): js.Array[T] = a.toJSArray

  implicit def arrayArrayJsToScala[T](a: js.Array[js.Array[T]]):Vector[Vector[T]] = a.toVector.map(_.toVector)

  implicit def arrayArrayScalaToJs[T](a: Iterable[Iterable[T]]): js.Array[js.Array[T]] = a.toJSArray.map(_.toJSArray)

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


  case class ArrayWrap(array: js.Array[js.Array[Data]]) {

    val a: Vector[Vector[Data]] = array

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
    val values = formSheet.getRange("a2:e2").getValues()
    for {
      date <- values.getOptDate(0, 0)
      amount <- values.getOptDouble(0, 1)
      comment <- values.getOptString(0, 2)
      balance <- values.getOptString(0, 3)
    } {
      dataSheet.getRange(dataSheet.getLastRow() + 1, 1, 1, 4)
        .setValues(js.Array(js.Array(date, amount, comment, balance)))

      refreshStatistics(2022, 1, balance)

      formSheet.getRange("b2:e2").clear()
    }
  }

  def refreshStatistics(year: Int, month: Int, balance: String): Unit = {
    val dataRange = dataSheet.getRange(2, 1, dataSheet.getLastRow(), 4)
    dataRange.sort(1)

    val statisticsRange = dataSheet.getRange(2, 9, 31, 2)
    statisticsRange.clear()

    val dateFrom = new js.Date(year, month, 20)
    val dateTo = new js.Date(if (month >= 11) year + 1 else year, (month + 1) % 12, 20)

    case class Record(date: js.Date, amount: Double, balance: String)

    val values = dataRange.getValues()

    val groupedData: js.Array[js.Array[Data]] = (0 to values.size).flatMap(row =>
      for {
        date <- values.getOptDate(row, 0)
        amount <- values.getOptDouble(row, 1)
        balance <- values.getOptString(row, 3)
      } yield Record(date, amount, balance))
      .filter(r => r.date.getTime() >= dateFrom.getTime() && r.date.getTime() < dateTo.getTime())
      .filter(_.balance == balance)
      .groupMapReduce(_.date)(_.amount)(_ + _)
      .toList
      .sortBy(_._1.getTime())
      .map{e => js.Array[Data](e._1, e._2)}
      .toJSArray


    dataSheet.getRange(2, 9, groupedData.size, 2).setValues(groupedData)

  }

  @JSExportTopLevel("onEdit")
  def onEdit(e: Event): Unit = {
    if (e.range.getColumn() == 5 && e.range.getRow() == 2) {
      addBalanceRow()
      e.range.setValues(js.Array(js.Array("commit?")))
    }
  }
}
