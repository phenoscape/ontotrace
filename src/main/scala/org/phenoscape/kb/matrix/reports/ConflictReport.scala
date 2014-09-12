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

object ConflictReport extends App {

  val matrixFile = args(0)
  val dataset = new NeXMLReader(new File(matrixFile), new OBOSessionImpl()).getDataSet
  val associations = dataset.getAssociationSupport
  val groupedByTaxonCharacter = associations.keys.groupBy { assoc => (assoc.getTaxonID, assoc.getCharacterID) }
  val conflicts = groupedByTaxonCharacter.filter { case (taxoncharacter, association) => association.size > 1 }
  println(conflicts.size)
  for {
    taxon <- dataset.getTaxa
    character <- dataset.getCharacters
  } {
    dataset.getStateForTaxon(taxon, character) match {
      case multi: MultipleState => {
        for {
          absent <- multi.getStates.find(_.getSymbol == "0")
          present <- multi.getStates.find(_.getSymbol == "1")
        } {
          val absentAssociation = new Association(taxon.getNexmlID, character.getNexmlID, absent.getNexmlID)
          val presentAssociation = new Association(taxon.getNexmlID, character.getNexmlID, present.getNexmlID)
          val absentSources = associations(absentAssociation).filter(_.isDirect).map(_.getDescriptionSource)
          val presentSources = associations(presentAssociation).filter(_.isDirect).map(_.getDescriptionSource)
          if ((absentSources & presentSources).isEmpty) {
            print(s"${taxon.getPublicationName}\t${character.getLabel}")
            (absentSources.isEmpty, presentSources.isEmpty) match {
              case (false, false) => print("\tasserted/asserted")
              case (true, false) => print("\tasserted/inferred")
              case (false, true) => print("\tasserted/inferred")
              case (true, true) => print("\tinferred/inferred")
            }
            println
          }
        }
      }
      case _ => {}
    }
  }

}