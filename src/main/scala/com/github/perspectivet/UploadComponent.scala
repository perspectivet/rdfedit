package com.github.perspectivet

import com.github.perspectivet.bigdata.rest.{Rest,Document}

import com.vaadin.event.Action
import com.vaadin.event.Action._
import com.vaadin.data.util.IndexedContainer
import com.vaadin.data.Item
import com.vaadin.ui.{CustomComponent,Layout,GridLayout,ComboBox,Component,TextArea}
import com.vaadin.ui.themes.BaseTheme

import org.openrdf.model.{Value,Resource,URI}
import org.openrdf.query.BindingSet
import org.openrdf.query.TupleQueryResult
import org.openrdf.model.impl.URIImpl


import java.util.{LinkedList => JLinkedList}
import scala.collection.immutable.Map
import scala.collection.JavaConverters._

import vaadin.scala._

class UploadComponent(val rest:Rest) extends CustomComponent {
//class DocumentComponent(val doc:Document, val rest:Rest) extends CustomComponent {

  setCompositionRoot(getSubjectPanel())
  
  def getSubjectPanel():Panel = {
    new Panel(caption = "RDF Upload") {
      val rdf = add(new TextArea("RDF (N3 only for now)"))
      rdf.setSizeFull
      rdf.setInputPrompt("Enter some RDF to add to the db")
      add(new Button("Upload", action = _ => upload(rdf,log)))
      add(new Button("Clear", action = _ => clear(rdf)))
      val log = add(new TextArea("Response Log"))
      log.setSizeFull
      log.setReadOnly(true)
    }
  }

  def upload(rdf:TextArea,log:TextArea) = {
    val mutationCount = rest.putN3String(rdf.getValue.toString)
    log.setValue(log.getValue + ("\nmodified %s records" format mutationCount))
  }

  def clear(rdf:TextArea) = {
    rdf.setValue("")
  }
}
