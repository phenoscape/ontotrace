package org.phenoscape.kb.matrix.reports

import org.phenoscape.io.NeXMLReader
import org.phenoscape.model.MultipleState
import org.phenoscape.model.Association
import org.phenoscape.model.AssociationSupport
import org.phenoscape.model.Taxon
import org.phenoscape.model.State
import org.phenoscape.model.Character
import org.obo.datamodel.impl.OBOSessionImpl
import java.io.File
import scala.collection.JavaConversions._

/**
 * This is one way to count states but not used for the paper. A SPARQL query is used instead.
 */
object CountAllSupportingStates extends App {

  val matrixFile = args(0)
  val reader = new NeXMLReader(new File(matrixFile), new OBOSessionImpl());
  val dataset = reader.getDataSet
  val associationSupports = dataset.getAssociationSupport.toMap
  val states = (associationSupports.values.flatten map { support => support.getDescriptionSource + support.getDescriptionText }).toSet
  println(states.size)

}