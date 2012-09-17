package com.github.perspectivet

import com.github.perspectivet.bigdata.rest.{Rest,Document}

import com.vaadin.terminal.ExternalResource
import com.vaadin.ui.{CustomComponent,Layout,Label}
import com.vaadin.ui.ComponentContainer
import com.vaadin.ui.Component._
import com.vaadin.ui.themes.BaseTheme

//import java.util.{LinkedList => JLinkedList,List => JList,Collection => JCollection}
import scala.collection.immutable.Map
import scala.collection._
import scala.collection.JavaConverters._

import vaadin.scala._

class HelpComponent extends CustomComponent {

  setCompositionRoot(new VerticalLayout(margin = true,spacing = true) {
    setSizeFull
    add(new Label() {
      setContentMode(Label.CONTENT_XHTML)
      setValue("""
	       <h1>RDFWorkbench Help</h1>
	       <h2>Intro</h2>
	       The RDFWorkbench is composed of the following tabs:
	       <ul>
	       <li>Document</li>
	       <li>Query</li>
	       <li>Import</li>
	       <li>Help</li>
	       </ul>

	       <p>
	       Clicking on URIs in Document or Query tabs will set the active document URI and make the Document tab active.
	       </p>
	       <p>
	       Also worth noting is the PREFIX statements. The Document and Query pages have a list of PREFIX statements at the top.  These are available by default in SPARQL queries and Document URIs.  The application also uses these prefixes to display abbreviated versions of URIs.
	       </p>
	       <h2>Document</h2>
	       The document view is for showing all properties related to a particular subject
	       <h2>Query</h2>
	       The Query view is for executing arbitrary SPARQL queries.  

	       <h2>Import</h2>
	       The Import view is for importing RDF Data in N3 format.  Paste your RDF data into the text box and click the 'Upload' button to upload the data to the DB

	       <h2>Help</h2>
	       This page
	       
	       
	       """)
    })
  })
}
