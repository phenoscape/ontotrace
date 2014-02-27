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

object ConstructPresenceAbsenceMatrix extends App {

  BasicConfigurator.configure()
  Logger.getRootLogger().setLevel(Level.WARN)

  val propertiesFile = args(0)
  val journalFile = args(1)
  val tboxFile = args(2)
  val anatomicalExpressionFile = args(3)
  val taxonomicExpressionFile = args(4)
  val resultFile = args(5)

  val bigdataProperties = new Properties()
  bigdataProperties.load(new FileReader(propertiesFile))
  bigdataProperties.setProperty(Options.FILE, new File(journalFile).getAbsolutePath)
  val sail = new BigdataSail(bigdataProperties)
  val repository = new BigdataSailRepository(sail)
  repository.initialize()
  val connection = repository.getConnection
  connection.setAutoCommit(false)
  val manager = OWLManager.createOWLOntologyManager()
  val tbox = manager.loadOntologyFromOntologyDocument(new File(tboxFile))
  val reasoner: OWLReasoner = new ElkReasonerFactory().createReasoner(tbox)
  reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY)
  val builder = new QueryBuilder(reasoner)
  val anatomicalExpression = ManchesterSyntaxClassExpressionParser.parse(Source.fromFile(anatomicalExpressionFile, "utf-8").mkString).getOrElse {
    throw new Exception("Unparsable anatomy expression")
  }
  val taxonomicExpression = ManchesterSyntaxClassExpressionParser.parse(Source.fromFile(taxonomicExpressionFile, "utf-8").mkString).getOrElse {
    throw new Exception("Unparsable taxonomic expression")
  }
  def createAssocation(result: BindingSet): Association = {
    Association(result.getValue("entity").stringValue, result.getValue("entity_label").stringValue,
      result.getValue("taxon").stringValue, result.getValue("taxon_label").stringValue,
      result.getValue("state").stringValue, result.getValue("state_label").stringValue,
      result.getValue("matrix_label").stringValue)
  }
  def runQuery(queryBuilder: (OWLClassExpression, OWLClassExpression) => Query): Set[Association] = {
    val builtQuery = queryBuilder(anatomicalExpression, taxonomicExpression)
    val bigdataQuery = connection.prepareTupleQuery(QueryLanguage.SPARQL, builtQuery.toString)
    val result = bigdataQuery.evaluate
    val associations = (result map createAssocation).toSet
    result.close()
    associations
  }
  def addStateToMultiState(multi: MultipleState, state: State): MultipleState = new MultipleState(multi.getStates + state, multi.getMode)
  val assertedAbsenceAssociations = runQuery(builder.assertedAbsenceQuery)
  val assertedPresenceAssociations = runQuery(builder.assertedPresenceQuery)
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
  def mergeIntoMatrix(association: Association, presenceAbsence: PresenceAbsence, assertions: Set[Association]): Unit = {
    val character = characters.getOrElseUpdate(association.entity, {
      val newChar = new Character(association.entity)
      newChar.setLabel(association.entityLabel)
      dataset.addCharacter(newChar)
      newChar
    })
    val stateID = s"${association.entity}#${presenceAbsence.symbol}"
    val state = states.getOrElseUpdate(stateID, {
      val newState = new State(stateID)
      newState.setSymbol(presenceAbsence.symbol)
      newState.setLabel(presenceAbsence.label)
      newState
    })
    //if (assertions(association)) state.setComment("asserted") TODO add this
    if (!character.getStates.contains(state)) character.addState(state)
    val taxon = taxa.getOrElseUpdate(association.taxon, {
      val newTaxon = new Taxon()
      newTaxon.setPublicationName(association.taxonLabel)
      val oboID = NeXMLUtil.oboID(URI.create(association.taxon))
      newTaxon.setValidName(new OBOClassImpl(oboID))
      dataset.addTaxon(newTaxon)
      newTaxon
    })
    val currentState = dataset.getStateForTaxon(taxon, character)
    val stateToAssign = currentState match {
      case polymorphic: MultipleState => addStateToMultiState(polymorphic, state)
      case null => state
      case _ => new MultipleState(Set(currentState, state), MODE.POLYMORPHIC)
    }
    dataset.setStateForTaxon(taxon, character, stateToAssign)
    val supports = dataset.getAssociationSupport.getOrElseUpdate(new org.phenoscape.model.Association(taxon.getNexmlID, character.getNexmlID, state.getNexmlID), mutable.Set[AssociationSupport]())
    supports.add(new AssociationSupport(association.stateLabel, association.matrixLabel))
  }
  inferredAbsenceAssociations foreach (mergeIntoMatrix(_, Absence, assertedAbsenceAssociations))
  inferredPresenceAssociations foreach (mergeIntoMatrix(_, Presence, assertedPresenceAssociations))
  val writer = new NeXMLWriter(UUID.randomUUID.toString);
  writer.setDataSet(dataset);
  writer.write(new File(resultFile));
}