package org.phenoscape.kb.matrix.reports

import scala.collection.JavaConversions._

import org.phenoscape.model.Association
import org.phenoscape.model.AssociationSupport
import org.phenoscape.model.Character
import org.phenoscape.model.DataSet
import org.phenoscape.model.MultipleState
import org.phenoscape.model.State
import org.phenoscape.model.Taxon

object Report {

  def countPopulatedCells(dataset: DataSet): Int = dataset.getAssociationSupport.keys.size

  def countAssertedCells(dataset: DataSet): Int = assertedAssociations(dataset).keys.size

  def assertedAssociations(dataset: DataSet): Map[Association, java.util.Set[AssociationSupport]] = dataset.getAssociationSupport.filter { case (association, supports) => supports.exists(_.isDirect) }.toMap

  def groupedByCharacter(dataset: DataSet): Map[String, Map[Association, java.util.Set[AssociationSupport]]] = dataset.getAssociationSupport.toMap.groupBy { case (association, supports) => association.getCharacterID }

  def assertedGroupedByCharacter(dataset: DataSet): Map[String, Map[Association, java.util.Set[AssociationSupport]]] = assertedAssociations(dataset).groupBy { case (association, supports) => association.getCharacterID }

  def charactersWithBothPresenceAndAbsenceAsserted(dataset: DataSet): Map[String, Map[Association, java.util.Set[AssociationSupport]]] = assertedGroupedByCharacter(dataset).filter {
    case (characterID, associations) =>
      associations.keys.exists(_.getStateID.endsWith("1")) && associations.keys.exists(_.getStateID.endsWith("0"))
  }

  def cellsForVariablyAssertedCharacters(dataset: DataSet): Set[Association] = charactersWithBothPresenceAndAbsenceAsserted(dataset).flatMap {
    case (characterID, associations) => associations.keys
  }.toSet

  def taxaForVariablyAssertedCharacters(dataset: DataSet): Set[String] = cellsForVariablyAssertedCharacters(dataset).map(_.getTaxonID).toSet

  def charactersWithoutAssertedCells(dataset: DataSet): Map[String, Map[Association, java.util.Set[AssociationSupport]]] = groupedByCharacter(dataset).filterNot {
    case (characterID, associations) => (associations.values.flatten.exists(_.isDirect))
  }

  def findTaxonLabel(id: String, dataset: DataSet): Option[String] = dataset.getTaxa.find(_.getNexmlID == id).map(_.getPublicationName)

  def directAndIndirectSupportsForCharacterValuesByTaxon(dataset: DataSet, characterTermID: String): Map[String, (Set[AssociationSupport], Set[AssociationSupport])] = {
    val associations = dataset.getAssociationSupport.toMap
    associations.filter { case (assoc, supports) => assoc.getCharacterID == characterTermID }
      .map { case (key, value) => (key.getTaxonID, value.toSet.partition(_.isDirect)) }
  }

  def taxaWithOnlyInferredStates(dataset: DataSet): Iterable[Taxon] = {
    val assertedTaxa = taxaWithAnyCharacterStateWithDirectSupport(dataset)
    dataset.getTaxa.filterNot(taxon => assertedTaxa(taxon.getNexmlID))
  }

  def listStates(dataset: DataSet, taxon: Taxon, character: Character): Set[State] = dataset.getStateForTaxon(taxon, character) match {
    case multi: MultipleState => multi.getStates.toSet
    case null => Set.empty[State]
    case single => Set(single)
  }

  def assertedAssociationSupports(dataset: DataSet, taxon: Taxon, character: Character, state: State): Set[AssociationSupport] = {
    val association = new Association(taxon.getNexmlID, character.getNexmlID, state.getNexmlID)
    val associationSupports = dataset.getAssociationSupport.toMap
    val supports = associationSupports.get(association).toSet.flatten
    supports.filter(_.isDirect)
  }

  def hasAnyCharacterStateWithDirectSupport(taxon: Taxon, dataset: DataSet): Boolean = {
    val associationSupport = dataset.getAssociationSupport.toMap
    associationSupport.exists {
      case (association, supports) =>
        (association.getTaxonID == taxon.getNexmlID) && (supports.exists(_.isDirect))
    }
  }

  def taxaWithAnyCharacterStateWithDirectSupport(dataset: DataSet): Set[String] = {
    val associationSupport = dataset.getAssociationSupport.toMap
    associationSupport.filter {
      case (association, supports) => supports.exists(_.isDirect)
    }
      .map {
        case (association, supports) =>
          association.getTaxonID
      }
      .toSet
  }

}