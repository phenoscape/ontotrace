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
  val patternsWithCharacters = charactersWithPatterns.groupBy { case (character, pattern) => pattern }.map { case (pattern, map) => (pattern, map.keys) }
  val clusters = patternsWithCharacters.values.filter(_.size > 1).toSeq.sortWith((x, y) => x.size > y.size)
  for (characters <- clusters) {
    println(characters.map(id => findCharacterLabel(id).getOrElse(id)).mkString(", "))
  }

}