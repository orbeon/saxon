name := "saxon"
version := "9.1.0.8.3"
organization := "org.orbeon"

Compile / compile / javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-g:lines")

crossPaths       := false // drop off Scala suffix from artifact names.
autoScalaLibrary := false // exclude scala-library from dependencies

ThisBuild / githubOwner       := "orbeon"
ThisBuild / githubRepository  := "saxon"
ThisBuild / githubTokenSource := TokenSource.Environment("GITHUB_TOKEN")
ThisBuild / traceLevel        := 0

Compile / unmanagedSources / excludeFilter :=
  HiddenFileFilter || { file =>
    Set("jdom", "xom", "dotnet")(file.getParentFile.getName) //, "xpath"
  }

// Orbeon Forms has its own `PathMap` implementation. We need to keep these in the source for
// compilation, but we exclude them from the resulting JAR file.
Compile / packageBin / mappings ~= (_.filterNot(_._1.getName.startsWith("PathMap")))