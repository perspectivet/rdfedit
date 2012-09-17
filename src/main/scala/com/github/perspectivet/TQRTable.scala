package com.github.perspectivet

import com.github.perspectivet.bigdata.rest.{Rest,Document}

import com.vaadin.event.Action
import com.vaadin.event.Action._
import com.vaadin.data.util.IndexedContainer
import com.vaadin.data.{Item,Container,Property => VProperty}
import com.vaadin.terminal.ExternalResource
import com.vaadin.ui.{CustomComponent,Layout,GridLayout,ComboBox,AbstractComponent,TextArea}
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

class TQREditableColumnGenerator(val comboValues:JCollection[Value], clickListener:VButton.ClickListener,valueChangeListener:VProperty.ValueChangeListener) extends TQRColumnGenerator(clickListener) {

  def selectComponent(v:Value, comboValues:JCollection[Value],valueChange:VProperty.ValueChangeListener):AbstractComponent = {
    DocUtils.getEditableComponent(POValue(v),comboValues,valueChangeListener)
  }

  override def generateCell(source:VTable,itemId:Any,columnId:Any):AbstractComponent = {
    val item = source.getItem(itemId)
    println("generating %s itemId:%s,columnId:%s" format (if(source.isEditable) "editable" else "read-only", itemId,columnId))
    val currColVal = item.getItemProperty(columnId).getValue().asInstanceOf[Value]
    val propertyValue = (
      itemId,
      columnId,
      item.getItemProperty(DocUtils.PROPERTY_NAME).getValue().asInstanceOf[Value],
      item.getItemProperty(DocUtils.VALUE_NAME).getValue().asInstanceOf[Value]
    )

    val component = 
      if(currColVal != null) {
	if(source.isEditable) {
	  val c = selectComponent(currColVal,comboValues,valueChangeListener)
	  //attach our current value 
	  c.setData(propertyValue)  
	  c match {
	    case b:Button => b.addListener(clickListener)
	    case _ => Unit
	  }
	  c
	} else {
	  val c = DocUtils.getComponent(currColVal)
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

class TQRPredicateEditableColumnGenerator(comboValues:JCollection[Value], clickListener:VButton.ClickListener,valueChangeListener:VProperty.ValueChangeListener) extends TQREditableColumnGenerator(comboValues,clickListener,valueChangeListener) {

  override def selectComponent(v:Value, comboValues:JCollection[Value],valueChange:VProperty.ValueChangeListener):AbstractComponent = {
    DocUtils.getEditableComponent(POPredicate(v),comboValues,valueChangeListener)
  }
}

class TQRObjectEditableColumnGenerator(comboValues:JCollection[Value], clickListener:VButton.ClickListener,valueChangeListener:VProperty.ValueChangeListener) extends TQREditableColumnGenerator(comboValues,clickListener,valueChangeListener) {

  override def selectComponent(v:Value, comboValues:JCollection[Value],valueChange:VProperty.ValueChangeListener):AbstractComponent = {
    DocUtils.getEditableComponent(POObject(v),comboValues,valueChangeListener)
  }
}

class TQRColumnGenerator(val clickListener:VButton.ClickListener) extends VTable.ColumnGenerator {

  override def generateCell(source:VTable,itemId:Any,columnId:Any):AbstractComponent = {
    val item = source.getItem(itemId)
    //println("generating %s itemId:%s,columnId:%s" format (if(source.isEditable) "editable" else "read-only", itemId,columnId))
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

class TQRTable(val rest:Rest, val urlClickListener:VButton.ClickListener) extends Table {

  def setQuery(query:String) = {
    val columnGenerator = new TQRColumnGenerator(urlClickListener)
    
    val prefixedQuery = ResUtils.prefix.mkString("","\n","\n") + query
    println("prefixedQuery:\n" + prefixedQuery)
    val results = rest.sparql(prefixedQuery)

    val container = new TQRContainer(results)
    setContainerDataSource(container)

    val colGen = columnGenerator

    var bNameIt = results.getBindingNames.iterator
    while(bNameIt.hasNext) {
      val bName = bNameIt.next
      println("adding generated column(%s,%s)" format (bName,columnGenerator))
      addGeneratedColumn(bName,columnGenerator)
    }
  }

  def setQuery(query:String,colGenMap:Map[String,TQRColumnGenerator]) = {
    
    val prefixedQuery = ResUtils.prefix.mkString("","\n","\n") + query
    println("prefixedQuery:\n" + prefixedQuery)
    val results = rest.sparql(prefixedQuery)

    val container = new TQRContainer(results)
    setContainerDataSource(container)

    var bNameIt = results.getBindingNames.iterator
    while(bNameIt.hasNext) {
      val bName = bNameIt.next
      println("adding generated column(%s)" format (bName))
      colGenMap.get(bName).map(addGeneratedColumn(bName,_))
    }
  }
}

