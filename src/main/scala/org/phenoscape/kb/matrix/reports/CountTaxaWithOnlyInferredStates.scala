package org.phenoscape.kb.matrix.reports

import java.io.File

import org.obo.datamodel.impl.OBOSessionImpl
import org.phenoscape.io.NeXMLReader

object CountTaxaWithOnlyInferredStates {

  def report(matrixFile: String, termID: String): Unit = {
    val dataset = new NeXMLReader(new File(matrixFile), new OBOSessionImpl()).getDataSet
    val inferredTaxa = Report.taxaWithOnlyInferredStates(dataset)
    inferredTaxa foreach { taxon => println(taxon.getPublicationName) }
    println(inferredTaxa.size)
  }

}