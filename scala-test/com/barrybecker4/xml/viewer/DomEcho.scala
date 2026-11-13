/* Copyright by Barry G. Becker, 2017. Licensed under MIT License: http://www.opensource.org/licenses/MIT*/
package com.barrybecker4.xml.viewer

import com.barrybecker4.xml.DomUtil
import org.w3c.dom.Document
import javax.swing._
import javax.swing.border.BevelBorder
import javax.swing.border.CompoundBorder
import javax.swing.border.EmptyBorder
import javax.swing.event.TreeSelectionEvent
import javax.swing.event.TreeSelectionListener
import javax.swing.filechooser.FileNameExtensionFilter
import java.awt._
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.File


/**
  * Graphically view some XML document in a swing UI.
  * Test/demo only — not part of the published library.
  */
object DomEcho {
  private val WINDOW_HEIGHT = 460
  private val LEFT_WIDTH = 300
  private val RIGHT_WIDTH = 340
  private val WINDOW_WIDTH = LEFT_WIDTH + RIGHT_WIDTH

  def makeFrame(document: Document, initialFile: Option[File] = None): Unit = {
    val frame = new JFrame(frameTitle(initialFile))
    frame.addWindowListener(new WindowAdapter() {
      override def windowClosing(e: WindowEvent): Unit = {
        System.exit(0)
      }
    })
    val echoPanel = new DomEcho(document)
    frame.setJMenuBar(createMenuBar(frame, echoPanel))
    frame.getContentPane.add(echoPanel)
    frame.pack()
    val screenSize = Toolkit.getDefaultToolkit.getScreenSize
    val w = WINDOW_WIDTH + 10
    val h = WINDOW_HEIGHT + 10
    frame.setLocation(screenSize.width / 3 - (w >> 1), (screenSize.height >> 1) - (h >> 1))
    frame.setSize(w, h)
    frame.setVisible(true)
  }

  private def frameTitle(file: Option[File]): String =
    file.map(f => s"DOM Echo — ${f.getName}").getOrElse("DOM Echo")

  private def createMenuBar(frame: JFrame, echoPanel: DomEcho): JMenuBar = {
    val openItem = new JMenuItem("Open…")
    openItem.addActionListener(_ => openFile(frame, echoPanel))
    val fileMenu = new JMenu("File")
    fileMenu.add(openItem)
    val menuBar = new JMenuBar
    menuBar.add(fileMenu)
    menuBar
  }

  private def openFile(frame: JFrame, echoPanel: DomEcho): Unit = {
    val chooser = new JFileChooser
    chooser.setCurrentDirectory(new File("."))
    chooser.setFileFilter(new FileNameExtensionFilter("XML files", "xml"))
    if (chooser.showOpenDialog(frame) != JFileChooser.APPROVE_OPTION) return

    val file = chooser.getSelectedFile
    DomUtil.parseXMLFile(file) match {
      case Some(document) =>
        echoPanel.loadDocument(document)
        frame.setTitle(frameTitle(Some(file)))
      case None =>
        JOptionPane.showMessageDialog(
          frame,
          s"Could not parse XML file:\n${file.getAbsolutePath}",
          "Open failed",
          JOptionPane.ERROR_MESSAGE
        )
    }
  }

  /** for testing */
  def main(argv: Array[String]): Unit = {
    if (argv.length < 1) {
      makeFrame(DomUtil.buildDom)
    } else {
      val file = new File(argv(0))
      DomUtil.parseXMLFile(file) match {
        case Some(document) => makeFrame(document, Some(file))
        case None =>
          System.err.println(s"Could not parse XML file: ${argv(0)}")
          System.exit(1)
      }
    }
  }
}

/**
  * @param initialDocument the xml document to show initially
  */
class DomEcho(initialDocument: Document) extends JPanel {
  private var document: Document = initialDocument
  this.setBorder(createBorder)
  private val tree = new JTree(new DomToTreeModelAdapter(document))
  private val treeView = new JScrollPane(tree)
  treeView.setPreferredSize(new Dimension(DomEcho.LEFT_WIDTH, DomEcho.WINDOW_HEIGHT))
  private val htmlPane = new JEditorPane
  htmlPane.setEditable(false)
  private val htmlView = new JScrollPane(htmlPane)
  htmlView.setPreferredSize(new Dimension(DomEcho.RIGHT_WIDTH, DomEcho.WINDOW_HEIGHT))
  connectViews(tree, htmlPane)
  private val splitPane: JSplitPane = createSplitPane(treeView, htmlView)
  this.setLayout(new BorderLayout)
  this.add(splitPane)

  /** Replace the displayed document (e.g. after File → Open…). */
  def loadDocument(doc: Document): Unit = {
    document = doc
    tree.setModel(new DomToTreeModelAdapter(document))
    htmlPane.setText("")
  }

  /**
    * Wire the two views together. Use a selection listener
    * created with an anonymous inner-class adapter.
    *
    * @param tree     left view
    * @param htmlPane right view detail
    */
  private def connectViews(tree: JTree, htmlPane: JEditorPane): Unit = {
    tree.addTreeSelectionListener(new TreeSelectionListener() {
      override def valueChanged(e: TreeSelectionEvent): Unit = {
        val p = e.getNewLeadSelectionPath
        if (p != null) {
          val adpNode = p.getLastPathComponent.asInstanceOf[AdapterNode]
          val attribMap = adpNode.getDomNode.getAttributes
          val attribs = DomUtil.getAttributeList(attribMap)
          htmlPane.setText(attribs)
        }
      }
    })
  }

  /**
    * @return new split-pane view with left and right view children
    */
  private def createSplitPane(treeView: JScrollPane, htmlView: JScrollPane) = {
    val splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeView, htmlView)
    splitPane.setContinuousLayout(true)
    splitPane.setDividerLocation(DomEcho.LEFT_WIDTH)
    splitPane.setPreferredSize(new Dimension(DomEcho.WINDOW_WIDTH + 10, DomEcho.WINDOW_HEIGHT + 10))
    splitPane
  }

  /** Make a nice border */
  private def createBorder = {
    val eb = new EmptyBorder(5, 5, 5, 5)
    val bb = new BevelBorder(BevelBorder.LOWERED)
    val cb = new CompoundBorder(eb, bb)
    new CompoundBorder(cb, eb)
  }
}
