package com.github.perspectivet

import com.github.perspectivet.bigdata.rest.{Rest,Document}

import com.vaadin.event.Action
import com.vaadin.event.Action._
import com.vaadin.data.util.IndexedContainer
import com.vaadin.data.{Item,Container}
import com.vaadin.terminal.ExternalResource
import com.vaadin.ui.{CustomComponent,Layout,GridLayout,ComboBox,Component,TextArea}
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

object TQRContainer {
  
  def addGeneratedColumns(bindings:JList[String],table:VTable,clickListener:VButton.ClickListener) {
    var bNameIt = bindings.iterator
    while(bNameIt.hasNext) {
      val bName = bNameIt.next
      println("adding generated column(%s)" format bName)
      table.addGeneratedColumn(bName, new TQRColumnGenerator(clickListener))
    }
  }
}

class TQRContainer(val results:TupleQueryResult) extends IndexedContainer {
  var bindings:JList[String] = null

  init(results)

  def init(results:TupleQueryResult) {
    bindings = results.getBindingNames
    var bNameIt = bindings.iterator
    while(bNameIt.hasNext) {
      val bName = bNameIt.next
      addContainerProperty(bName, classOf[Value],null)
      println("bName:(%s)" format bName)
    }

    var i = 0
    while(results.hasNext) {
      val id = i.toString
      val item = addItem(id)
      val colNames = bindings.iterator
      val rowBindingSet = results.next
      while(colNames.hasNext) {
	val colName = colNames.next
	item.getItemProperty(colName).setValue(rowBindingSet.getBinding(colName).getValue/*.stringValue*/)
      }
      i = i+1
    }
  }

}

class TQREditableColumnGenerator(val comboValues:JCollection[Value], clickListener:VButton.ClickListener) extends TQRColumnGenerator(clickListener) {

  override def generateCell(source:VTable,itemId:Object,columnId:Object):Component = {
    val item = source.getItem(itemId)
    println("generating %s itemId:%s,columnId:%s" format (if(source.isEditable) "editable" else "read-only", itemId,columnId))
    val colVal = item.getItemProperty(columnId).getValue().asInstanceOf[Value]
    
    val component = 
      if(colVal != null) {
	if(source.isEditable) {
	  val c = DocUtils.getEditableComponent(colVal,comboValues)
	  c match {
	    case b:Button => b.addListener(clickListener)
	    case _ => Unit
	  }
	  c
	} else {
	  val c = DocUtils.getComponent(colVal)
	  c match {
	    case b:Button => b.addListener(clickListener)
	    case _ => Unit
	  }
	  c
	}
      } else {
	new Label("null")
      }
    component.setWidth(100 percent)
    component
  }
}

class TQRColumnGenerator(val clickListener:VButton.ClickListener) extends VTable.ColumnGenerator {

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

class TQRTable(val columnGenerator:Map[String,TQRColumnGenerator], rest:Rest, query:String) extends Table {

  val prefixedQuery = ResUtils.prefix.mkString("","\n","\n") + query
  val results = rest.sparql(prefixedQuery)

  val container = new TQRContainer(results)
  setContainerDataSource(container)

  var bNameIt = results.getBindingNames.iterator
  while(bNameIt.hasNext) {
    val bName = bNameIt.next
    println("adding generated column(%s,%s)" format (bName,columnGenerator))
    columnGenerator.get(bName).map(addGeneratedColumn(bName,_))
  }
  println("prefixedQuery:\n" + prefixedQuery)
}

class SparqlComponent(val rest:Rest, val clickListener:VButton.ClickListener) extends CustomComponent {

  setCompositionRoot(getSubjectPanel())
  
  var table:Table = null

  def getSubjectPanel():Panel = {
    val component = this
    val colGen = new TQRColumnGenerator(clickListener)
    val genMap = Map("s" -> colGen, "p" -> colGen, "o" -> colGen)

    val query = "select distinct ?s where { ?s ?p ?o }"
    table = new TQRTable(genMap,rest,query)

    val panel = new Panel(caption = "SPARQL") 

    val layout = new VerticalLayout(width = 100 percent) with FilterableComponentContainer {
      ResUtils.prefix.foreach(p => add(new Label(p)))
      val rdf = add(new TextArea("Query Statement"))
      val layout = this
      rdf.setSizeFull
      rdf.setValue("select distinct ?s where { ?s ?p ?o }")
      //rdf.setInputPrompt("Enter some SPARQL to run against the DB")
      add(new HorizontalLayout() {
	add(new Button("Query!", action = {c => println(c); doQuery(rdf,layout)}))
	add(new Button("Clear", action = _ => doClear(rdf)))
      })
      add(table)
    }

    panel.setContent(layout)
    panel
  }
    

  def doQuery(rdf:TextArea,parent:ComponentContainer) = {
    val colGen = new TQRColumnGenerator(clickListener)
    val genMap = Map("s" -> colGen, "p" -> colGen, "o" -> colGen)
    
    val t = new TQRTable(genMap,rest,rdf.getValue.toString)
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
