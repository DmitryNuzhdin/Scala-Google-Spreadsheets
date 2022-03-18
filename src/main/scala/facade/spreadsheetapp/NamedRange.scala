package facade.spreadsheetapp

import scala.scalajs.js

@js.native
trait NamedRange extends js.Object {
  def getName(): String = js.native

  def getRange(): Range = js.native

  def remove(): Unit = js.native

  def setName(name: String): NamedRange = js.native

  def setRange(range: Range): NamedRange = js.native
}
