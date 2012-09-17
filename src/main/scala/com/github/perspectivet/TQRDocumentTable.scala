package com.github.perspectivet

import com.github.perspectivet.bigdata.rest.{Rest,Document,RDFDocUtils}

import com.vaadin.event.Action
import com.vaadin.event.Action._
import com.vaadin.data.util.IndexedContainer
import com.vaadin.data.{Item,Container,Property => VProperty}
import com.vaadin.terminal.ExternalResource
import com.vaadin.ui.{CustomComponent,Layout,GridLayout,ComboBox,Component,TextArea}
import com.vaadin.ui.{Button => VButton}
import com.vaadin.ui.{AbstractComponent => VAbstractComponent}
import com.vaadin.ui.ComponentContainer
import com.vaadin.ui.Component._
import com.vaadin.ui.themes.BaseTheme
import com.vaadin.ui.{Table => VTable}

import java.util.{LinkedList => JLinkedList,List => JList,Collection => JCollection}

import org.openrdf.model.{Value,Resource,URI}
import org.openrdf.query.BindingSet
import org.openrdf.query.TupleQueryResult
import org.openrdf.model.impl.{URIImpl,LiteralImpl}

import grizzled.slf4j.Logger

import scala.collection.immutable.Map
import scala.collection._
import scala.collection.JavaConverters._

import vaadin.scala._

//typed wrappers for values
case class POValue(val v:Value)//may be predicate or object
case class POPredicate(_v:Value) extends POValue(_v) //predicate
case class POObject(_v:Value) extends POValue(_v) //object

class TQRDocumentTable(rest:Rest, urlClickListener:VButton.ClickListener) extends TQRTable(rest,urlClickListener) with Action.Handler with VProperty.ValueChangeListener {
  val log = Logger(classOf[TQRDocumentTable])

  //TODO - come up with a better way to set the 
  //subject
  var subjectOption:Option[URI] = None

  def subject:URI = subjectOption match {
    case Some(s) => s
    case None => 
      log.error("Please call setSubject() prior to using this TQRDocumentTable")
      throw new IllegalArgumentException("Please call setSubject() prior to using this TQRDocumentTable")
  }

  def setSubject(subject:URI) = {
    subjectOption = Some(subject)

    val predResults = rest.sparql("SELECT distinct ?p WHERE { <%s> ?p ?o }" format subject.stringValue)
    val predTypes = RDFDocUtils.bindingToCollection("p",predResults).asJavaCollection
    val pColGen = new TQREditableColumnGenerator(predTypes,urlClickListener,this)

    val objResults = rest.sparql("SELECT distinct ?o WHERE { <%s> ?p ?o }" format subject.stringValue)
    val objTypes = RDFDocUtils.bindingToCollection("o",objResults).asJavaCollection
    val oColGen = new TQREditableColumnGenerator(objTypes,urlClickListener,this)

    val genMap = Map("Property" -> pColGen, "Value" -> oColGen)
    val query = "SELECT ?Property ?Value WHERE { <%s> ?Property ?Value }" format subject.stringValue

    addActionHandler(this)
    setQuery(query,genMap)
  }

  override def valueChange(event:VProperty.ValueChangeEvent) {
    val prop = event.getProperty
    println("valueChange: property(%s:%s)" format (prop.getClass.getName,prop.toString))
    
    val result = prop match {
      case ac:VAbstractComponent => {
	ac.getData match {
	  case (itemId:Any,columnId:Any,oldProperty:URI,oldValue:Value) => {
	    println("valueChange: newValue(%s)" format (ac.getValue.toString))
	    println("itemId,p,v:(%s,%s,%s)" format (itemId.toString,oldProperty.stringValue,oldValue.stringValue))
	    val item = this.getItem(itemId)

	    val newProperty = item.getItemProperty(DocUtils.PROPERTY_NAME).getValue().asInstanceOf[URI]
	    val newValue = item.getItemProperty(DocUtils.VALUE_NAME).getValue().asInstanceOf[Value]
	    println("removeOp(%s,%s,%s)" format (subject.stringValue,oldProperty.stringValue,oldValue.stringValue))
	    val removeOp = RDFDocUtils.removeOp(subject,oldProperty,oldValue)

	    val addOp = columnId match {
	      case DocUtils.PROPERTY_NAME => 
		println("addOp(%s,%s,%s)" format (subject,ResUtils.toValue(ac.getValue.toString).stringValue,newValue))
		RDFDocUtils.addOp(subject,ResUtils.toValue(ac.getValue.toString).asInstanceOf[URI],newValue)
	      case DocUtils.VALUE_NAME => 
		println("addOp(%s,%s,%s)" format (subject,newProperty,ResUtils.toValue(ac.getValue.toString).stringValue))
		RDFDocUtils.addOp(subject,newProperty,ResUtils.toValue(ac.getValue.toString))
	    }

	    rest.putRemove(removeOp)
	    rest.putAdd(addOp)
	    //rest.putUpdate(removeOp,addOp)
	  }
	  case a:Any => println("any"); a
	}
      }
    }
    println("result: " + result.toString)
  }
  
