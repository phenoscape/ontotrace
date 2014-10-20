package org.phenoscape.kb.matrix.reports

import java.io.File

import scala.collection.JavaConversions._

import org.obo.datamodel.impl.OBOSessionImpl
import org.phenoscape.io.NeXMLReader
import org.phenoscape.model.AssociationSupport
import org.phenoscape.model.DataSet

object CharacterReport {

  def report(filePath: String, termID: String): String = {
    val dataset = new NeXMLReader(new File(filePath), new OBOSessionImpl()).getDataSet
    val taxonToSupports = Report.directAndIndirectSupportsForCharacterValuesByTaxon(dataset, termID)
    println("taxon\tdirects\tindirects")
    val lines = for ((taxon, (directs, indirects)) <- taxonToSupports) yield {
      val taxonName = Report.findTaxonLabel(taxon, dataset).getOrElse(taxon)
      (s"$taxonName\t${directs.size}\t${indirects.size}")
    }
    lines.mkString("\n")
  }

}

