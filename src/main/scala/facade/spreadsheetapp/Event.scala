package facade.spreadsheetapp

import cells.Cell.Data

import scala.scalajs.js

/**
 * https://developers.google.com/apps-script/reference/spreadsheet/range
 */
@js.native
trait Event extends js.Object {

  def range: Range = js.native

  def source: SpreadSheet = js.native

  def value: Data = js.native

}
