package org.opencommercesearch.api.controllers

import java.text.SimpleDateFormat
import java.util.{Calendar, Date}
import javax.ws.rs.PathParam

import com.wordnik.swagger.annotations._
import org.apache.commons.lang.StringUtils
import org.opencommercesearch.api.service.OmnitureClient
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.Json
import play.api.mvc.Action

import scala.concurrent.Future

@Api(value = "reports", basePath = "/api-docs/reports", description = "Reports API endpoints.")
object ReportsController extends  BaseController {

  val dateFormatter = new SimpleDateFormat("yyyy-MM-dd")
  val bcReportSuite = "bcbackcountry"
  val reportMethod = "Report.GetRankedReport"

  @ApiOperation(value = "Page Revenue", notes = "Returns the revenue for the top N pages for a given time frame", httpMethod = "GET")
  @ApiImplicitParams(value = Array(
    new ApiImplicitParam(name = "top", value = "The top N results", defaultValue = "100", required = true, dataType = "string", paramType = "query")
  ))
  def getPageRevenue(version: Int,
                        @ApiParam(value = "The report time window", required = true)
                        @PathParam("days")
                        days: Int) = Action.async { implicit request =>

    val top = request.getQueryString("top").getOrElse("100")
    if (StringUtils.isEmpty(top)) {
      Future.successful(BadRequest("Missing required top query parameter"))
    } else {
      val dateFrom = dateFormatter.format(getLastXDays(-days))
      val dateTo = dateFormatter.format(new Date())

      //TODO gsegura: support adding the correct report suite per site, not hardcode bc report suite always
      val jsonTemplate = "{\"reportDescription\": {\"reportSuiteID\": \"" + bcReportSuite + "\",\"dateFrom\": \"" + dateFrom + "\", \"dateTo\": \""+ dateTo + "\",\"metrics\": [{\"id\":\"revenue\"}],\"elements\":[{\"id\":\"page\",\"top\":" + top + "}]}}"
      val data = Json.parse(jsonTemplate)

      OmnitureClient.callOmniture(data, reportMethod).map { response =>
        Ok(response.json)
      }
    }
  }

  private def getLastXDays(days: Int) = {
      val calendar = Calendar.getInstance();
      calendar.set(Calendar.HOUR_OF_DAY, 23);
      calendar.set(Calendar.MINUTE, 59);
      calendar.set(Calendar.SECOND, 59);
      calendar.set(Calendar.MILLISECOND, 999);
      calendar.add(Calendar.DATE, days);
      calendar.getTime();
  }
}
