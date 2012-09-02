package com.github.perspectivet

import com.github.perspectivet.bigdata.rest.{Rest,Document}

import com.vaadin.event.Action
import com.vaadin.event.Action._
import com.vaadin.data.util.IndexedContainer
import com.vaadin.data.Item
import com.vaadin.terminal.ExternalResource
import com.vaadin.ui.{CustomComponent,Layout,GridLayout,ComboBox,Component,TextArea}
import com.vaadin.ui.themes.BaseTheme
import com.vaadin.ui.{Table => VTable}

import org.openrdf.model.{Value,Resource,URI}
import org.openrdf.query.BindingSet
import org.openrdf.query.TupleQueryResult
import org.openrdf.model.impl.URIImpl


import java.util.{LinkedList => JLinkedList,List => JList}
import scala.collection.immutable.Map
import scala.collection.JavaConverters._

import vaadin.scala._


class SListContainer(names:List[String],types:Seq[String],values:Seq[String]) extends IndexedContainer {
  //val PREDICATE_ID = "Id"
  //val TYPE_NAME = "Type"
  //val VALUE_NAME = "Value"

  //addContainerProperty(PREDICATE_ID, String.class,null)
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
  
  /*container.sort(new Object[] { iso3166_PROPERTY_NAME },
new boolean[] { true });
}*/

}

object TQRContainer {
  
  def addGeneratedColumns(bindings:JList[String],table:VTable) {
    var bNameIt = bindings.iterator
    while(bNameIt.hasNext) {
      val bName = bNameIt.next
      println("adding generated column(%s)" format bName)
      table.addGeneratedColumn(bName, new TQRColumnGenerator())
    }
  }
}

class TQRContainer(results:TupleQueryResult) extends IndexedContainer {
  //val PREDICATE_ID = "Id"
  //val TYPE_NAME = "Type"
  //val VALUE_NAME = "Value"

  val bindings = results.getBindingNames
  var bNameIt = bindings.iterator
  while(bNameIt.hasNext) {
  //addContainerProperty(PREDICATE_ID, String.class,null)
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
  
  /*container.sort(new Object[] { iso3166_PROPERTY_NAME },
new boolean[] { true });
}*/

}

class TQRColumnGenerator extends VTable.ColumnGenerator {

  override def generateCell(source:VTable,itemId:Object,columnId:Object):Component = {
    val item = source.getItem(itemId)
    println("columnId:" + columnId.toString)
    val colVal = item.getItemProperty(columnId).getValue().asInstanceOf[Value]
    
    DocUtils.getComponent(colVal)
/*                
                String fn = (String) item.getItemProperty(
                        ExampleUtil.PERSON_PROPERTY_FIRSTNAME).getValue()
                String ln = (String) item.getItemProperty(
                        ExampleUtil.PERSON_PROPERTY_LASTNAME).getValue()
                String email = fn.toLowerCase() + "." + ln.toLowerCase()
                        + "@example.com"
			*/
                // the Link -component:
    //val link = new Link("http://www.google.com", new ExternalResource("http://www.google.com"))
    //link
  }
}


class SparqlComponent(val rest:Rest) extends CustomComponent {
//class DocumentComponent(val doc:Document, val rest:Rest) extends CustomComponent {

  setCompositionRoot(getSubjectPanel())
  
  def getSubjectPanel():Panel = {


//    table.setContainerDataSource(new SListContainer(List("a","b"),List("1","2","3"),List("a","b","c")))
//    table.setContainerDataSource(new TQRContainer(rest.sparql("select distinct ?s where { ?s ?p ?o }")))

    val table = new Table("Test Table", width = 100 percent)
    val results = rest.sparql("select ?s ?p ?o where { ?s ?p ?o. ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://bbp.epfl.ch/ontology/morphology#Morphology> }")

    table.setContainerDataSource(new TQRContainer(results))
    TQRContainer.addGeneratedColumns(results.getBindingNames,table)

    new Panel(caption = "SPARQL") {
      add(table)
      val rdf = add(new TextArea("Query Statement"))
      rdf.setSizeFull
      rdf.setValue("select distinct ?s where { ?s ?p ?o }")
      //rdf.setInputPrompt("Enter some SPARQL to run against the DB")
      add(new HorizontalLayout() {
	add(new Button("Query!", action = _ => query(rdf,table)))
	add(new Button("Clear", action = _ => clear(rdf)))
      })

      add(table)
      //val log = add(new TextArea("Results Log"))
      //log.setSizeFull
      //log.setReadOnly(true)
    }
  }
  
  def query(rdf:TextArea,table:VTable) = {
    val results = rest.sparql(rdf.getValue.toString)
    //table.setContainerDataSource(null)
    //val container = table.getContainerDataSource(null)
    table.removeAllItems()
    
    results.getBindingNames.asScala.foreach { table.removeGeneratedColumn(_) }
    table.setContainerDataSource(new TQRContainer(results))
    //TQRContainer.addGeneratedColumns(results.getBindingNames,table)

    /*
    log.setReadOnly(false)
    log.setValue(rest.resultToString(results))
    log.setReadOnly(true)
    */
  }

  def clear(rdf:TextArea) = {
    rdf.setValue("")
  }
}
