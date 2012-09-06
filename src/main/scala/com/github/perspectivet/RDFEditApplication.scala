package com.github.perspectivet

import com.github.perspectivet.bigdata.rest.Rest
import com.github.perspectivet.bigdata.rest.Document

import com.vaadin.Application
import vaadin.scala._
import com.vaadin.ui.{ Button => VButton }

import org.openrdf.model.impl.URIImpl
import org.openrdf.model.URI

class RDFEditApplication extends Application with VButton.ClickListener{
  val application = this
  val sparqlUrl = "http://localhost:8080/bigdata/sparql"
  val rest = new Rest(sparqlUrl)

    val layout = new VerticalLayout(margin = true, spacing = true)
    layout.setSizeFull
    setMainWindow(new Window("rdfworkbench", content = layout))

    val doc = 
      new DocumentComponent(
	new URIImpl("http://purl.uniprot.org/locations/9919"),
	rest)


    val tabs = new TabSheet(caption = "RDF Workbench",width = 100 percent, height = 100 percent) {
      addTab(new SparqlComponent(rest, application),"Query")
      addTab(doc,"Document View")
      addTab(new UploadComponent(rest),"RDF Data Upload")
    }

  override def buttonClick(event:VButton#ClickEvent) {
    event.getSource match {
      case b:Button => 
	b.getData match {
	  case u:URI =>
	    println("clicked button with value(%s)" format u.stringValue)
	    switchToDocument(u,tabs,doc)
	  case _ =>
	    println("clicked a non-URI button")
	}
      case _ =>
	println("clicked a non button")
    }
  }

  def init {

    getMainWindow.addComponent(tabs)

  }
  
  def switchToDocument(newSubject:URI,tabs:TabSheet,doc:DocumentComponent) = {
    tabs.setSelectedTab(doc)
    doc.setSubjectPanel(newSubject)
  }
}