  val ACTION_NEW = new Action("New")
  val ACTION_DELETE = new Action("Delete")
  val ACTION_FOLLOW = new Action("Follow link")
  //TBD : val ACTION_COPY_URL = new Action("Copy")
  //TBD : val ACTION_LOG = new Action("Log")
  val ACTIONS_EDITABLE = Array(ACTION_NEW,ACTION_DELETE,ACTION_FOLLOW)
  val ACTIONS_NON_EDITABLE = Array(ACTION_FOLLOW/*,ACTION_COPY_URL*/)

  override def getActions(target:Any, sender:Any):Array[Action] = {
    if(isEditable)
      ACTIONS_EDITABLE
    else
      ACTIONS_NON_EDITABLE
  }

  def addPredicateObject(itemId:Int):(URI,Value) = {
    val ds = getContainerDataSource
    val ids = ds.getItem(itemId).getItemPropertyIds
    val idit = ids.iterator

    var predicate:URI = null
    var value:Value = null

    while(idit.hasNext) {
      val idnext = idit.next
      //println("next:" + idnext.toString + "," + ds.getItem(itemId).getItemProperty(idnext))
      val prop = ds.getItem(itemId).getItemProperty(idnext)
      idnext.toString match {
	case "Property" => {
	  predicate = new URIImpl("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")
	  prop.setValue(predicate)
	}
	case "Value" => {
	  value = new LiteralImpl("nothing")
	  prop.setValue(value)
	}
      }
    }

    (predicate,value)
  }

  def findPredicateObject(itemId:Any):(URI,Value) = {
    val ds = getContainerDataSource
    val ids = ds.getItem(itemId).getItemPropertyIds
    val idit = ids.iterator

    var predicate:URI = null
    var value:Value = null

    while(idit.hasNext) {
      val idnext = idit.next
      //println("next:" + idnext.toString + "," + ds.getItem(itemId).getItemProperty(idnext))
      val prop = ds.getItem(itemId).getItemProperty(idnext)
      idnext.toString match {
	case "Property" => {
	  predicate = prop.getValue.asInstanceOf[URI]
	}
	case "Value" => {
	  value = prop.getValue.asInstanceOf[Value]
	}
      }
    }

    (predicate,value)
  }

  override def handleAction(action:Action, target:Any, sender:Any) = {
    println("Handle that action...")
    println("target:" + target.toString)
    if(sender != null) 
      println("sender/row:%s:%s" format (sender.toString,sender.getClass.getName))

    if (ACTION_NEW == action) {
      println("Add...")
      val itemId = addItemAfter(sender)
      val (predicate,value) = addPredicateObject(itemId.asInstanceOf[Int])
      rest.putAdd(RDFDocUtils.addOp(subject,predicate,value))

      println("itemId:" + itemId.toString)
      refreshRowCache()
      requestRepaint()
    } else if (ACTION_DELETE == action) {
      println("Delete...")

      val (predicate,value) = findPredicateObject(sender)

      rest.putRemove(RDFDocUtils.removeOp(subject,predicate,value))
      removeItem(sender)
      refreshRowCache()
      requestRepaint()
//    } else if (ACTION_LOG == action) {
//      println("Log...")
    } 
  }
}
