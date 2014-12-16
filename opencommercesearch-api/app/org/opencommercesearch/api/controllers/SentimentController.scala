package org.opencommercesearch.api.controllers

import javax.ws.rs.PathParam

import com.wordnik.swagger.annotations._
import org.apache.commons.lang.StringUtils
import org.apache.solr.client.solrj.SolrQuery
import org.opencommercesearch.api.Global._
import org.opencommercesearch.api.models.SentimentList
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, Request}

import scala.collection.mutable
import scala.concurrent.Future

@Api(value = "sentiment", basePath = "/api-docs/sentiment", description = "Sentiment API endpoints.")
object SentimentController extends BaseController {

  @ApiOperation(value = "Find product sentiment", notes = "Returns sentiment of a given set of products", response = classOf[SentimentList], httpMethod = "GET")
  @ApiResponses(Array(new ApiResponse(code = 404, message = "Sentiment not found")))
  @ApiImplicitParams(value = Array(
    new ApiImplicitParam(name = "fields", value = "Comma delimited field list", defaultValue = "id,sentiment_d", required = false, dataType = "string", paramType = "query")
  ))
  def getByProductList(
                        version: Int,
                        @ApiParam(value = "A list of product ids", required = true)
                        @PathParam("productIds")
                        productIds: String
                        ) = Action.async { implicit request =>

    val query = withSentimentCollection(withFields(new SolrQuery(), Option("id,sentiment_d")), request.acceptLanguages)

    val productList = StringUtils.split(productIds, ",");

    val stringBuilder = new StringBuilder("id: (");
    val appendOperator = " or ";
    productList foreach { id =>
      stringBuilder append (id);
      stringBuilder append (appendOperator);
    }

    stringBuilder.setLength(stringBuilder.size - appendOperator.size);
    stringBuilder.append(")");

    query.add("q", "*:*");
    query.add("fq", stringBuilder.toString());

    Logger.debug("Query Sentiment for this query: " + stringBuilder.toString());
    val future = solrServer.query(query).map(response => {
      val results = response.getResults
      Logger.debug("Num found " + results.getNumFound)
      if (results.getNumFound > 0 && results.get(0) != null) {
        withCorsHeaders(Ok(Json.obj("results" -> SentimentList.fromDefinition(results))))
      }
      else {
        Logger.debug("Sentiment for productIds: " + productIds + " not found")
        withCorsHeaders(NotFound(Json.obj(
          "message" -> s"Cannot find sentiment for productIds [$productIds]"
        )))
      }
    })

    withErrorHandling(future, s"Cannot retrieve sentiment for productIds [$productIds]")
  }

  @ApiOperation(value = "Find aggregated sentiment", notes = "Returns aggregated sentiment for a given category, brand or brand category", httpMethod = "GET")
  @ApiImplicitParams(value = Array(
    new ApiImplicitParam(name = "categoryId", value = "A category id", required = false, dataType = "string", paramType = "query"),
    new ApiImplicitParam(name = "brandId", value = "A brand id", required = false, dataType = "string", paramType = "query")
  ))
  @ApiResponses(Array(new ApiResponse(code = 404, message = "Sentiment not found")))
  def getAggregatedSentiment(version: Int) = Action.async { implicit request =>

    val query = withSentimentCollection(withFields(new SolrQuery(), Option("")), request.acceptLanguages)

    val categoryId = request.getQueryString("categoryId").getOrElse("")
    if(StringUtils.isNotBlank(categoryId)) {
      query.add("fq", "categories_ss: " + categoryId);
    }

    val brandId = request.getQueryString("brandId").getOrElse("")
    if(StringUtils.isNotBlank(brandId)) {
      query.add("fq", "brandId_s: " + brandId);
    }

    query.add("q", "*:*");
    query.add("stats", "true");
    query.add("stats.field", "sentiment_d");
    query.add("rows", "0");

    Logger.debug(s"Query Sentiment for categoryId [$categoryId] and brandId [$brandId]");
    val future = solrServer.query(query).map(response => {
      val stats = response.getFieldStatsInfo
      if (stats != null && stats.size() > 0) {
        val statsEntry = stats.get("sentiment_d")
        withCorsHeaders(Ok(Json.obj("sentiment" -> statsEntry.getMean.toString)))
      } else {
        Logger.debug(s"Sentiment for categoryId [$categoryId] and brandId [$brandId] not found")
        withCorsHeaders(NotFound(Json.obj(
          "message" -> s"Cannot find sentiment for categoryId [$categoryId] brandId [$brandId]"
        )))
      }
    })

    withErrorHandling(future, s"Cannot retrieve sentiment for categoryId [$categoryId] brandId [$brandId]")
  }

