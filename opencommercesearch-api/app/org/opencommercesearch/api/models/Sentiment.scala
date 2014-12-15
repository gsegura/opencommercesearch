package org.opencommercesearch.api.models

import com.fasterxml.jackson.annotation.JsonProperty
import org.apache.solr.common.{SolrDocument, SolrDocumentList}
import org.jongo.marshall.jackson.oid.Id
import play.api.libs.json.Json

import scala.collection.JavaConversions._

case class Sentiment(
                      @Id var id: Option[String] = None,
                      @JsonProperty("productId") var productId: Option[String] = None,
                      @JsonProperty("categories") var categories: Option[Seq[String]] = None,
                      @JsonProperty("brandId") var brandId: Option[String] = None,
                      @JsonProperty("siteId") var siteId: Option[String] = None,
                      @JsonProperty("sentiment") var sentiment: Option[Double] = None) {

}

object Sentiment {
  implicit val readsSentiment = Json.reads[Sentiment]
  implicit val writesSentiment = Json.writes[Sentiment]

  def fromDefinition(sentimentDefinition: SolrDocument): Sentiment = {
    val value = sentimentDefinition.get("sentiment_d")
    val categories = sentimentDefinition.get("categories_ss").asInstanceOf[java.util.List[String]]

    val sentiment = new Sentiment(
      id = Option(sentimentDefinition.get("id").asInstanceOf[String]),
      productId = Option(sentimentDefinition.get("productId_s").asInstanceOf[String]),
      brandId = Option(sentimentDefinition.get("brandId_s").asInstanceOf[String]),
      siteId = Option(sentimentDefinition.get("siteId_s").asInstanceOf[String]),
      sentiment =  if (value != null) Some(value.asInstanceOf[Double]) else None,
      categories = if (categories != null) Some(categories.toList) else None
    )
    sentiment
  }
}

case class SentimentList(sentiments: List[Sentiment]) {
}

object SentimentList {
  implicit val readsSentimentList = Json.reads[SentimentList]
  implicit val writesSentimentList = Json.writes[SentimentList]

  def fromDefinition(solrDocuments: SolrDocumentList): List[Sentiment] = {
    val docs = solrDocuments.map( solrDocument => {
      Sentiment.fromDefinition(solrDocument)
    })
    docs.toList
  }
}
