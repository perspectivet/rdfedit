package com.github.perspectivet

import com.github.perspectivet.bigdata.rest.{Rest,Document}
import com.github.perspectivet.JavaUtils._

import com.vaadin.event.Action
import com.vaadin.event.Action._
import com.vaadin.data.util.IndexedContainer
import com.vaadin.data.{Item,Container}
import com.vaadin.ui.{ Button => VButton, TextField }
import com.vaadin.ui.{ CustomComponent,Layout,GridLayout,ComboBox,Component}
import com.vaadin.ui.AbstractSelect._
import com.vaadin.ui.themes.BaseTheme
import com.vaadin.terminal.Sizeable

import org.openrdf.model.{Value,Resource,URI}
import org.openrdf.query.BindingSet
import org.openrdf.query.TupleQueryResult
import org.openrdf.model.impl.{URIImpl,LiteralImpl}

import java.util.{LinkedList => JLinkedList,Collection => JCollection}
import scala.collection.immutable.Map
import scala.collection.JavaConverters._
import vaadin.scala._

//class Document


class DocumentActionHandler(val table:Table) extends Action.Handler {
  val ACTION_NEW = new Action("New")
  val ACTION_DELETE = new Action("Delete")
  val ACTION_LOG = new Action("Log")
  val ACTIONS = Array(ACTION_NEW,ACTION_DELETE/*,ACTION_LOG*/)

  var d1:Document = null
  var d2:Document = null

  override def getActions(target:Object, sender:Object) = ACTIONS

  override def handleAction(action:Action, target:Object, sender:Object) = {
    println("Handle that action...")
    if (ACTION_NEW == action) {
      println("Add...")
      val itemId = table.addItemAfter(sender)
      println("target:" + target.toString)
      println("sender:" + sender.toString)
      val ds = table.getContainerDataSource
      val ids = ds.getItem(itemId).getItemPropertyIds
      val idit = ids.iterator
      while(idit.hasNext) {
	val idnext = idit.next
	println("next:" + idnext.toString + "," + ds.getItem(itemId).getItemProperty(idnext))
	val prop = ds.getItem(itemId).getItemProperty(idnext)
	idnext.toString match {
	  case "Property" => prop.setValue(new URIImpl("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"))
	  case "Value" => prop.setValue(new LiteralImpl("nothing"))
	}
      }
      table.setEditable(true)
      println("itemId:" + itemId.toString)
      table.refreshRowCache()
      table.requestRepaint()
    } else if (ACTION_DELETE == action) {
      println("Delete...")
      table.removeItem(target)
      table.refreshRowCache()
      table.requestRepaint()
    } else if (ACTION_LOG == action) {
      println("Log...")
    } 
  }
}


object ResUtils {
  val convertible = List(
    ("http://www.w3.org/1999/02/22-rdf-syntax-ns",("#","rdf")),
    ("http://www.w3.org/2000/01/rdf-schema",("#","rdfs")),
    ("http://bbp.epfl.ch/ontology/morphology",("#","mt")),
    ("http://purl.uniprot.org/core",("/","up-core"))
  )

  val prefix = convertible.map { 
    case(long,(sep,short)) => 
      "PREFIX " + short + ": <" + long + sep + ">"
  }

  val shortenMap = convertible.toMap

  val expandMap = convertible.map{ case(long,(sep,short)) => (short,(sep,long)) }.toMap
  
  def urlreduce(url:String):String = {
    val (p,s) = urlsplit(url)
    println("p(%s),s(%s)" format (p,s))
    shortenMap.get(p) match {
      case Some((sep,redp)) => redp + ":" + s
      case None => url
    }
  }

  def urlsplit(s:String):(String,String) = {
    val lastHash = Math.max(s.lastIndexOf("#"),s.lastIndexOf("/"))
    if (lastHash >= 0) {
      val (pref,suff) = s.splitAt(lastHash)
      (pref,suff.tail)
    } else {
      ("",s)
    }
  }

  def toAbbrevString(value:Value):String = { 
    value match {
      case r:Resource => urlreduce(r.stringValue)
      case v:Value => v.stringValue
    }
  }

  def toValue(s:String):Value = {
    val colon = s.indexOf(':')
    if(colon >= 0) {
      val (prefix,suffix) = s.splitAt(s.indexOf(':'))
      //suffix includes the ':' at the front
      expandMap.get(prefix) match {
	case Some((sep,long)) => new URIImpl(long + sep + suffix.tail)
	case _ => new LiteralImpl(s)
      }
    } else {
      new LiteralImpl(s)
    }
  }

}

