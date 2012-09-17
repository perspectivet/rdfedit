package com.github.perspectivet

import com.github.perspectivet.bigdata.rest.{Rest,Document,RDFDocUtils,PredicateObject}
import com.github.perspectivet.JavaUtils._

import com.vaadin.event.Action
import com.vaadin.event.Action._
import com.vaadin.event.ShortcutAction._
import com.vaadin.data.util.IndexedContainer
import com.vaadin.data.{Item,Container,Property => VProperty}
import com.vaadin.ui.{ Button => VButton, TextField }
import com.vaadin.ui.{ CustomComponent,Layout,GridLayout,ComboBox,AbstractComponent}
import com.vaadin.ui.AbstractSelect._
import com.vaadin.ui.themes.BaseTheme
import com.vaadin.terminal.Sizeable

import org.openrdf.model.{Value,Resource,URI}
import org.openrdf.query.BindingSet
import org.openrdf.query.TupleQueryResult
import org.openrdf.model.impl.{URIImpl,LiteralImpl}

import java.util.{LinkedList => JLinkedList,Collection => JCollection}

import grizzled.slf4j.Logger

import scala.collection.JavaConverters._
import scala.collection.immutable.Map
import vaadin.scala._



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
    //println("p(%s),s(%s)" format (p,s))
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
  val PROPERTY_NAME = "Property"
  val VALUE_NAME = "Value"

  def getPredicateComponent(v:Value, parent:DocumentComponent):AbstractComponent = {
    val label = v match {
      case u:URI => {
	//println("uri : " + v.stringValue)
	val button = new Button(ResUtils.toAbbrevString(u), 
				action = _ => parent.setSubjectPanel(u))
	button.setData(u)
	button.setStyleName(BaseTheme.BUTTON_LINK)
	button
      }
      case r:Resource => {
	//println("resource : " + v.stringValue)
	new Label(ResUtils.toAbbrevString(r))
      }
      case _v:Value => {
	//println("value : " + v.stringValue)
	new Label(ResUtils.toAbbrevString(_v))
      }
      case _ => {
	println("Any : "+v.toString)
	new Label(v.toString)
      }
    }

    label
  }

  def getEditableComponent(poValue:POValue, comboValues:JCollection[Value],valueChange:VProperty.ValueChangeListener):AbstractComponent = {
    val v = poValue.v

    val label = v match {
      case u:URI => {
	//println("uri : " + v.stringValue)
	val value = ResUtils.toAbbrevString(u)
	val comboBox = new ComboBox
	jforeach[Value](comboValues,c => 
	  comboBox.addItem(ResUtils.toAbbrevString(c)))
	comboBox.setValue(value)
	comboBox.setTextInputAllowed(true)
	comboBox.setFilteringMode(Filtering.FILTERINGMODE_CONTAINS)
	comboBox.setNewItemsAllowed(true)
	comboBox
      }
      case r:Resource => {
	//println("resource : " + v.stringValue)
	val te = new TextField
	te.setValue(ResUtils.toAbbrevString(r))
	te
      }
      case _v:Value => {
	//println("value : " + v.stringValue)
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
    
    label.addListener(valueChange)
      
    label
  }

  def getComponent(v:Value):AbstractComponent = {
    val label = v match {
      case u:URI => {
	//println("uri : " + v.stringValue)
	val button = new Button(ResUtils.toAbbrevString(u))
	button.setData(u)
	button.setStyleName(BaseTheme.BUTTON_LINK)
	button
      }
      case r:Resource => {
	//println("resource : " + v.stringValue)
	new Label(ResUtils.toAbbrevString(r))
      }
      case _v:Value => {
	//println("value : " + v.stringValue)
	new Label(ResUtils.toAbbrevString(_v))
      }
      case _ => {
	println("Any : "+v.toString)
	new Label(v.toString)
      }
    }
    label
  }

  def getSubjectComponent(v:Value, parent:DocumentComponent):AbstractComponent = {
    val label = v match {
      case u:URI => {
	//println("uri : " + v.stringValue)
	val button = new Button(ResUtils.toAbbrevString(u), 
		   action = _ => parent.setSubjectPanel(u))
	button.setData(u)
	button.setStyleName(BaseTheme.BUTTON_LINK)
	button
      }
      case r:Resource => {
	//println("resource : " + v.stringValue)
	new Label(ResUtils.toAbbrevString(r))
      }
      case _v:Value => {
	//println("value : " + v.stringValue)
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

  setSubjectPanel(s)

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

  def getSubjectTable(subject:URI):Panel = {
    val doc = rest.getSubjectDocument(subject,List())

    val propTable = new TQRDocumentTable(rest,this)
    propTable.setSubject(subject)
    propTable.setWidth(100 percent)

    val editButton = new Button("Edit Mode")
    editButton.setClickShortcut(KeyCode.E,ModifierKey.ALT, ModifierKey.SHIFT)
    val toggleEditable = { 
      c:Any =>
	propTable.setEditable(! propTable.isEditable())
	editButton.setCaption((if(propTable.isEditable()) "View Mode" else "Edit Mode"))
        //println("results\n" + RDFDocUtils.resultToString(propTable.results))
    }

    editButton.addListener(toggleEditable)

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
