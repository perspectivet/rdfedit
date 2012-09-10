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
import org.openrdf.model.impl.URIImpl

import java.util.{LinkedList => JLinkedList,Collection => JCollection}
import scala.collection.immutable.Map
import scala.collection.JavaConverters._
import vaadin.scala._

//class Document


class DocumentActionHandler(val table:Table) extends Action.Handler {
  val ACTION_NEW = new Action("New")
  val ACTION_DELETE = new Action("Delete")
  val ACTION_LOG = new Action("Log")
  val ACTIONS = Array(ACTION_NEW,ACTION_DELETE,ACTION_LOG)

  override def getActions(target:Object, sender:Object) = ACTIONS

  override def handleAction(action:Action, target:Object, sender:Object) = {
    if (ACTION_NEW == action) {
      val obj = table.addItem(Array[Object](new Button("foo"),"unset"),null)
      table.refreshRowCache()
      table.requestRepaint()
    } else if (ACTION_DELETE == action) {
      table.removeItem(target)
      table.refreshRowCache()
      table.requestRepaint()
    } else if (ACTION_LOG == action) {
      /*table.removeItem(target)
      table.refreshRowCache()
      table.requestRepaint()*/
    } 
  }
}


object ResUtils {
  val convertible = Map(
    "http://www.w3.org/1999/02/22-rdf-syntax-ns" -> "rdf",
    "http://www.w3.org/2000/01/rdf-schema" -> "rdfs",
    "http://bbp.epfl.ch/ontology/morphology" -> "mt"
    //"http://purl.uniprot.org/core/" -> "uniprot-core"
  )
  
  def urlreduce(url:String):String = {
    val (p,s) = urlsplit(url)
    println("p(%s),s(%s)" format (p,s))
    convertible.get(p) match {
      case Some(redp) => redp + ":" + s
      case None => url
    }
  }

  def urlsplit(s:String):(String,String) = {
    val lastHash = s.lastIndexOf("#")
    if (lastHash >= 0) {
      val (pref,suff) = s.splitAt(lastHash)
      (pref,suff.replace("#",""))
    } else {
      ("",s)
    }
  }

  def shorten(value:Value):String = { 
    value match {
      case r:Resource => urlreduce(r.stringValue)
      case v:Value => v.stringValue
    }
  }

}

object DocUtils {
  def getPredicateComponent(v:Value, parent:DocumentComponent):Component = {
    val label = v match {
      case u:URI => {
	println("uri : " + v.stringValue)
	val button = new Button(ResUtils.shorten(u), 
				action = _ => parent.setSubjectPanel(u))
	button.setData(u)
	button.setStyleName(BaseTheme.BUTTON_LINK)
	button
      }
      case r:Resource => {
	println("resource : " + v.stringValue)
	new Label(ResUtils.shorten(r))
      }
      case _v:Value => {
	println("value : " + v.stringValue)
	new Label(ResUtils.shorten(_v))
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
	val value = ResUtils.shorten(u)
	val comboBox = new ComboBox
	jforeach[Value](comboValues,c => 
	  comboBox.addItem(ResUtils.shorten(c)))
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
	te.setValue(ResUtils.shorten(r))
	te
      }
      case _v:Value => {
	println("value : " + v.stringValue)
	val te = new TextField
	te.setValue(ResUtils.shorten(_v))
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
	val button = new Button(ResUtils.shorten(u))
	button.setData(u)
	button.setStyleName(BaseTheme.BUTTON_LINK)
	button
      }
      case r:Resource => {
	println("resource : " + v.stringValue)
	new Label(ResUtils.shorten(r))
      }
      case _v:Value => {
	println("value : " + v.stringValue)
	new Label(ResUtils.shorten(_v))
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
	val button = new Button(ResUtils.shorten(u), 
		   action = _ => parent.setSubjectPanel(u))
	button.setData(u)
	button.setStyleName(BaseTheme.BUTTON_LINK)
	button
      }
      case r:Resource => {
	println("resource : " + v.stringValue)
	new Label(ResUtils.shorten(r))
      }
      case _v:Value => {
	println("value : " + v.stringValue)
	new Label(ResUtils.shorten(_v))
      }
      case _ => {
	println("Any : "+v.toString)
	new Label(v.toString)
      }
    }


    label
  }
}
/*
class TQREditableGenerator(val clickListener:VButton.ClickListener,optionList:JCollection[Value]) extends VTable.ColumnGenerator {

  override def generateCell(source:VTable,itemId:Object,columnId:Object):Component = {
    val item = source.getItem(itemId)
    println("generating %s itemId:%s,columnId:%s" format (if(source.isEditable) "editable" else "read-only", itemId,columnId))
    val colVal = item.getItemProperty(columnId).getValue().asInstanceOf[Value]
    
    val component = 
      if(colVal != null) {
	val c = DocUtils.getComponent(colVal)
	c match {
	  case b:Button => b.addListener(clickListener)
	  case _ => Unit
	}
	c
      } else {
	new Label("null")
      }

    component
  }
}
*/

class DocumentComponent(val s:URI, val rest:Rest) extends CustomComponent with VButton.ClickListener {
//class DocumentComponent(val doc:Document, val rest:Rest) extends CustomComponent {
  val document = this
  //setCompositionRoot(getSubjectPanel(s))
  setCompositionRoot(getSubjectTable(s))

  override def buttonClick(event:VButton#ClickEvent) {
    event.getSource match {
      case b:Button => 
	b.getData match {
	  case u:URI =>
	    println("clicked button with value(%s)" format u.stringValue)
	  //switchToDocument(u,tabs,doc)
	  case _ =>
	    println("clicked a non-URI button")
	}
      case _ =>
	println("clicked a non button")
    }
  }
  
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
      add(hl)
      add(propertyGrid)
      add(new Button("Add property"/*,action = _ => addProperty(propertyGrid)*/))
    }

    panel
  }

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
    propTable.addActionHandler(new DocumentActionHandler(propTable))

    val hl = new HorizontalLayout() {
      val subject = add(new TextField)
      subject.setValue(doc.subject.stringValue)
      subject.setWidth(doc.subject.stringValue.length, Sizeable.UNITS_EM)
      val load = add(new Button("Load",action = _ => setSubjectPanel(new URIImpl(subject.getValue.toString))))
    }
    val panel = new Panel(caption = "Document") {
      add(hl)
      add(propTable)
      add(new Button("Edit",
		     action = _ => {
		       propTable.setEditable(!propTable.isEditable())
		       this.setCaption((if(propTable.isEditable()) "Save" else "Edit"))
		     }))
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
