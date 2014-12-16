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
  val overTimeReport = "Report.GetOvertimeReport"

  @ApiOperation(value = "PLP Revenue", notes = "Returns the revenue for a plp page for a given time frame", httpMethod = "GET")
  @ApiImplicitParams(value = Array(
    new ApiImplicitParam(name = "pageType", value = "Page type & id", defaultValue = "PLP Cat:Men's Down Jackets", required = true, dataType = "string", paramType = "query")
  ))
  def getPlpPageRevenue(version: Int,
                        @ApiParam(value = "The report time window", required = true)
                        @PathParam("days")
                        days: Int) = Action.async { implicit request =>

    val pageType = request.getQueryString("pageType").getOrElse("")
    if (StringUtils.isEmpty(pageType)) {
      Future.successful(BadRequest("Missing required pageType query parameter"))
    } else {
      val dateFrom = dateFormatter.format(getLastXDays(-days))
      val dateTo = dateFormatter.format(new Date())

      //TODO gsegura: support adding the correct report suite per site, not hardcode bc report suite always
      val jsonTemplate = "{\"reportDescription\": {\"reportSuiteID\": \"" + bcReportSuite + "\",\"dateFrom\": \"" + dateFrom + "\", \"dateTo\": \""+ dateTo + "\",\"metrics\": [{\"id\":\"revenue\"}],\"elements\":[{\"id\":\"eVar36\",\"selected\":[\"" + pageType + "\"]}]}}"
      val data = Json.parse(jsonTemplate)

      OmnitureClient.callOmniture(data, overTimeReport).map { response =>
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
