import com.typesafe.sbt.SbtNativePackager._
import NativePackagerKeys._

organization  := "org.phenoscape"

name          := "ontotrace"

version       := "1.0.4-SNAPSHOT"

packageArchetype.java_application

mainClass in Compile := Some("org.phenoscape.kb.matrix.ConstructPresenceAbsenceMatrix")

scalaVersion  := "2.10.4"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

resolvers += "Phenoscape Maven repository" at "http://phenoscape.svn.sourceforge.net/svnroot/phenoscape/trunk/maven/repository"

resolvers += "Bigdata releases" at "http://www.systap.com/maven/releases"

resolvers += "NXParser repository" at "http://nxparser.googlecode.com/svn/repository"

resolvers += "BBOP repository" at "http://code.berkeleybop.org/maven/repository"

javaOptions += "-Xmx12G"

libraryDependencies ++= {
  Seq(
      "junit"                  %   "junit"                         % "4.10" % "test",
      "org.apache.commons"     %   "commons-lang3"                 % "3.1",
      "commons-io"             %   "commons-io"                    % "2.4",
      "net.sourceforge.owlapi" %   "owlapi-distribution"           % "3.5.0",
      "org.semanticweb.elk"    %   "elk-owlapi"                    % "0.4.1",
      "com.bigdata"            %   "bigdata"                       % "1.3.2",
      "org.openrdf.sesame"     %   "sesame-queryresultio-text"     % "2.6.10",
      "org.phenoscape"         %   "scowl"                         % "0.8",
      "org.phenoscape"         %   "kb-owl-tools"                  % "1.0.3",
      "org.phenoscape"         %   "owlet"                         % "1.1.6",
      "org.phenoscape"         %   "phenex"                        % "1.15.4",
      "org.bbop"               %   "oboformat"                     % "0.5.5"
  )
}
