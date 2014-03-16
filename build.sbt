
//The core project has minimal library dependencies, and provides the basic Box system
lazy val core = project

//New prototype stuff for providing concurrent transactions
lazy val transact = project

//Swing bindings for boxes
lazy val swing = project.dependsOn(core)

//Graph system for boxes - see also swinggraph for Swing bindings of graphs
//TODO need to remove dependency on swing by moving colors and icons to a ui 
//project, and preferably making graph project use non-Swing class instead 
//of Color, and maybe to make a replacement for Image that could just use a 
//String name, with the canvas responsible for converting the name to an 
//image and drawing it.
lazy val graph = project.dependsOn(core, swing)

//Swing bindings for graphs
lazy val swinggraph = project.dependsOn(core, swing, graph)

//Persistence for boxes
lazy val persistence = project.dependsOn(core)

//Demos for all projects except lift
lazy val demo = project.dependsOn(core, swing, graph, swinggraph, persistence)

//Lift bindings
lazy val liftdemo = project.dependsOn(core, graph, persistence)

//Root project just aggregates all subprojects
lazy val root = project.in(file(".")).aggregate(core, transact, swing, graph, swinggraph, persistence, demo, liftdemo)

scalacOptions in ThisBuild  ++= Seq("-unchecked", "-deprecation", "-feature")

version in ThisBuild        := "0.1"

organization in ThisBuild   := "org.boxstack"

scalaVersion in ThisBuild   := "2.10.2"

