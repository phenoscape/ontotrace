package org.phenoscape.kb.matrix.reports

import java.io.File

import scala.collection.JavaConversions._

import org.obo.datamodel.impl.OBOSessionImpl
import org.phenoscape.io.NeXMLReader

object ClusterCharacters extends App {

  val filePath = args(0)
  val dataset = new NeXMLReader(new File(filePath), new OBOSessionImpl()).getDataSet
  def findCharacterLabel(id: String) = dataset.getCharacters.find(_.getNexmlID == id).map(_.getLabel)
  val associations = dataset.getAssociationSupport
  val charactersWithPatterns = associations.keys
    .groupBy(_.getCharacterID)
    .map {
      case (characterID, associations) =>
        (characterID, associations.map(assoc => (assoc.getStateID.last, assoc.getTaxonID)).toSet)
    }
  def characterWithPatternToCharacterWithTaxonCount(in: Map[String, Set[(Char, String)]]) = in.map {
    case (characterID, pattern) =>
      (characterID, pattern.groupBy {
        case (state, taxon) =>
          taxon
      }.keys.toSet.size)
  }
  val patternsWithCharacters = charactersWithPatterns.groupBy { case (character, pattern) => pattern }.map { case (pattern, characterToPattern) => (pattern, characterWithPatternToCharacterWithTaxonCount(characterToPattern)) }
  val clusters = patternsWithCharacters.values.filter(_.size > 1).toSeq.sortWith((x, y) => x.size > y.size)
  val inferredOnlyCharacters = Report.charactersWithoutAssertedCells(dataset).keys.toSet
  val characterGroups = for (characterGroup <- clusters) yield {
    characterGroup.map {
      case (characterID, taxonCount) =>
        val characterLabel = findCharacterLabel(characterID).getOrElse(characterID)
        val inferredLabel = if (inferredOnlyCharacters(characterID)) "[inferred]" else ""
        s"$characterLabel $inferredLabel"
    }.mkString(", ")
  }

  val totalFilledCellsUnderCharactersInClustersOfSize2OrMore = (for {
    cluster <- clusters
  } yield cluster.values.sum).sum

  print(characterGroups.mkString("\n"))

}