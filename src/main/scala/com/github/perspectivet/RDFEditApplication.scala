package com.github.perspectivet

import com.github.perspectivet.bigdata.rest.Rest
import com.github.perspectivet.bigdata.rest.Document

import com.vaadin.Application
import vaadin.scala._

import org.openrdf.model.impl.URIImpl

class RDFEditApplication extends Application {
  val sparqlUrl = "http://localhost:8080/bigdata/sparql"
  val rest = new Rest(sparqlUrl)

  def init {
    val layout = new VerticalLayout(margin = true, spacing = true)
    layout.setSizeFull
    setMainWindow(new Window("rdfworkbench", content = layout))
    
    val tabs = new TabSheet(caption = "RDF Workbench",width = 100 percent, height = 100 percent) {
      addTab(new SparqlComponent(rest),"Query")
      addTab(
	new DocumentComponent(
	  new URIImpl("http://purl.uniprot.org/locations/9919"),
	  rest),
	"Document View")
      addTab(new UploadComponent(rest),"RDF Data Upload")
    }

    getMainWindow.addComponent(tabs)

  }
}
