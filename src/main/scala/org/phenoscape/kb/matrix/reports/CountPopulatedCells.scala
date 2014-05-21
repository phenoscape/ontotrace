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

object CountPopulatedCells extends App {

  val matrixFile = args(0)
  val dataset = new NeXMLReader(new File(matrixFile), new OBOSessionImpl()).getDataSet
  val associations = dataset.getAssociationSupport
  println("Number of populated cells: " + associations.keys.size)
  val assertedAssociations = associations filter { case (association, supports) => supports exists (_.isDirect) }
  println("Number of asserted cells: " + assertedAssociations.keys.size)
  val groupedByCharacter = associations groupBy { case (association, supports) => association.getCharacterID }
  val charactersWithoutAssertedCells = groupedByCharacter filterNot {
    case (characterID, associations) => (associations.values.flatten exists (_.isDirect))
  }
  println("Number of characters with no asserted cells: " + charactersWithoutAssertedCells.keys.size)
  val idToCharacterLabel = (dataset.getCharacters map { c => (c.getNexmlID -> c.getLabel) }).toMap
  for (id <- charactersWithoutAssertedCells.keys) {
    println(idToCharacterLabel(id))
  }

  val presenceAssociations = associations filter { case (association, supports) => association.getStateID.endsWith("1") }
  println("Number of presence associations: " + presenceAssociations.keys.size)
  val absenceAssociations = associations filter { case (association, supports) => association.getStateID.endsWith("0") }
  println("Number of absence associations: " + absenceAssociations.keys.size)
  val presencesGroupedByCharacter = presenceAssociations groupBy { case (association, supports) => association.getCharacterID }
  val absencesGroupedByCharacter = absenceAssociations groupBy { case (association, supports) => association.getCharacterID }
  val charactersWithAssertedPresences = presencesGroupedByCharacter filter {
    case (characterID, associations) => (associations.values.flatten exists (_.isDirect))
  }
  val charactersWithoutAssertedPresences = presencesGroupedByCharacter filterNot {
    case (characterID, associations) => (associations.values.flatten exists (_.isDirect))
  }
  val charactersWithAssertedAbsences = absencesGroupedByCharacter filter {
    case (characterID, associations) => (associations.values.flatten exists (_.isDirect))
  }
  val charactersWithoutAssertedAbsences = absencesGroupedByCharacter filterNot {
    case (characterID, associations) => (associations.values.flatten exists (_.isDirect))
  }
  val charactersMadeInformativeByInferredAbsences = charactersWithAssertedPresences.keys.toSet & charactersWithoutAssertedAbsences.keys.toSet
  val charactersMadeInformativeByInferredPresences = charactersWithAssertedAbsences.keys.toSet & charactersWithoutAssertedPresences.keys.toSet
  println("Number of characters made informative by inferred absences: " + charactersMadeInformativeByInferredAbsences.size)
  println("Number of characters made informative by inferred presences: " + charactersMadeInformativeByInferredPresences.size)
  println((charactersMadeInformativeByInferredAbsences & charactersMadeInformativeByInferredPresences).size)

}