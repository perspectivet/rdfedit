package com.github.perspectivet

import com.github.perspectivet.bigdata.rest.Rest
import com.github.perspectivet.bigdata.rest.Document

import com.vaadin.Application
import vaadin.scala._

class RDFEditApplication extends Application {
  val sparqlUrl = "http://localhost:8080/bigdata/sparql"
  val rest = new Rest(sparqlUrl)

  def init {
    val layout = new VerticalLayout
    setMainWindow(new Window("rdfedit", content = layout))
    
    val subject = "<http://purl.uniprot.org/locations/9919>"
    val doc = rest.getSubjectDocument(subject,List())

    val panel = new Panel(caption = "RDF Editor") {
      //add(new Button(caption = "Button that does nothing"))
      add(new DocumentComponent(doc))
    }

    getMainWindow.addComponent(new VerticalLayout(width = 100 percent, height = 100 percent) {
      add(panel)
      //add(new Button(caption = "Button that does nothing"))
    })

    layout.setSizeFull
  }
}
