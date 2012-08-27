package com.github.perspectivet

import com.github.perspectivet.bigdata.rest.Document

import com.vaadin.data.util.IndexedContainer
import com.vaadin.ui.CustomComponent

import vaadin.scala._

class DocumentContainer(val doc:Document) extends IndexedContainer {
  val PREDICATE_ID = "Id"
  val PREDICATE_NAME = "Type"
  val OBJECT_NAME = "Value"

  //addContainerProperty(PREDICATE_ID, String.class,null)
  addContainerProperty(PREDICATE_NAME, classOf[String],null)
  addContainerProperty(OBJECT_NAME, classOf[String],null)

  var i = 0
  val pi = doc.properties.listIterator
  while(pi.hasNext) {
    val id = i.toString
    val item = addItem(id)
    val prop = pi.next
    item.getItemProperty(PREDICATE_NAME).setValue(prop.pred)
    item.getItemProperty(OBJECT_NAME).setValue(prop.obj)
    i = i+1
  }
  
  /*container.sort(new Object[] { iso3166_PROPERTY_NAME },
                new boolean[] { true });
    }*/

}
class DocumentComponent(val doc:Document) extends CustomComponent {

  val properties = new Table(caption = "Properties",height = 600 px, width = 100 percent)
  properties.setContainerDataSource(new DocumentContainer(doc))
  
  val panel = new Panel(caption = doc.subject) {
    //add(new Button(caption = "Doc Button"))
    add(new Label(doc.subject))
    add(properties)
  }

  setCompositionRoot(panel)
}