object DocUtils {
  def getPredicateComponent(v:Value, parent:DocumentComponent):Component = {
    val label = v match {
      case u:URI => {
	println("uri : " + v.stringValue)
	val button = new Button(ResUtils.toAbbrevString(u), 
				action = _ => parent.setSubjectPanel(u))
	button.setData(u)
	button.setStyleName(BaseTheme.BUTTON_LINK)
	button
      }
      case r:Resource => {
	println("resource : " + v.stringValue)
	new Label(ResUtils.toAbbrevString(r))
      }
      case _v:Value => {
	println("value : " + v.stringValue)
	new Label(ResUtils.toAbbrevString(_v))
      }
      case _ => {
	println("Any : "+v.toString)
	new Label(v.toString)
      }
    }

    label
  }

    def getEditableComponent(v:Value, comboValues:JCollection[Value]):Component = {
    val label = v match {
      case u:URI => {
	println("uri : " + v.stringValue)
	val value = ResUtils.toAbbrevString(u)
	val comboBox = new ComboBox
	jforeach[Value](comboValues,c => 
	  comboBox.addItem(ResUtils.toAbbrevString(c)))
	comboBox.setValue(value)
	comboBox.setTextInputAllowed(true)
	comboBox.setFilteringMode(Filtering.FILTERINGMODE_CONTAINS)
	comboBox.setNewItemsAllowed(true)
	//comboBox.setContainerDataSource(comboValues)
	comboBox
      }
      case r:Resource => {
	println("resource : " + v.stringValue)
	val te = new TextField
	te.setValue(ResUtils.toAbbrevString(r))
	te
      }
      case _v:Value => {
	println("value : " + v.stringValue)
	val te = new TextField
	te.setValue(ResUtils.toAbbrevString(_v))
	
	te
      }
      case _ => {
	println("Any : "+v.toString)
	val te = new TextField
	te.setValue(v.toString)
	te
      }
    }

    label
  }

  def getComponent(v:Value):Component = {
    val label = v match {
      case u:URI => {
	println("uri : " + v.stringValue)
	val button = new Button(ResUtils.toAbbrevString(u))
	button.setData(u)
	button.setStyleName(BaseTheme.BUTTON_LINK)
	button
      }
      case r:Resource => {
	println("resource : " + v.stringValue)
	new Label(ResUtils.toAbbrevString(r))
      }
      case _v:Value => {
	println("value : " + v.stringValue)
	new Label(ResUtils.toAbbrevString(_v))
      }
      case _ => {
	println("Any : "+v.toString)
	new Label(v.toString)
      }
    }
    label
  }

  def getSubjectComponent(v:Value, parent:DocumentComponent):Component = {
    val label = v match {
      case u:URI => {
	println("uri : " + v.stringValue)
	val button = new Button(ResUtils.toAbbrevString(u), 
		   action = _ => parent.setSubjectPanel(u))
	button.setData(u)
	button.setStyleName(BaseTheme.BUTTON_LINK)
	button
      }
      case r:Resource => {
	println("resource : " + v.stringValue)
	new Label(ResUtils.toAbbrevString(r))
      }
      case _v:Value => {
	println("value : " + v.stringValue)
	new Label(ResUtils.toAbbrevString(_v))
      }
      case _ => {
	println("Any : "+v.toString)
	new Label(v.toString)
      }
    }


    label
  }
}

class DocumentComponent(val s:URI, val rest:Rest) extends CustomComponent with VButton.ClickListener {
  val document = this
  setCompositionRoot(getSubjectTable(s))

