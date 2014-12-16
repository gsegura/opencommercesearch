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
import play.api.mvc.Action

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
}