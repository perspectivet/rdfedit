package com.github.perspectivet

import com.github.perspectivet.bigdata.rest.{Rest,Document}

import com.vaadin.event.Action
import com.vaadin.event.Action._
import com.vaadin.data.util.IndexedContainer
import com.vaadin.data.{Item,Container,Property => VProperty}
import com.vaadin.terminal.ExternalResource
import com.vaadin.ui.{CustomComponent,Layout,GridLayout,ComboBox,Component,TextArea}
import com.vaadin.ui.{Button => VButton}
import com.vaadin.ui.{Button => VButton}
import com.vaadin.ui.ComponentContainer
import com.vaadin.ui.Component._
import com.vaadin.ui.themes.BaseTheme
import com.vaadin.ui.{Table => VTable}

import org.openrdf.model.{Value,Resource,URI}
import org.openrdf.query.BindingSet
import org.openrdf.query.TupleQueryResult
import org.openrdf.model.impl.URIImpl


import java.util.{LinkedList => JLinkedList,List => JList,Collection => JCollection}
import scala.collection.immutable.Map
import scala.collection._
import scala.collection.JavaConverters._

import vaadin.scala._

class SListContainer(names:List[String],types:Seq[String],values:Seq[String]) extends IndexedContainer {
  addContainerProperty(names(0), classOf[String],null)
  addContainerProperty(names(1), classOf[String],null)

  var i = 0
  val ti = types.iterator
  val vi = values.iterator
  while(ti.hasNext) {
    val id = i.toString
    val item = addItem(id)
    item.getItemProperty(names(0)).setValue(ti.next)
    item.getItemProperty(names(1)).setValue(vi.next)
    i = i+1
  }
}

class SparqlComponent(val rest:Rest, val clickListener:VButton.ClickListener) extends CustomComponent {

  setCompositionRoot(getSubjectPanel())
  
  var table:TQRTable = null

  def getSubjectPanel():Panel = {
    val component = this
    val query = "select distinct ?s where { ?s ?p ?o }"
    table = new TQRTable(rest,clickListener)
    table.setQuery(query)

    val panel = new Panel(caption = "SPARQL") 

    val layout = new VerticalLayout(width = 100 percent) with FilterableComponentContainer {
      ResUtils.prefix.foreach(p => add(new Label(p)))
      val rdf = add(new TextArea("Query Statement"))
      val layout = this
      rdf.setSizeFull
      rdf.setValue("select distinct ?s where { ?s ?p ?o }")
      //rdf.setInputPrompt("Enter some SPARQL to run against the DB")
      add(new HorizontalLayout() {
	add(new Button("Query", action = {c => println(c); doQuery(rdf,layout)}))
	add(new Button("Clear", action = _ => doClear(rdf)))
      })
      add(table)
    }

    panel.setContent(layout)
    panel
  }
    

  def doQuery(rdf:TextArea,parent:ComponentContainer) = {
    val t = new TQRTable(rest,clickListener)
    t.setQuery(rdf.getValue.toString)
    println("wtf rdf:" + rdf.getValue.toString)
    val cIt = parent.getComponentIterator
    var done = false
    while(cIt.hasNext && ! done) {
      done = cIt.next match {
	case t:TQRTable => println("removing component"); parent.removeComponent(t); true
	case _ => false
      }
    }

    this.table = t
    parent.addComponent(this.table)
    println("wtf rdf:" + rdf.getValue.toString)
    
    /*
    log.setReadOnly(false)
    log.setValue(rest.resultToString(results))
    log.setReadOnly(true)
    */
  }

  def doClear(rdf:TextArea) = {
    rdf.setValue("")
  }
}