  override def buttonClick(event:VButton#ClickEvent) {
    event.getSource match {
      case b:Button => 
	b.getData match {
	  case u:URI =>
	    println("clicked button with value(%s)" format u.stringValue)
	    setSubjectPanel(u)
	  case _ =>
	    println("clicked a non-URI button")
	}
      case _ =>
	println("clicked a non button")
    }
  }

/*  
  def getSubjectPanel(subject:URI):Panel = {
    val doc = rest.getSubjectDocument(subject,List())

    val predResults = rest.sparql("SELECT distinct ?p WHERE { <%s> ?p ?o }" format doc.subject.stringValue)
    //val predTypes = rest.bindingToCollection("p",predResults).asJavaCollection
    val predTypes = new TQRContainer(predResults)

    val objResults = rest.sparql("SELECT distinct ?o WHERE { <%s> ?p ?o }" format doc.subject.stringValue)
    //val objTypes = rest.bindingToCollection("o",objResults).asJavaCollection
    val objTypes = new TQRContainer(objResults)

    var i = 0
    val ilen = doc.properties.size
    val propertyGrid = 
      if(ilen > 0) {
	val propIt = doc.properties.iterator
	val grid = new GridLayout(4,ilen)
	grid.setSpacing(true)
	while(i < ilen) {
	  val po = propIt.next
	  val pred = DocUtils.getPredicateComponent(po.pred,this)
	  val obj = DocUtils.getSubjectComponent(po.obj,this)
	  val edit = new Button("Edit"/*, action = _ => editProperty(i,grid)*/)
	  edit.setStyleName("small")
	  val delete = new Button("-", action = _ => deleteProperty(i,grid))
	  delete.setStyleName("small")
	  grid.addComponent(edit,0,i)
	  grid.addComponent(pred,1,i)
	  grid.addComponent(obj,2,i)
	  grid.addComponent(delete,3,i)
	  i=i+1
	}
	//switchToDocument(u,tabs,document)
	//switchToDocument(u,tabs,document)
	grid
      } else {
	new GridLayout()
      }
  
    val hl = new HorizontalLayout() {
      val subject = add(new TextField)
      subject.setValue(doc.subject.stringValue)
      subject.setWidth(doc.subject.stringValue.length, Sizeable.UNITS_EM)
      val load = add(new Button("Load",action = _ => setSubjectPanel(new URIImpl(subject.getValue.toString))))
    }
    val panel = new Panel(caption = "Document") {
      ResUtils.prefix.foreach(p => add(new Label(p)))
      add(hl)
      add(propertyGrid)
      add(new Button("Add property"/*,action = _ => addProperty(propertyGrid)*/))
    }

    panel
  }
*/

  def getSubjectTable(subject:URI):Panel = {
    val doc = rest.getSubjectDocument(subject,List())
    val predResults = rest.sparql("SELECT distinct ?p WHERE { <%s> ?p ?o }" format doc.subject.stringValue)
    val predTypes = rest.bindingToCollection("p",predResults).asJavaCollection
    val pColGen = new TQREditableColumnGenerator(predTypes,document)

    val objResults = rest.sparql("SELECT distinct ?o WHERE { <%s> ?p ?o }" format doc.subject.stringValue)
    val objTypes = rest.bindingToCollection("o",objResults).asJavaCollection
    val oColGen = new TQREditableColumnGenerator(objTypes,document)

    val genMap = Map("Property" -> pColGen, "Value" -> oColGen)
    val query = "SELECT ?Property ?Value WHERE { <%s> ?Property ?Value }" format doc.subject.stringValue

    val propTable = new TQRTable(genMap,rest,query)
    //propTable.addActionHandler(new DocumentActionHandler(propTable))
    propTable.addActionHandler(new DocumentActionHandler(propTable))
    propTable.setWidth(100 percent)
    val hl = new HorizontalLayout() {
      val subject = add(new TextField)
      val subjectVal = ResUtils.toAbbrevString(doc.subject)
      subject.setValue(subjectVal)
      subject.setWidth(subjectVal.length, Sizeable.UNITS_EM)
      val load = add(new Button("Load",action = _ => setSubjectPanel(new URIImpl(subject.getValue.toString))))
    }
    val panel = new Panel(caption = "Document") {
      ResUtils.prefix.foreach(p => add(new Label(p)))
      add(hl)
      add(propTable)
      val editButton = new Button("Edit")
      editButton.addListener(
		   action = _ => {
		       propTable.setEditable(!propTable.isEditable())
		       editButton.setCaption((if(propTable.isEditable()) "Save" else "Edit"))
		     })
      add(editButton)
    }

    panel
  }

  def addProperty(panel:Panel) = {
    println("adding property")
  }

  def editProperty(panel:Panel) = {
    println("editing...")
    
  }

  def deleteProperty(index:Int,layout:Layout) = {
    println("deleting property:" + index.toString)
  }

  def setSubjectPanel(subject:URI) = {
    setCompositionRoot(getSubjectTable(subject))
  }
  
/*
  def reload(event:ContextMenu.ClickEvent) = {
    println("reloading")
  }
*/
}
