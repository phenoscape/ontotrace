package org.phenoscape.kb.matrix.reports

import org.phenoscape.io.NeXMLReader
import org.obo.datamodel.impl.OBOSessionImpl
import java.io.File
import scala.collection.JavaConversions._
import org.phenoscape.model.MultipleState
import org.phenoscape.model.State
import org.phenoscape.model.Character
import org.phenoscape.model.Taxon
import org.phenoscape.model.Association
import org.phenoscape.model.AssociationSupport

object CountTaxaWithOnlyInferredStates extends App {

  val matrixFile = args(0)
  val dataset = new NeXMLReader(new File(matrixFile), new OBOSessionImpl()).getDataSet
  val associationSupports = dataset.getAssociationSupport.toMap
  def findStates(taxon: Taxon, character: Character): Set[State] = dataset.getStateForTaxon(taxon, character) match {
    case multi: MultipleState => multi.getStates.toSet
    case null => Set[State]()
    case single => Set(single)
  }
  def assertedAssociations(taxon: Taxon, character: Character, state: State): Set[AssociationSupport] = {
    val association = new Association(taxon.getNexmlID, character.getNexmlID, state.getNexmlID)
    val supportss = associationSupports.get(association).toSet
    for {
      supports <- supportss
      support <- supports
      if support.isDirect
    } yield support
  }
  def hasDirectSupport(taxon: Taxon): Boolean = {
    val asserteds = for {
      character <- dataset.getCharacters
      state <- findStates(taxon, character)
      association <- this.assertedAssociations(taxon, character, state)
    } yield association
    !asserteds.isEmpty
  }
  println(dataset.getCharacters.size)
  val inferredTaxa = dataset.getTaxa.toList.par filterNot hasDirectSupport
  inferredTaxa.seq foreach { taxon => println(taxon.getPublicationName) }
  println(inferredTaxa.size)
  println(dataset.getTaxa.size)

}