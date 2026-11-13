/* Copyright by Barry G. Becker, 2000-2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT */
package com.barrybecker4.xml

import org.w3c.dom.{Document, Element, Node}

import scala.collection.immutable.Map
import scala.collection.mutable.ListBuffer

/** Document transformations such as use-element substitution and whitespace cleanup. */
object DomTransformer {
  private val USE_ELEMENT = "use"

  private def isWhitespaceOnlyTextNode(n: Node): Boolean = {
    val name = n.getNodeName
    name != null && name.startsWith("#text") && {
      val text = n.getNodeValue
      text != null && text.matches("[ \\t\n\\x0B\\f\\r]*")
    }
  }

  /** Go through the dom hierarchy and remove spurious text nodes and also
    * replace "use" nodes with a deep copy of what they refer to.
    */
  private[xml] def postProcessDocument(root: Node, document: Document, replaceUseWithDeepCopy: Boolean): Unit = {
    val index = DomQuery.buildIdIndex(document.getDocumentElement)
    postProcess(root, document, replaceUseWithDeepCopy, index)
  }

  private def postProcess(
      root: Node,
      document: Document,
      replaceUseWithDeepCopy: Boolean,
      index: Map[String, Element]): Unit = {
    val l = root.getChildNodes
    val deleteList = ListBuffer[Node]()
    var i = 0
    while (i < l.getLength) {
      val n = l.item(i)
      val name = n.getNodeName
      if (isWhitespaceOnlyTextNode(n))
        deleteList.append(n)
      postProcess(n, document, replaceUseWithDeepCopy, index)
      if (name != null && USE_ELEMENT == name)
        substituteUseElement(root, n, document, replaceUseWithDeepCopy, index)
      i += 1
    }
    deleteList.foreach(c => root.removeChild(c))
  }

  private def substituteUseElement(
      root: Node,
      n: Node,
      document: Document,
      replaceUseWithDeepCopy: Boolean,
      index: Map[String, Element]): Unit = {
    val attrs = n.getAttributes
    val attr = attrs.item(0)
    assert("ref" == attr.getNodeName, "attr name=" + attr.getNodeName)
    val attrValue = attr.getNodeValue
    val element = DomQuery.findElementById(document, attrValue, index).getOrElse(
      throw new IllegalStateException(
        "No element with id '" + attrValue + "' for <use> reference"))
    val clonedElement = element.cloneNode(replaceUseWithDeepCopy)
    postProcess(clonedElement, document, replaceUseWithDeepCopy, index)
    root.replaceChild(clonedElement, n)
  }
}
