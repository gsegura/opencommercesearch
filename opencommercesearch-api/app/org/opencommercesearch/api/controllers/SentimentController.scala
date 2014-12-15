package org.opencommercesearch.api.controllers

import javax.ws.rs.{QueryParam, PathParam}

import play.api.libs.concurrent.Execution.Implicits._
import com.wordnik.swagger.annotations._
import org.apache.solr.client.solrj.SolrQuery
import org.opencommercesearch.api.Global._
import org.opencommercesearch.api.models.{SentimentList, Facet}
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.Action

import scala.concurrent.Future

@Api(value = "sentiment", basePath = "/api-docs/sentiment", description = "Sentiment API endpoints.")
object SentimentController extends BaseController{

  @ApiOperation(value = "Searches facets", notes = "Returns information for a given facet", response = classOf[Facet], httpMethod = "GET")
  @ApiResponses(Array(new ApiResponse(code = 404, message = "Facet not found")))
  @ApiImplicitParams(value = Array(
    new ApiImplicitParam(name = "fields", value = "Comma delimited field list", defaultValue = "id,sentiment_d", required = false, dataType = "string", paramType = "query")
  ))
  def findById(
                version: Int,
                @ApiParam(value = "A product id", required = true)
                @PathParam("id")
                id: String,
                @ApiParam(defaultValue="false", allowableValues="true,false", value = "Display preview results", required = false)
                @QueryParam("preview")
                preview: Boolean) = Action.async { implicit request =>

    val query = withSentimentCollection(withFields(new SolrQuery(), request.getQueryString("fields")), preview, request.acceptLanguages)

    query.add("q", "productId_s:" + id)

    Logger.debug("Query Sentiment for productId: " + id)
    val future = solrServer.query(query).map( response => {
      val results = response.getResults
      Logger.debug("Num found " + results.getNumFound)
      if(results.getNumFound > 0 && results.get(0) != null) {
        val doc = results.get(0)
        Logger.debug("Found sentiment for productId: " + id)
        Ok(Json.obj("results" -> SentimentList.fromDefinition(results)))
      }
      else {
        Logger.debug("Sentiment for productId: " + id + " not found")
        NotFound(Json.obj(
          "message" -> s"Cannot find sentiment for product id [$id]"
        ))
      }
    })

    withErrorHandling(future, s"Cannot retrieve sentiment for product id [$id]")
  }
}
