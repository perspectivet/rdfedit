package com.github.perspectivet

import com.github.perspectivet.bigdata.rest.{Rest,Document}

import com.vaadin.event.Action
import com.vaadin.event.Action._
import com.vaadin.data.util.IndexedContainer
import com.vaadin.data.Item
import com.vaadin.ui.{CustomComponent,Layout,GridLayout,ComboBox,Component}
import com.vaadin.ui.themes.BaseTheme
import com.vaadin.terminal.Sizeable

import org.openrdf.model.{Value,Resource,URI}
import org.openrdf.query.BindingSet
import org.openrdf.query.TupleQueryResult
import org.openrdf.model.impl.URIImpl


import java.util.{LinkedList => JLinkedList}
import scala.collection.immutable.Map
import scala.collection.JavaConverters._

import vaadin.scala._

//class Document
/*
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
*/

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
	//new Label(ResUtils.shorten(u))
	val button = new Button(ResUtils.shorten(u), 
		   action = _ => parent.setSubjectPanel(u))
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

  def getComponent(v:Value):Component = {
    val label = v match {
      case u:URI => {
	println("uri : " + v.stringValue)
	val button = new Button(ResUtils.shorten(u))
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
class DocumentComponent(val s:URI, val rest:Rest) extends CustomComponent {
//class DocumentComponent(val doc:Document, val rest:Rest) extends CustomComponent {

  setCompositionRoot(getSubjectPanel(s))
  
  def getSubjectPanel(subject:URI):Panel = {
  val doc = rest.getSubjectDocument(subject,List())

  val predResults = rest.sparql("SELECT distinct ?p WHERE { <%s> ?p ?o }" format doc.subject.stringValue)
  val predTypes = rest.bindingToCollection("p",predResults).asJavaCollection

  val objResults = rest.sparql("SELECT distinct ?o WHERE { <%s> ?p ?o }" format doc.subject.stringValue)
  val objTypes = rest.bindingToCollection("o",objResults).asJavaCollection

  var i = 0
  val ilen = doc.properties.size
  val properties = 
    if(ilen > 0) {
      val propIt = doc.properties.iterator
      val grid = new GridLayout(3,ilen)
      grid.setSpacing(true)
      while(i < ilen) {
	val po = propIt.next
	val pred = DocUtils.getPredicateComponent(po.pred,this)
	val obj = DocUtils.getSubjectComponent(po.obj,this)
	val delete = new Button("-", action = _ => deleteProperty(i,grid))
	delete.setStyleName("small")
	grid.addComponent(delete,0,i)
	grid.addComponent(pred,1,i)
	grid.addComponent(obj,2,i)
	i=i+1
      }
      grid
    } else {
      new Label("No properties")
    }

    val hl = new HorizontalLayout() {
      val subject = add(new TextField("") {
      })

      //subject.addListener(event:Property.ValueChangeEvent => setSubjectPanel(this.getValue.toString) )
      subject.setValue(doc.subject.stringValue)
      subject.setWidth(doc.subject.stringValue.length, Sizeable.UNITS_EM)
      val load = add(new Button("Load",action = _ => setSubjectPanel(new URIImpl(subject.getValue.toString))))
    }
    val panel = new Panel(caption = "Document") {
      add(hl)
      add(properties)
    }

    panel
  }

  def deleteProperty(index:Int,layout:Layout) = {
    
  }

  def setSubjectPanel(subject:URI) = {
    setCompositionRoot(getSubjectPanel(subject))
  }
}
