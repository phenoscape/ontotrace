package org.phenoscape.kb.matrix

import com.hp.hpl.jena.query.Query
import org.phenoscape.owlet.ManchesterSyntaxClassExpressionParser
import org.phenoscape.owlet.SPARQLComposer._
import org.phenoscape.owl.Vocab
import org.phenoscape.owl.Vocab._
import com.hp.hpl.jena.vocabulary.RDFS
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.model.OWLClassExpression
import org.phenoscape.owl.NamedRestrictionGenerator
import org.semanticweb.owlapi.reasoner.OWLReasoner
import org.semanticweb.owlapi.model.IRI
import org.phenoscape.owlet.QueryExpander

class QueryBuilder(owlReasoner: OWLReasoner) {

  implicit val reasoner = owlReasoner
  val factory = OWLManager.getOWLDataFactory
  val rdfsSubClassOf = factory.getOWLObjectProperty(OWLRDFVocabulary.RDFS_SUBCLASS_OF.getIRI)
  val implies_presence_of_some = NamedRestrictionGenerator.getClassRelationIRI(IMPLIES_PRESENCE_OF.getIRI)
  val entity_term = factory.getOWLObjectProperty(IRI.create("http://example.org/entity_term"))
  val quality_term = factory.getOWLObjectProperty(IRI.create("http://example.org/quality_term"))
  val Present = factory.getOWLClass(IRI.create("http://purl.obolibrary.org/obo/PATO_0000467"))
  val Count = factory.getOWLClass(IRI.create("http://purl.obolibrary.org/obo/PATO_0000070"))

  def assertedAbsenceQuery(anatomicalExpression: OWLClassExpression, taxonomicExpression: OWLClassExpression): Query = {
    select_distinct('entity, 'entity_label, 'taxon, 'taxon_label, 'state, 'state_label, 'matrix_label) from "http://kb.phenoscape.org/" where (
      bgp(
        t('eq, rdfsSubClassOf, 'absence),
        t('absence, ABSENCE_OF, 'entity),
        t('entity, rdfsLabel, 'entity_label),
        t('state, DENOTES_EXHIBITING / rdfType, 'eq),
        t('state, dcDescription, 'state_label),
        t('cell, HAS_STATE, 'state),
        t('cell, BELONGS_TO_TU, 'otu),
        t('otu, HAS_EXTERNAL_REFERENCE, 'taxon),
        t('taxon, rdfsLabel, 'taxon_label),
        t('matrix, HAS_CHARACTER, 'matrix_char),
        t('matrix, rdfsLabel, 'matrix_label),
        t('matrix_char, MAY_HAVE_STATE_VALUE, 'state)),
        subClassOf('entity, anatomicalExpression),
        subClassOf('taxon, taxonomicExpression))
  }

  def assertedPresenceQuery(anatomicalExpression: OWLClassExpression, taxonomicExpression: OWLClassExpression): Query = {
    select_distinct('entity, 'entity_label, 'taxon, 'taxon_label, 'state, 'state_label, 'matrix_label) from "http://kb.phenoscape.org/" where (
      bgp(
        t('character, entity_term, 'entity),
        // Using "count" assumes that all absences have been translated into lacks_all_parts_of_type.
        t('character, quality_term, Count),
        t('eq, rdfsSubClassOf, 'character),
        t('entity, rdfsLabel, 'entity_label),
        t('state, DENOTES_EXHIBITING / rdfType, 'eq),
        t('state, dcDescription, 'state_label),
        t('cell, HAS_STATE, 'state),
        t('cell, BELONGS_TO_TU, 'otu),
        t('otu, HAS_EXTERNAL_REFERENCE, 'taxon),
        t('taxon, rdfsLabel, 'taxon_label),
        t('matrix, HAS_CHARACTER, 'matrix_char),
        t('matrix, rdfsLabel, 'matrix_label),
        t('matrix_char, MAY_HAVE_STATE_VALUE, 'state)),
        subClassOf('entity, anatomicalExpression),
        subClassOf('taxon, taxonomicExpression))
  }

  def absenceQuery(anatomicalExpression: OWLClassExpression, taxonomicExpression: OWLClassExpression): Query = {

    select_distinct('entity, 'entity_label, 'taxon, 'taxon_label, 'state, 'state_label, 'matrix_label) from "http://kb.phenoscape.org/" where (
      bgp(
        t('eq, rdfsSubClassOf*, 'absence),
        t('absence, ABSENCE_OF, 'entity),
        t('entity, rdfsLabel, 'entity_label),
        t('state, DENOTES_EXHIBITING / rdfType, 'eq),
        t('state, dcDescription, 'state_label),
        t('cell, HAS_STATE, 'state),
        t('cell, BELONGS_TO_TU, 'otu),
        t('otu, HAS_EXTERNAL_REFERENCE, 'taxon),
        t('taxon, rdfsLabel, 'taxon_label),
        t('matrix, HAS_CHARACTER, 'matrix_char),
        t('matrix, rdfsLabel, 'matrix_label),
        t('matrix_char, MAY_HAVE_STATE_VALUE, 'state)),
        subClassOf('entity, anatomicalExpression),
        subClassOf('taxon, taxonomicExpression))

  }

  def presenceQuery(anatomicalExpression: OWLClassExpression, taxonomicExpression: OWLClassExpression): Query = {

    select_distinct('entity, 'entity_label, 'taxon, 'taxon_label, 'state, 'state_label, 'matrix_label) from "http://kb.phenoscape.org/" where (
      bgp(
        t('eq, rdfsSubClassOf*, 'presence),
        t('presence, implies_presence_of_some, 'entity),
        t('entity, rdfsLabel, 'entity_label),
        t('state, DENOTES_EXHIBITING / rdfType, 'eq),
        t('state, dcDescription, 'state_label),
        t('cell, HAS_STATE, 'state),
        t('cell, BELONGS_TO_TU, 'otu),
        t('otu, HAS_EXTERNAL_REFERENCE, 'taxon),
        t('taxon, rdfsLabel, 'taxon_label),
        t('matrix, HAS_CHARACTER, 'matrix_char),
        t('matrix, rdfsLabel, 'matrix_label),
        t('matrix_char, MAY_HAVE_STATE_VALUE, 'state)),
        subClassOf('entity, anatomicalExpression),
        subClassOf('taxon, taxonomicExpression))

  }

}