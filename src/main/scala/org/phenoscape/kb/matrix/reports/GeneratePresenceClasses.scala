package org.phenoscape.kb.matrix.reports

import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.model.IRI
import org.phenoscape.scowl.OWL._
import org.semanticweb.elk.owlapi.ElkReasonerFactory
import scala.collection.JavaConversions._
import org.semanticweb.owlapi.model.OWLEntity
import org.semanticweb.owlapi.model.OWLAnnotationProperty
import org.semanticweb.owlapi.model.OWLOntology
import org.semanticweb.owlapi.model.OWLLiteral
import java.io.File

object GeneratePresenceClasses extends App {

  val outputFile = args(0)

  def annotationsFor(obj: OWLEntity, property: OWLAnnotationProperty, ont: OWLOntology): Iterable[String] =
    obj.getAnnotations(ont, property).map(_.getValue).collect(
      { case literal: OWLLiteral => literal.getLiteral.toString })

  def labelFor(obj: OWLEntity, ont: OWLOntology): Option[String] = annotationsFor(obj, factory.getRDFSLabel, ont).headOption

  val AnatomicalEntity = Class("http://purl.obolibrary.org/obo/UBERON_0001062")
  val rdfsLabel = OWLManager.getOWLDataFactory.getRDFSLabel
  val implies_presence_of = ObjectProperty("http://purl.org/phenoscape/vocab.owl#implies_presence_of")
  val manager = OWLManager.createOWLOntologyManager
  val uberon = manager.loadOntology(IRI.create("http://purl.obolibrary.org/obo/uberon/ext.owl"))
  val elk = new ElkReasonerFactory().createReasoner(uberon)
  val allTerms = elk.getSubClasses(AnatomicalEntity, false).getFlattened
  val axioms = for {
    term <- allTerms
    termLabel <- labelFor(term, uberon)
    presenceTerm = Class(term.getIRI.toString + "/presence")
  } yield Set(
    presenceTerm EquivalentTo (implies_presence_of some term),
    presenceTerm Annotation (rdfsLabel, s"presence of $termLabel"))
  val presences = manager.createOntology(axioms.flatten)
  manager.saveOntology(presences, IRI.create(new File(outputFile)))
  
}