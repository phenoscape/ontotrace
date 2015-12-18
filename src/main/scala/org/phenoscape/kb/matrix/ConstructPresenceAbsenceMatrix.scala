package org.phenoscape.kb.matrix

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader
import java.util.Properties
import scala.io.Source
import org.apache.log4j.BasicConfigurator
import org.apache.log4j.Level
import org.apache.log4j.Logger
import org.openrdf.query.BooleanQuery
import org.openrdf.query.GraphQuery
import org.openrdf.query.QueryLanguage
import org.openrdf.query.TupleQuery
import org.openrdf.query.resultio.text.tsv.SPARQLResultsTSVWriterFactory
import com.bigdata.journal.Options
import com.bigdata.rdf.sail.BigdataSail
import com.bigdata.rdf.sail.BigdataSailRepository
import com.bigdata.rdf.sail.BigdataSailUpdate
import org.semanticweb.owlapi.reasoner.OWLReasoner
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.elk.owlapi.ElkReasonerFactory
import org.semanticweb.owlapi.reasoner.InferenceType
import org.phenoscape.owlet.ManchesterSyntaxClassExpressionParser
import info.aduna.iteration.Iteration
import org.openrdf.query.TupleQueryResult
import scala.collection.JavaConversions._
import org.phenoscape.kb.matrix.SesameIterationIterator.iterationToIterator
import org.openrdf.query.BindingSet
import org.semanticweb.owlapi.model.OWLClassExpression
import com.hp.hpl.jena.query.Query
import org.phenoscape.model.DataSet
import scala.collection.mutable
import org.phenoscape.model.Taxon
import org.phenoscape.model.State
import org.phenoscape.model.Character
import org.phenoscape.model.MultipleState
import org.phenoscape.io.NeXMLUtil
import org.phenoscape.model.MultipleState.MODE
import java.net.URI
import org.obo.datamodel.impl.OBOClassImpl
import org.phenoscape.io.NeXMLWriter
import java.util.UUID
import java.util.Date
import org.phenoscape.model.AssociationSupport
import org.phenoscape.owl.Vocab
import java.text.SimpleDateFormat
import java.util.Calendar
import org.semanticweb.owlapi.model.OWLClass
import org.semanticweb.owlapi.model.IRI
import org.semanticweb.owlapi.model.OWLOntology

object ConstructPresenceAbsenceMatrix extends App {

  BasicConfigurator.configure()
  Logger.getRootLogger().setLevel(Level.WARN)

  val informative = args.take(2).contains("--informative") // --all
  val includeSupportingStates = args.take(2).contains("--include-supporting-states") // --exclude-supporting-states
  val propertiesFile = args(2)
  val journalFile = args(3)
  val tboxFile = args(4)
  val anatomicalExpressionFile = args(5)
  val taxonomicExpressionFile = args(6)
  val resultFile = args(7)

  val anatomyString = Source.fromFile(anatomicalExpressionFile, "utf-8").mkString
  val taxonString = Source.fromFile(taxonomicExpressionFile, "utf-8").mkString
  val anatomicalExpression = ManchesterSyntaxClassExpressionParser.parse(anatomyString).getOrElse {
    throw new Exception("Unparsable anatomy expression")
  }
  val taxonomicExpression = ManchesterSyntaxClassExpressionParser.parse(taxonString).getOrElse {
    throw new Exception("Unparsable taxonomic expression")
  }

