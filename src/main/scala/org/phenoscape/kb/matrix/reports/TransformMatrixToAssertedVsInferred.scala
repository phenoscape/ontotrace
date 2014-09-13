package org.phenoscape.kb.matrix.reports

import org.phenoscape.io.NeXMLReader
import org.obo.datamodel.impl.OBOSessionImpl
import java.io.File
import scala.collection.JavaConversions._
import org.phenoscape.model.Association
import org.phenoscape.model.State
import org.phenoscape.io.NeXMLWriter
import java.util.UUID
import org.phenoscape.model.MultipleState

object TransformMatrixToAssertedVsInferred extends App {

  val filePath = args(0)
  val newFilePath = args(1)
  val dataset = new NeXMLReader(new File(filePath), new OBOSessionImpl()).getDataSet
  val associations = dataset.getAssociationSupport
  val Asserted = new State()
  Asserted.setSymbol("0")
  val Inferred = new State()
  Inferred.setSymbol("1")
  for {
    taxon <- dataset.getTaxa
    character <- dataset.getCharacters
    state <- Option(dataset.getStateForTaxon(taxon, character))
  } {
    val states = state match {
      case multi: MultipleState => multi.getStates.toSet
      case _ => Set(state)
    }
    val stateAssociations = states.map(s => (new Association(taxon.getNexmlID, character.getNexmlID, s.getNexmlID)))
    val newState = if (stateAssociations.flatMap(associations(_)).exists(_.isDirect)) Asserted else Inferred
    dataset.setStateForTaxon(taxon, character, newState)
  }
  val writer = new NeXMLWriter(UUID.randomUUID.toString)
  println("writing")
  writer.setDataSet(dataset)
  writer.write(new File(newFilePath))
  
  // the matrix will still require some hand editing at this point to link to the proper states block

}