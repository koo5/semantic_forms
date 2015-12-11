package deductions.runtime.services

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.xml.Elem
import org.w3.banana.RDF
import org.w3.banana.SparqlOpsModule
import org.w3.banana.TryW
import org.w3.banana.syntax._
import deductions.runtime.dataset.RDFStoreLocalProvider
import deductions.runtime.html.Form2HTML
import deductions.runtime.abstract_syntax.PreferredLanguageLiteral
import org.w3.banana.Transactor
import org.w3.banana.RDFOpsModule
import org.w3.banana.RDFOps
import deductions.runtime.abstract_syntax.InstanceLabelsInferenceMemory
import scala.xml.NodeSeq
import scala.xml.Text
import scala.util.Try

trait SPARQLQueryMaker {
  def makeQueryString(search: String): String
}

/**
 * Generic SPARQL SELECT Search with single parameter,
 * and single return URI value,
 *  and showing in HTML a column of hyperlinked results with instance Labels
 */
trait ParameterizedSPARQL[Rdf <: RDF, DATASET]
    extends RDFStoreLocalProvider[Rdf, DATASET]
    with InstanceLabelsInferenceMemory[Rdf, DATASET]
    with PreferredLanguageLiteral[Rdf]
    with SPARQLHelpers[Rdf, DATASET] {

  import ops._
  import sparqlOps._
  import rdfStore.transactorSyntax._
  import rdfStore.sparqlEngineSyntax._

  /**
   * Generic SPARQL SELECT with single result columns (must be named "thing"),
   *  search and display results as an XHTML element
   */
  def search(search: String, hrefPrefix: String = "",
             lang: String = "")(implicit queryMaker: SPARQLQueryMaker): Future[Elem] = {
    println(s"search: starting TRANSACTION for dataset $dataset")
    val elem0 = dataset.rw({
    	val uris = search_onlyNT(search)
      println(s"after search_only uris $uris")
    	val graph: Rdf#Graph = allNamedGraph
      println(s"displayResults : 1" + graph  ) // ZZZZZZZZZZZZZZZZZZZZZZ
      val elem = uris.map(
        u => displayResults(u.toIterable, hrefPrefix, lang, graph))
      elem
    })
    println(s"search: leaving TRANSACTION for dataset $dataset")
    val elem = elem0.get
    elem.asFuture
  }

  /**
   * Generic SPARQL SELECT Search with multiple result columns
   *  search and display results as an XHTML element
   */
  def search2(search: String, hrefPrefix: String = "",
              lang: String = "")(implicit queryMaker: SPARQLQueryMaker)
  = {
    val uris = search_only2(search)
    println(s"after search_only uris $uris")
    val elem0 =
      dataset.rw({
    	  val graph: Rdf#Graph = allNamedGraph
    	 println(s"displayResults : 1") // ZZZZZZZZZZZZZZZZZZZZZZ
        uris.map(
            // TODO create table like HTML
          u => displayResults(u.toIterable, hrefPrefix, lang, graph))
      })
    val elem = elem0.get
    elem
  }
  
  /** generate a column of HTML hyperlinks for given list of RDF Node;
   *  non TRANSACTIONAL
   */
  private def displayResults(res0: Iterable[Rdf#Node], hrefPrefix: String,
      lang: String = "",
      graph: Rdf#Graph ) = {
    <p>{
      val res = res0.toSeq
      println(s"displayResults :\n${res.mkString("\n")}")
      val uriLabelCouples = res.map(uri => (uri, instanceLabel(uri, graph, lang )))
      println(s"displayResults : 2") // ZZZZZZZZZZZZZZZZZZZZZZ
        uriLabelCouples.sortBy( c => c._2) .
        map( uriLabelCouple => { val uri = uriLabelCouple._1
            val uriString = uri.toString
            val blanknode = !isURI(uri)
            // TODO : show named graph
            <div title={ uri.toString() } class="form-row" >
              <a href={
                Form2HTML.createHyperlinkString(hrefPrefix, uriString, blanknode) }
              class="form-value" >
                { uriLabelCouple._2 }
              </a><br/>
							{ columnsForURI( uriLabelCouple._1, uriLabelCouple._2) }
            </div>
          })
    }</p>
  }

  /** overridable function for adding columns in response */
  def columnsForURI( node: Rdf#Node, label: String): NodeSeq = Text("")

  /**
   * TRANSACTIONAL
   *
   * CAUTION: It is of particular importance to note that
   * one should never use an Iterator after calling a method on it;
   * cf http://stackoverflow.com/questions/18420995/scala-iterator-one-should-never-use-an-iterator-after-calling-a-method-on-it
   */
  private def search_only(search: String)
  (implicit queryMaker: SPARQLQueryMaker): Future[Iterator[Rdf#Node]] = {
    val transaction =
      dataset.r({
    	  search_onlyNT(search)
      })
    val tryIteratorRdfNode = transaction.flatMap { identity }
    println( s"after search_only(search tryIteratorRdfNode $tryIteratorRdfNode" )
    tryIteratorRdfNode.asFuture
  }
  
  private def search_onlyNT(search: String)
  (implicit queryMaker: SPARQLQueryMaker): Try[Iterator[Rdf#Node]] = {
    val queryString = queryMaker.makeQueryString(search)
    println( s"search_only(search $search" )
        println(s"search_only: starting TRANSACTION for dataset $dataset")
        val result = for {
          query <- parseSelect(queryString)
          solutions <- dataset.executeSelect(query, Map())
        } yield {
          solutions.toIterable.map {
            row =>
              row("thing") getOrElse sys.error(s"search_only($search) : no ?thing in row")
          }
        }
    println( s"after search_only(search $search" )
    result
  }

  private def search_only2(search: String)
  (implicit queryMaker: SPARQLQueryMaker): List[List[Rdf#Node]] = {
    val queryString = queryMaker.makeQueryString(search)
	  println( s"search_only2( search $search" )
    val res = sparqlSelectQuery(queryString)
//            ds: DATASET=dataset): Try[List[Set[Rdf#Node]]]
	  ???
  }

}