  val bigdataProperties = new Properties()
  bigdataProperties.load(new FileReader(propertiesFile))
  bigdataProperties.setProperty(Options.FILE, new File(journalFile).getAbsolutePath)
  val sail = new BigdataSail(bigdataProperties)
  val repository = new BigdataSailRepository(sail)
  repository.initialize()
  val connection = repository.getReadOnlyConnection
  connection.setAutoCommit(false)
  val manager = OWLManager.createOWLOntologyManager()
  val factory = OWLManager.getOWLDataFactory
  val tbox = manager.loadOntologyFromOntologyDocument(new File(tboxFile))
  val anatomyQuery = addQueryAsClass(anatomicalExpression, tbox)
  val taxonomicQuery = addQueryAsClass(taxonomicExpression, tbox)
  val reasoner: OWLReasoner = new ElkReasonerFactory().createReasoner(tbox)
  reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY)
  val builder = new QueryBuilder(reasoner)

  def createAssociation(result: BindingSet): Association = {
    val presentOrAbsent = Set(Vocab.Present.getIRI.toString, Vocab.Absent.getIRI.toString)
    val direct = (result.getValue("entity") == result.getValue("curated_entity")) && (presentOrAbsent(result.getValue("curated_quality").stringValue))
    Association(result.getValue("entity").stringValue, result.getValue("entity_label").stringValue,
      result.getValue("taxon").stringValue, result.getValue("taxon_label").stringValue,
      result.getValue("state").stringValue, result.getValue("state_label").stringValue,
      result.getValue("matrix_label").stringValue, direct)
  }
  def runQuery(queryBuilder: (OWLClassExpression, OWLClassExpression) => Query): Set[Association] = {
    val builtQuery = queryBuilder(anatomyQuery, taxonomicQuery)
    val bigdataQuery = connection.prepareTupleQuery(QueryLanguage.SPARQL, builtQuery.toString)
    val result = bigdataQuery.evaluate
    val associations = (result map createAssociation).toSet
    result.close()
    associations
  }
  def addStateToMultiState(multi: MultipleState, state: State): MultipleState = {
    if (multi.getStates map (_.getNexmlID) contains state.getNexmlID) multi
    else new MultipleState(multi.getStates + state, multi.getMode)

  }
  val inferredAbsenceAssociations = runQuery(builder.absenceQuery)
  val inferredPresenceAssociations = runQuery(builder.presenceQuery)
  reasoner.dispose()
  connection.commit()
  connection.close()
  val characters: mutable.Map[String, Character] = mutable.Map()
  val states: mutable.Map[String, State] = mutable.Map()
  val taxa: mutable.Map[String, Taxon] = mutable.Map()
  val dataset = new DataSet()
  sealed abstract class PresenceAbsence(val symbol: String, val label: String)
  case object Absence extends PresenceAbsence("0", "absent")
  case object Presence extends PresenceAbsence("1", "present")
  def mergeIntoMatrix(association: Association, presenceAbsence: PresenceAbsence): Unit = {
    val characterID = unOBO(association.entity)
    val character = characters.getOrElseUpdate(characterID, {
      val newChar = new Character(characterID)
      newChar.setLabel(association.entityLabel)
      newChar.setDenotes(URI.create(association.entity))
      dataset.addCharacter(newChar)
      newChar
    })
    val stateID = s"${unOBO(association.entity)}_${presenceAbsence.symbol}"
    val state = states.getOrElseUpdate(stateID, {
      val newState = new State(stateID)
      newState.setSymbol(presenceAbsence.symbol)
      newState.setLabel(presenceAbsence.label)
      newState
    })
    if (!character.getStates.contains(state)) character.addState(state)
    val matrixTaxonID = unOBO(association.taxon)
    val taxon = taxa.getOrElseUpdate(matrixTaxonID, {
      val newTaxon = new Taxon(matrixTaxonID)
      newTaxon.setPublicationName(association.taxonLabel)
      val oboID = NeXMLUtil.oboID(URI.create(association.taxon))
      newTaxon.setValidName(new OBOClassImpl(oboID))
      dataset.addTaxon(newTaxon)
      newTaxon
    })
    val currentState = dataset.getStateForTaxon(taxon, character)
    val stateToAssign = currentState match {
      case polymorphic: MultipleState => addStateToMultiState(polymorphic, state)
      case `state`                    => state
      case null                       => state
      case _                          => new MultipleState(Set(currentState, state), MODE.POLYMORPHIC)
    }
    dataset.setStateForTaxon(taxon, character, stateToAssign)
    if (includeSupportingStates) {
      val supports = dataset.getAssociationSupport.getOrElseUpdate(new org.phenoscape.model.Association(taxon.getNexmlID, character.getNexmlID, state.getNexmlID), mutable.Set[AssociationSupport]())
      supports.add(new AssociationSupport(association.stateLabel, association.matrixLabel, association.direct))
    }
  }
  if (informative) {
    val absentEntities = inferredAbsenceAssociations map (_.entity)
    val presentEntities = inferredPresenceAssociations map (_.entity)
    val informativeEntities = absentEntities & presentEntities
    inferredAbsenceAssociations filter (informativeEntities contains _.entity) foreach (mergeIntoMatrix(_, Absence))
    inferredPresenceAssociations filter (informativeEntities contains _.entity) foreach (mergeIntoMatrix(_, Presence))
  } else {
    inferredAbsenceAssociations foreach (mergeIntoMatrix(_, Absence))
    inferredPresenceAssociations foreach (mergeIntoMatrix(_, Presence))
  }
  val date = new SimpleDateFormat("y-M-d").format(Calendar.getInstance.getTime)
  dataset.setPublicationNotes(s"Generated on $date by Ontotrace query:\n* taxa: $taxonString\n* entities: $anatomyString")
  val writer = new NeXMLWriter(UUID.randomUUID.toString);
  writer.setDataSet(dataset);
  writer.write(new File(resultFile));

  private def unOBO(uri: String): String = uri.replaceAllLiterally("http://purl.obolibrary.org/obo/", "")

  def addQueryAsClass(expression: OWLClassExpression, ontology: OWLOntology): OWLClass = expression match {
    case named: OWLClass => named
    case anonymous => {
      val manager = ontology.getOWLOntologyManager
      val namedQuery = factory.getOWLClass(IRI.create(s"http://example.org/${UUID.randomUUID.toString}"))
      manager.addAxiom(ontology, factory.getOWLEquivalentClassesAxiom(namedQuery, expression))
      namedQuery
    }
  }

}