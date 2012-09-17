import net.thunderklaus.GwtPlugin._

name := "rdfedit"
 
scalaVersion := "2.9.1"
 
seq(webSettings: _*)

seq(gwtSettings: _*)

gwtVersion := "2.4.0"

resolvers ++= Seq(
	  "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository",
	  "repo.codahale.com" at "http://repo.codahale.com",
	  "bigdata.releases" at "http://www.systap.com/maven/releases",
	  "bigdata.snapshots" at "http://www.systap.com/maven/snapshots",
	  "nxparser-repo" at "http://nxparser.googlecode.com/svn/repository",
	  "openrdf.releases" at "http://repo.aduna-software.org/maven2/releases",
	  "Vaadin add-ons repository" at "http://maven.vaadin.com/vaadin-addons"
)

// basic dependencies
libraryDependencies ++= Seq(
  "com.vaadin" % "vaadin" % "6.7.6",
  "org.vaadin.addons" % "scaladin" % "1.0.0",
  "org.eclipse.jetty" % "jetty-webapp" % "8.0.4.v20111024" % "container",
  "com.github.perspectivet" %% "bigdata-rest-scala" % "0.0.1-SNAPSHOT",
  "org.clapper" %% "grizzled-slf4j" % "0.6.9"
)

//other add-ons
libraryDependencies ++= Seq(
//	"org.vaadin.addons" % "contextmenu" % "3.1.0"
)



// hack: sbt-gwt-plugin assumes that sources are in src/main/java
javaSource in Compile <<= (scalaSource in Compile)

gwtModules := List("com.github.perspectivet.RDFEditWidgetset")

javaOptions in Gwt += "-mx1024M"

// more correct place would be to compile widgetset under the target dir and configure jetty to find it from there 
gwtTemporaryPath := file(".") / "src" / "main" / "webapp" / "VAADIN" / "widgetsets"

port in container.Configuration := 8282