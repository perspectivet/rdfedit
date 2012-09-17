rdfedit
=======

rdfedit

A simple web based RDF Workbench for the bigdata rdf db.  The system is written in scala and Vaadin.

Requirements:
[Bigdata NanoSparqlServer](http://sourceforge.net/apps/mediawiki/bigdata/index.php?title=NanoSparqlServer)

Notable Dependencies:
+ [scaladin](https://github.com/henrikerola/scaladin)
+ [Grizzled slf4j](http://software.clapper.org/grizzled-slf4j/)
+ [Sesame](http://www.openrdf.org/index.jsp)

If you run in to PermGen space issues run sbt with the following command line:

>$ JAVA_OPTS="-XX:MaxPermSize=256M -Xmx512M" sbt

Or add those to the default options in the sbt launcher script.

Notes:

If you run the tests the RestSpec.scala:"insert an rdf file" test will insert the uniprot locations.rdf file (include in src/test/resources) into your bigdata instance.

You may have to edit RestSpec to match your test bigdata instance.

TODO
----
Sooner:
* More testing (lots of things should break)
* Lots of code clean-up needed

Later:
* Support proper sail interfaces instead of just Bigdata's Sail-like Rest client 
* Maybe a proper Rest service for the Document (most of the neccessary classes should already be there)