  @ApiOperation(value = "Find tendencies sentiment", notes = "Returns top 10 sentiment for a given category, brand or brand category", httpMethod = "GET")
  @ApiImplicitParams(value = Array(
    new ApiImplicitParam(name = "categoryId", value = "A category id", required = false, dataType = "string", paramType = "query")
  ))
  @ApiResponses(Array(new ApiResponse(code = 404, message = "Sentiment not found")))
  def getTendencies(version: Int) = Action.async { implicit request =>

    val query = withSentimentCollection(withFields(new SolrQuery(), Option("")), request.acceptLanguages)

    val categoryId = request.getQueryString("categoryId").getOrElse("")
    if(StringUtils.isNotBlank(categoryId)) {
      query.add("fq", "categories_ss: " + categoryId);
    }

    query.add("q", "*:*");
    query.add("rows", "0");
    query.add("facet", "true");
    query.add("facet.field", "brandId_s");


    Logger.debug(s"Query Tendencies for brands");
    val future = solrServer.query(query).flatMap(response => {
      val facetFields = response.getFacetFields
      if (facetFields != null && facetFields.size() > 0) {
        val values = facetFields.get(0).getValues
        val futureList = mutable.ArrayBuffer[Future[Option[Tuple2[String, Double]]]]()
        val aggregates = mutable.HashMap[String, Double]();
        var nodeIndex = 0;
        for(nodeIndex <- 0 to values.size()-1 ){
          val nodeName = values.get(nodeIndex).getName
          futureList.append(getBrandIdAggregates(nodeName, request, aggregates))
        }

        Future.sequence(futureList) map { tupleList =>
          tupleList.map { tuples => {
              if(tuples.isDefined) {
                aggregates.put(tuples.get._1, tuples.get._2)
              }
            }
          }
          withCorsHeaders(Ok(Json.obj("results" -> Json.toJson(aggregates.toMap))))
        }
      } else {
        Logger.debug(s"Tendencies for brands not found")
        Future.successful(withCorsHeaders(NotFound(Json.obj("message" -> s"Cannot find tendencies for brands"))))
      }
    })

    withErrorHandling(future, s"Cannot retrieve tendencies for brands")
  }

  private def getBrandIdAggregates (brandId : String, request : Request[AnyContent], aggregate : mutable.Map[String, Double]) : Future[Option[Tuple2[String, Double]]] = {
    val query = withSentimentCollection(withFields(new SolrQuery(), Option("brandId_s,sentiment_d")), request.acceptLanguages)

    query.add("q", "*:*");
    query.add("fq", "brandId_s: (" + brandId + ")");
    query.add("stats", "true");
    query.add("stats.field", "sentiment_d");
    query.add("rows", "0");

    Logger.debug("Query Sentiment for this query: " + query.toString());
    val future = solrServer.query(query).map(response => {

      val stats = response.getFieldStatsInfo
      if (stats != null && stats.size() > 0) {
        val statsEntry = stats.get("sentiment_d")
        Option(new Tuple2(brandId, statsEntry.getMean.asInstanceOf[Double]))
      } else {
        Logger.warn("No sentiments found for BrandId: [$brandId]")
        None
      }
    })
    future
  }

}