package org.phenoscape.kb.matrix

import org.phenoscape.owl.NamedRestrictionGenerator
import org.phenoscape.owl.Vocab
import org.phenoscape.owl.Vocab._
import org.phenoscape.owlet.SPARQLComposer._
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.model.IRI
import org.semanticweb.owlapi.model.OWLClassExpression
import org.semanticweb.owlapi.reasoner.OWLReasoner
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary

import com.hp.hpl.jena.query.Query

class QueryBuilder(owlReasoner: OWLReasoner) {

  implicit val reasoner = owlReasoner
  val factory = OWLManager.getOWLDataFactory
  val rdfsSubClassOf = factory.getOWLObjectProperty(OWLRDFVocabulary.RDFS_SUBCLASS_OF.getIRI)
  val implies_presence_of_some = NamedRestrictionGenerator.getClassRelationIRI(IMPLIES_PRESENCE_OF.getIRI)
  val entity_term = factory.getOWLObjectProperty(IRI.create("http://example.org/entity_term"))
  val quality_term = factory.getOWLObjectProperty(IRI.create("http://example.org/quality_term"))
  val denotes_exhibiting = factory.getOWLObjectProperty(IRI.create("http://purl.org/phenoscape/vocab.owl#denotes_exhibiting"))
  
  def absenceQuery(anatomicalExpression: OWLClassExpression, taxonomicExpression: OWLClassExpression): Query =
    select_distinct('entity, 'entity_label, 'taxon, 'taxon_label, 'state, 'state_label, 'matrix_label, 'curated_entity, 'curated_quality) from "http://kb.phenoscape.org/" where (
      bgp(
        t('eq, rdfsSubClassOf*, 'absence),
        t('eq, Vocab.entity_term, 'curated_entity),
        t('eq, Vocab.quality_term, 'curated_quality),
        t('absence, ABSENCE_OF, 'entity),
        t('entity, rdfsLabel, 'entity_label),
        t('state, denotes_exhibiting / rdfType, 'eq),
        t('state, dcDescription, 'state_label),
        t('cell, has_state, 'state),
        t('cell, belongs_to_TU, 'otu),
        t('otu, has_external_reference, 'taxon),
        t('taxon, rdfsLabel, 'taxon_label),
        t('matrix, has_character, 'matrix_char),
        t('matrix, rdfsLabel, 'matrix_label),
        t('matrix_char, may_have_state_value, 'state)),
        subClassOf('entity, anatomicalExpression),
        subClassOf('taxon, taxonomicExpression))

  def presenceQuery(anatomicalExpression: OWLClassExpression, taxonomicExpression: OWLClassExpression): Query =
    select_distinct('entity, 'entity_label, 'taxon, 'taxon_label, 'state, 'state_label, 'matrix_label, 'curated_entity, 'curated_quality) from "http://kb.phenoscape.org/" where (
      bgp(
        t('eq, rdfsSubClassOf*, 'presence),
        t('eq, Vocab.entity_term, 'curated_entity),
        t('eq, Vocab.quality_term, 'curated_quality),
        t('presence, implies_presence_of_some, 'entity),
        t('entity, rdfsLabel, 'entity_label),
        t('state, denotes_exhibiting / rdfType, 'eq),
        t('state, dcDescription, 'state_label),
        t('cell, has_state, 'state),
        t('cell, belongs_to_TU, 'otu),
        t('otu, has_external_reference, 'taxon),
        t('taxon, rdfsLabel, 'taxon_label),
        t('matrix, has_character, 'matrix_char),
        t('matrix, rdfsLabel, 'matrix_label),
        t('matrix_char, may_have_state_value, 'state)),
        subClassOf('entity, anatomicalExpression),
        subClassOf('taxon, taxonomicExpression))

}