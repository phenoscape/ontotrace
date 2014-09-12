package org.phenoscape.kb.matrix.reports

import java.io.File

import scala.collection.JavaConversions._

import org.obo.datamodel.impl.OBOSessionImpl
import org.phenoscape.io.NeXMLReader

object CharacterReport extends App {

  val filePath = args(0)
  val termID = args(1)
  val dataset = new NeXMLReader(new File(filePath), new OBOSessionImpl()).getDataSet
  def findTaxonLabel(id: String) = dataset.getTaxa.find(_.getNexmlID == id).map(_.getPublicationName)
  val associations = dataset.getAssociationSupport
  val taxonToSupports = associations.filter { case (assoc, supports) => assoc.getCharacterID == termID }
    .map { case (key, value) => (key.getTaxonID, value.partition(_.isDirect)) }
  println("taxon\tdirects\tindirects")
  for ((taxon, (directs, indirects)) <- taxonToSupports) {
    val taxonName = findTaxonLabel(taxon).getOrElse(taxon)
    println(s"$taxonName\t${directs.size}\t${indirects.size}")
  }

}

