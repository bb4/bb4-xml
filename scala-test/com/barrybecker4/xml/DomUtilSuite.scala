/* Copyright by Barry G. Becker, 2019. Licensed under MIT License: http://www.opensource.org/licenses/MIT */
package com.barrybecker4.xml

import org.scalatest.funsuite.AnyFunSuite
import org.w3c.dom.Node

import java.io.File
import java.net.URL
import java.nio.file.Files


class DomUtilSuite extends AnyFunSuite {

  private val PREFIX = "com/barrybecker4/xml/"

  private def resourceUrl(path: String): URL = {
    val url = Option(getClass.getClassLoader.getResource(path))
      .getOrElse(throw new IllegalArgumentException("failed to create url for  " + path))
    url
  }

  test("read XML file with embedded dtd") {
    val url = resourceUrl(PREFIX + "p_embedded_dtd.xml")
    val xmlDocument = DomUtil.parseXML(url)

    println("doc = " + xmlDocument)
    assertResult("plugins") { xmlDocument.getDocumentElement.getTagName }

    val result = DomUtil.asString(xmlDocument.getDocumentElement.asInstanceOf[Node], 1)
    assert(result.startsWith("    Node: <plugins>  author=\"Barry Becker\"  date=\"10/19/2004\""))
  }

  test("read XML file with remote dtd") {

    val url = resourceUrl(PREFIX + "p_remote_dtd.xml")
    val xmlDocument = DomUtil.parseXML(url)

    println("doc = " + xmlDocument)
    assertResult("plugins") { xmlDocument.getDocumentElement.getTagName }

    val result = DomUtil.asString(xmlDocument.getDocumentElement.asInstanceOf[Node], 1)
    assert(result.startsWith("    Node: <plugins>  author=\"Barry Becker\"  date=\"10/19/2004\""))
  }

  test("read XML web-app file with remote dtd") {

    val url = resourceUrl(PREFIX + "web-app.xml")
    val xmlDocument = DomUtil.parseXML(url)

    println("doc = " + xmlDocument)
    assertResult("web-app") { xmlDocument.getDocumentElement.getTagName }

    val result = DomUtil.asString(xmlDocument.getDocumentElement.asInstanceOf[Node], 1)

    assert(result.startsWith("    Node: <web-app>"))
  }

  test("parse malformed XML file throws IllegalStateException") {
    val f = File.createTempFile("bad", ".xml")
    try {
      Files.writeString(f.toPath, "<not>")
      assertThrows[IllegalStateException](DomUtil.parseXMLFile(f))
    } finally f.delete()
  }

  test("substitute use refs without external DTD ID typing") {
    val f = File.createTempFile("use-refs", ".xml")
    try {
      Files.writeString(f.toPath,
        """<?xml version="1.0"?>
          |<!DOCTYPE hierarchy SYSTEM "https://example.invalid/missing.dtd">
          |<hierarchy>
          |  <node id="leaf" label="leaf"/>
          |  <node id="parent" label="parent">
          |    <use ref="leaf"/>
          |  </node>
          |</hierarchy>
          |""".stripMargin)
      val doc = DomUtil.parseXMLFile(f).get
      val asText = DomUtil.asString(doc.getDocumentElement.asInstanceOf[Node], 0)
      assert(asText.contains("id=\"leaf\""), asText)
      assert(!asText.contains("<use>"), "use elements should be substituted away: " + asText)
      // parent should now have a cloned leaf child (two leaf id occurrences)
      assert(asText.split("id=\"leaf\"").length - 1 == 2, asText)
    } finally f.delete()
  }

  test("buildIdIndex keeps the first id and skips blanks") {
    val doc = DomUtil.buildDom
    val root = doc.getDocumentElement
    val first = doc.createElement("node")
    first.setAttribute("id", "dup")
    first.setAttribute("label", "first")
    val text = doc.createTextNode("ignore")
    val blank = doc.createElement("node")
    blank.setAttribute("id", "")
    val second = doc.createElement("node")
    second.setAttribute("id", "dup")
    second.setAttribute("label", "second")
    val other = doc.createElement("node")
    other.setAttribute("id", "other")
    root.appendChild(first)
    root.appendChild(text)
    root.appendChild(blank)
    root.appendChild(second)
    root.appendChild(other)

    val index = DomQuery.buildIdIndex(root)

    assert(index.keySet == Set("dup", "other"))
    assert(index("dup").getAttribute("label") == "first")
    assert(DomQuery.findElementById(doc, "dup", index).contains(first))
    assert(DomQuery.findElementById(doc, "missing", index).isEmpty)
  }

  test("duplicate ids substitute the first element") {
    val f = File.createTempFile("dup-ids", ".xml")
    try {
      Files.writeString(f.toPath,
        """<?xml version="1.0"?>
          |<root>
          |  <node id="x" label="first"/>
          |  <node id="x" label="second"/>
          |  <use ref="x"/>
          |</root>
          |""".stripMargin)
      val doc = DomUtil.parseXMLFile(f).get
      val text = DomUtil.asString(doc.getDocumentElement, 0)
      assert(text.split("label=\"first\"").length - 1 == 2, text)
      assert(text.split("label=\"second\"").length - 1 == 1, text)
      assert(!text.contains("<use>"), text)
    } finally f.delete()
  }

  test("nested and repeated use refs resolve without a per-ref tree walk") {
    val f = File.createTempFile("many-uses", ".xml")
    try {
      val uses = (1 to 5).map(_ => "    <use ref=\"parent\"/>").mkString("\n")
      Files.writeString(f.toPath,
        s"""<?xml version="1.0"?>
           |<root>
           |  <node id="leaf" label="leaf"/>
           |  <node id="parent" label="parent">
           |    <use ref="leaf"/>
           |  </node>
           |$uses
           |</root>
           |""".stripMargin)
      val doc = DomUtil.parseXMLFile(f).get
      val text = DomUtil.asString(doc.getDocumentElement, 0)
      assert(!text.contains("<use>"), text)
      // original leaf, plus one copy under the original parent, plus one under each of 5 substituted parents
      assert(text.split("id=\"leaf\"").length - 1 == 7, text)
      assert(text.split("id=\"parent\"").length - 1 == 6, text)
    } finally f.delete()
  }

  test("missing use ref throws IllegalStateException") {
    val f = File.createTempFile("missing-ref", ".xml")
    try {
      Files.writeString(f.toPath,
        """<?xml version="1.0"?>
          |<root><use ref="absent"/></root>
          |""".stripMargin)
      val ex = intercept[IllegalStateException](DomUtil.parseXMLFile(f).get)
      assert(ex.getMessage.contains("absent"))
    } finally f.delete()
  }
}
