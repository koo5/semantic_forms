package deductions.runtime.html

import deductions.runtime.utils.RDFPrefixesInterface
import org.joda.time.DateTime

import scala.xml.{NodeSeq, Unparsed}
import deductions.runtime.core.HTTPrequest
import java.net.URLEncoder
import deductions.runtime.core.HTMLutils

import scalaz._
import Scalaz._

/** generate HTML from abstract Form for Display (Read only) */
trait Form2HTMLDisplay[NODE, URI <: NODE]
  extends Form2HTMLBase[NODE, URI]
  with RDFPrefixesInterface
  with HTMLutils {

  import config._

  private[html] def createHTMLiteralReadonlyField(
    literalEntry: formMod#LiteralEntry,
    request:      HTTPrequest          = HTTPrequest()): NodeSeq = {
    <span>
        {
          val valueDisplayed =
            if (literalEntry.type_.toString().endsWith("dateTime"))
              literalEntry.valueLabel.replaceFirst("T00:00:00$", "")
            else
              literalEntry.valueLabel
          Unparsed(valueDisplayed)
        }
        { makeUserInfoOnTriples(literalEntry, request.getLanguage()) }
        <div>{ if (literalEntry.lang  =/=  "" && literalEntry.lang  =/=  "No_language") " > " + literalEntry.lang }</div>
      </span>
  }

  /** create HTML Resource Readonly Field
    * PENDING if inner val should need to be overridden, they should be directly in trait
    *  */
  def createHTMLResourceReadonlyField(
                                       resourceEntry: formMod#ResourceEntry,
                                       // TODO remove arg:
                                       hrefPrefixxxxx: String = hrefDisplayPrefix,
                                       request: HTTPrequest = HTTPrequest()
                                     ): NodeSeq = {

    import resourceEntry._

    val objectURIstringValue = value.toString()
    val css = cssForURI(objectURIstringValue)

    val typ = firstNODEOrElseEmptyString(type_)
//  println(s"==== createHTMLResourceReadonlyField: typ: $typ")

    val widgets =
      hyperlinkToField(resourceEntry) ++
      hyperlinkToURI(hrefDisplayPrefix, objectURIstringValue, valueLabel,
          typ, // TODO pass type_
          resourceEntry) ++
      displayThumbnail(resourceEntry) ++
      backLinkButton(resourceEntry) ++
      makeUserInfoOnTriples(resourceEntry, request.getLanguage()) ++
      showHideHTMLOnClick(
          normalNavigationButton(resourceEntry) ++
            makeDrawGraphLink(objectURIstringValue) ++
            creationButton(
              objectURIstringValue,
              type_.map { t => t.toString() },
              request.getLanguage()) ++
              makeClassTableButton(resourceEntry) ++
              hyperlinkForEditingURIinsideForm(objectURIstringValue, request.getLanguage()),
              resourceEntry.value.toString(),
              <button title={s"""Show "expert" buttons: navigate, edit, graph, for value <${resourceEntry.value}>"""}
              style="height: 20px"
              >...</button>)

      <span class="sf-statistics">{widgets}</span>
  }

  /** TODO duplication with preceding function */
  def createHTMLResourceReadonlyFieldBriefly(
    resourceEntry: formMod#ResourceEntry,
    request:       HTTPrequest           = HTTPrequest()): NodeSeq = {
    import resourceEntry._
    val typ = firstNODEOrElseEmptyString(type_)
    val objectURIstringValue = value.toString()
    hyperlinkToURI(hrefDisplayPrefix, objectURIstringValue, valueLabel,
      typ, // TODO pass type_
      resourceEntry) ++
      displayThumbnail(resourceEntry)
  }

  /** hyperlink To RDF property */
  private def hyperlinkToField(resourceEntry: formMod#ResourceEntry
//      , objectURIstringValue: String
      ) = {
    val id = urlEncode(resourceEntry.property).replace("%", "-")
    /*  ID and NAME tokens must begin with a letter ([A-Za-z]) and may be followed by any number of letters,
     *  digits ([0-9]), hyphens ("-"), underscores ("_"), colons (":"), and periods ("."). */

    val objectURIstringValue = resourceEntry.value.toString()
    if( objectURIstringValue  =/=  "" && id  =/=  "") {
      <a href={ "#" + id } draggable="true">
        <i class="glyphicon glyphicon-link"></i>
      </a>
        <a id={ id }></a>
    } else NodeSeq.Empty
  }

  private[html] def hyperlinkToURI(hrefPrefix: String, objectURIstringValue: String, valueLabel: String,
      type_ : String, resourceEntry: formMod#ResourceEntry) = {
    val types0 = resourceEntry.type_.mkString(", ")
    val types = if(types0 == "") type_ else types0
    addTripleAttributesToXMLElement(
      <a href={createHyperlinkString(hrefPrefix, objectURIstringValue)} class={cssForURI(objectURIstringValue)} title=
      {s"""Value ${if (objectURIstringValue  =/=  valueLabel) objectURIstringValue else ""}
              of type(s) ${types}"""} draggable="true">
        {valueLabel}
      </a>,
      resourceEntry)
  }

  private def backLinkButton(resourceEntry: formMod#ResourceEntry) = {
    import resourceEntry._
    val objectURIstringValue = resourceEntry.value.toString()
    (if (objectURIstringValue.size > 0 && showExpertButtons) {
      val title = s""" Reverse links reaching "$valueLabel" <$value> """
      makeBackLinkButton(objectURIstringValue, title = title)
    } else NodeSeq.Empty)
  }

  private def normalNavigationButton(resourceEntry: formMod#ResourceEntry) = {
    val objectURIstringValue = resourceEntry.value.toString()
    (if (objectURIstringValue.size > 0 &&
        isDownloadableURL(objectURIstringValue) && showExpertButtons) {
    	val value = resourceEntry.value
      <a class="btn btn-primary btn-xs" href={objectURIstringValue} title={s"Normal HTTP link to $value"}
         draggable="true">
        <i class="glyphicon glyphicon-share-alt"></i>
      </a>
    } else NodeSeq.Empty)
  }

  private def displayThumbnail(resourceEntry: formMod#ResourceEntry): NodeSeq = {
    import resourceEntry._
    val imageURL = if (isImage) Some(value)
    else thumbnail
    if (isImage || thumbnail.isDefined) {
      <a class="image-popup-vertical-fit" href={ imageURL.get.toString() } title={ s"Image of $valueLabel: ${value.toString()}" }>
        <img src={ imageURL.get.toString() } css="sf-thumbnail" height="40" alt={ s"Image of $valueLabel: ${value.toString()}" }/>
      </a>
    } else NodeSeq.Empty
  }

  private[html] def createHTMLBlankNodeReadonlyField(
                                                      r: formMod#BlankNodeEntry,
                                                      hrefPrefix: String = config.hrefDisplayPrefix) =
    <a href={ Form2HTML.createHyperlinkString(hrefPrefix, r.value.toString, true) }>{
      r.valueLabel
      }</a> ++
      {makeUserInfoOnTriples(r)}

  def creationButton(classURIstringValue: String, types: Seq[String], lang: String): NodeSeq = {
    val imageURL = "/assets/images/create-instance.svg"
    implicit val _ = lang
    val messCreate_instance = mess("Create_instance_of") + s" <$classURIstringValue>"
    if ( types.exists { t => t.endsWith("#Class") } ) {
//      println(s"==== creationButton: typ: $typ")
        <a href={
          "/create?uri=" + URLEncoder.encode(classURIstringValue, "UTF-8")
        } title={ messCreate_instance }>
          <img src={ imageURL } css="sf-thumbnail" height="40" alt={ messCreate_instance }/>
        </a>
    } else NodeSeq.Empty
  }

  private def makeClassTableButton(resourceEntry: FormEntry // ResourceEntry
  ): NodeSeq = {
    val classURI = toPlainString(resourceEntry.value)
    val triple =
      if (resourceEntry.isClass) {
        s"?S a <$classURI> ."
      } else
        s"?S ?P1 <$classURI> ."

    val sparlqlQuery = s"""
      CONSTRUCT {
        ?S ?P ?O .
      } WHERE {
        GRAPH ?G {
        ?S ?P ?O .
        $triple
      } }
      """
    <a href={ "/table?query=" + URLEncoder.encode(sparlqlQuery, "UTF-8") } target="_blank">TABLE</a>
  }


  /**
   * Statistics about languages In Data:
   *  - total number of literal triples,
   *  - excluded ones
   */
  def languagesInDataStatistics(
    form:    formMod#FormSyntax,
    request: HTTPrequest): NodeSeq = {
    val fittingCount = countLanguageDataFittingRequest( form, request)
    val literalEntriesCount = getLiteralEntries(form).size

    logger.info(s"fittingCount $fittingCount, literalEntriesCount $literalEntriesCount")
    // form.fields.collect { case l: formMod#LiteralEntry => l } . foreach ( println(_) )
    if(fittingCount  =/=  literalEntriesCount)
      <span>{literalEntriesCount - fittingCount} data not fitting user language ({request.getLanguage()})</span>
      <button
        onclick="
console.log(document.getElementsByClassName('sf-data-not-fitting-user-language'));
Array.from(document.getElementsByClassName('sf-data-not-fitting-user-language')).map(
  elem => elem.style.display='block' );"
      >
        Show all
      </button>
    else NodeSeq.Empty
  }

  private def getLiteralEntries(form: formMod#FormSyntax) = {
    form.fields.collect {
//        case l: formMod#LiteralEntry if ( l.valueLabel =/= "" ) => l
        case l: formMod#LiteralEntry if ( toPlainString(l.value) =/= "" ) => l
    }
  }

  private def countLanguageDataFittingRequest(
    form:    formMod#FormSyntax,
    request: HTTPrequest): Int = {
    getLiteralEntries(form).count(
      f => f match {
        case l => isLanguageDataFittingRequest(l, request)
      })
  }
}
