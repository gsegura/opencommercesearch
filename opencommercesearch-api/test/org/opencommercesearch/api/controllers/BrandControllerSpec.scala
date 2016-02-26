package org.opencommercesearch.api.controllers

/*
* Licensed to OpenCommerceSearch under one
* or more contributor license agreements. See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership. OpenCommerceSearch licenses this
* file to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License. You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied. See the License for the
* specific language governing permissions and limitations
* under the License.
*/

import org.apache.solr.client.solrj.response.FacetField
import org.apache.solr.client.solrj.{AsyncSolrServer, SolrQuery, SolrRequest}
import org.apache.solr.common.SolrDocumentList
import org.opencommercesearch.api.Global._
import org.opencommercesearch.api.models.Brand
import org.opencommercesearch.api.service.{MongoStorage, MongoStorageFactory}
import org.specs2.mutable._
import play.api.libs.json.{JsError, Json}
import play.api.test.Helpers._
import play.api.test._
import reactivemongo.core.commands.LastError

import scala.concurrent.Future

class BrandControllerSpec extends BaseSpec {

  trait Brands extends Before {
    val storage = mock[MongoStorage]

    def before = {
      solrServer = mock[AsyncSolrServer]

      storageFactory = mock[MongoStorageFactory]
      storageFactory.getInstance(anyString) returns storage
      val writeResult = mock[LastError]
      storage.saveBrand(any) returns Future.successful(writeResult)

    }
  }

  sequential

  "Brand Controller" should {

    "send 404 on an unknown route" in new Brands {
      running(FakeApplication()) {
        route(FakeRequest(GET, "/boum")) must beNone
      }
    }

    "send 404 when a brand is not found"  in new Brands {
      running(FakeApplication()) {
        val expectedId = "1000"

        storage.findBrand(anyString, any) returns Future.successful(null)

        val result = route(FakeRequest(GET, routes.BrandController.findById(version = 1, expectedId).url))
        validateQueryResult(result.get, NOT_FOUND, "application/json", s"Cannot find brand with id [$expectedId]")
        there was one(storage).findBrand(anyString, any)
      }
    }

    "send 200 when a brand is found" in new Brands {
      running(FakeApplication()) {
        val (expectedId, expectedName, expectedLogo) = ("1000", "A Brand", "/brands/logo.jpg")

        val brand = new Brand(id = Some(expectedId), name = Some(expectedName), logo = Some(expectedLogo))
        storage.findBrand(anyString, any) returns Future.successful(brand)

        val result = route(FakeRequest(GET, routes.BrandController.findById(version = 1, expectedId).url))

        validateQueryResult(result.get, OK, "application/json")
        there was one(storage).findBrand(anyString, any)

        val json = Json.parse(contentAsString(result.get))
        (json \ "brand").validate[Brand].map { brand =>
          brand.id.get must beEqualTo(expectedId)
          brand.name.get must beEqualTo(expectedName)
          brand.logo.get must beEqualTo(expectedLogo)
        } recoverTotal {
          e => failure("Invalid JSON for brand: " + JsError.toFlatJson(e))
        }
      }
    }

    "send 200 when multiple brands are found" in new Brands {
      running(FakeApplication()) {
        val (expectedId, expectedName, expectedLogo) = ("1000", "A Brand", "/brands/logo.jpg")

        val brand1 = new Brand(id = Some(expectedId + "1"), name = Some(expectedName + "1"), logo = Some(expectedLogo))
        val brand2 = new Brand(id = Some(expectedId + "2"), name = Some(expectedName + "2"), logo = Some(expectedLogo))
        storage.findBrands(any[Iterable[String]], any) returns Future.successful(Seq(brand1, brand2))

        val result = route(FakeRequest(GET, routes.BrandController.findById(version = 2, "10001,10002").url))

        validateQueryResult(result.get, OK, "application/json")

        val captor = capture[List[String]]
        there was one(storage).findBrands(captor, any)
        captor.value.head must beEqualTo("10001");
        captor.value.tail.head must beEqualTo("10002");

        val json = Json.parse(contentAsString(result.get))
        (json \ "brands").validate[List[Brand]].map { brands =>
          brands.head must beEqualTo(brand1)
          brands.tail.head must beEqualTo(brand2)
        } recoverTotal {
          e => failure("Invalid JSON for brands: " + JsError.toFlatJson(e))
        }
      }
    }

    "send 201 when a brand is created" in new Brands {
      running(FakeApplication()) {
        val (queryResponse, _) = setupQuery

        val documentList = mock[SolrDocumentList]
        documentList.getNumFound returns 5
        queryResponse.getResults returns documentList
        queryResponse.getFacetFields returns getFacetFields

        setupUpdate
        val (expectedId, expectedName, expectedLogo) = ("1000", "A Brand", "/brands/logo.jpg")
        val json = Json.obj(
          "id" -> expectedId,
          "name" -> expectedName,
          "logo" -> expectedLogo
        )

        val url = routes.BrandController.createOrUpdate(expectedId).url
        val fakeRequest = FakeRequest(PUT, url)
          .withHeaders((CONTENT_TYPE, "application/json"))
          .withJsonBody(json)

        val result = route(fakeRequest)
        validateUpdateResult(result.get, CREATED, url)

        there was one(solrServer).query(any[SolrQuery])
        there was one(solrServer).request(any[SolrRequest])
        there was one(storage).saveBrand(any)
      }
    }

    "send 400 when trying to create a brand with missing fields" in new Brands {
      running(FakeApplication()) {
        val (updateResponse) = setupUpdate
        val expectedId = "1000"
        val json = Json.obj(
          "id" -> expectedId
        )

        val url = routes.BrandController.createOrUpdate(expectedId).url
        val fakeRequest = FakeRequest(PUT, url)
          .withHeaders((CONTENT_TYPE, "application/json"))
          .withJsonBody(json)

        val result = route(fakeRequest)
        validateFailedUpdate(updateResponse)
        validateFailedUpdateResult(result.get, BAD_REQUEST, "Missing required fields")
      }
    }

    "send 400 when not sending a JSON body" in new Brands {
      running(FakeApplication()) {
        val (updateResponse) = setupUpdate
        val expectedId = "1000"

        val url = routes.BrandController.createOrUpdate(expectedId).url
        val fakeRequest = FakeRequest(PUT, url)
          .withHeaders((CONTENT_TYPE, "text/plain"))

        val result = route(fakeRequest)
        validateFailedUpdate(updateResponse)
        validateUpdateResult(result.get, BAD_REQUEST)
      }
    }

    "send 400 when exceeding maximum brands an a bulk create" in new Brands {
      running(FakeApplication(additionalConfiguration = Map("index.brand.batchsize.max" -> 2))) {
        val (updateResponse) = setupUpdate
        val (expectedId, expectedName, expectedLogo) = ("1000", "A Brand", "/brands/logo.jpg")
        val json = Json.obj(
          "feedTimestamp" -> 1000,
          "brands" -> Json.arr(
            Json.obj(
              "id" -> expectedId,
              "name" -> expectedName,
              "logo" -> expectedLogo),
            Json.obj(
              "id" -> (expectedId + "1"),
              "name" -> (expectedName + " X"),
              "logo" -> expectedLogo),
            Json.obj(
              "id" -> (expectedId + "2"),
              "name" -> (expectedName + " Y"),
              "logo" -> expectedLogo)))

        val url = routes.BrandController.bulkCreateOrUpdate().url
        val fakeRequest = FakeRequest(PUT, url)
          .withHeaders((CONTENT_TYPE, "application/json"))
          .withJsonBody(json)

        val result = route(fakeRequest)
        validateFailedUpdate(updateResponse)
        validateFailedUpdateResult(result.get, BAD_REQUEST, "Exceeded number of brands. Maximum is 2")
      }
    }

    "send 400 when trying to bulk create brands with missing fields" in new Brands {
      running(FakeApplication(additionalConfiguration = Map("index.brand.batchsize.max" -> 2))) {
        val (updateResponse) = setupUpdate
        val (expectedId, expectedName, expectedLogo) = ("1000", "A Brand", "/brands/logo.jpg")
        val json = Json.obj(
          "brands" -> Json.arr(
            Json.obj(
              "id" -> expectedId,
              "name" -> expectedName,
              "logo" -> expectedLogo),
            Json.obj(
              "id" -> (expectedId + "2"),
              "logo" -> expectedLogo)))

        val url = routes.BrandController.bulkCreateOrUpdate().url
        val fakeRequest = FakeRequest(PUT, url)
          .withHeaders((CONTENT_TYPE, "application/json"))
          .withJsonBody(json)

        val result = route(fakeRequest)
        validateFailedUpdateResult(result.get, BAD_REQUEST, "Missing required fields")
        validateFailedUpdate(updateResponse)
      }
    }

    "send 201 when brands are created" in new Brands {
      running(FakeApplication()) {
        val (queryResponse, _) = setupQuery

        val documentList = mock[SolrDocumentList]
        documentList.getNumFound returns 5
        queryResponse.getResults returns documentList
        queryResponse.getFacetFields returns getFacetFields

        setupUpdate
        val (expectedId, expectedName, expectedLogo) = ("1000", "A Brand", "/brands/logo.jpg")
        val (expectedId2, expectedName2, expectedLogo2) = ("1001", "Another Brand", "/brands/logo2.jpg")
        val json = Json.obj(
          "feedTimestamp" -> 1000,
          "brands" -> Json.arr(
            Json.obj(
              "id" -> expectedId,
              "name" -> expectedName,
              "logo" -> expectedLogo),
            Json.obj(
              "id" -> expectedId2,
              "name" -> expectedName2,
              "logo" -> expectedLogo2)))

        val url = routes.BrandController.bulkCreateOrUpdate().url
        val fakeRequest = FakeRequest(PUT, url)
          .withHeaders((CONTENT_TYPE, "application/json"))
          .withJsonBody(json)

        val result = route(fakeRequest)
        validateUpdateResult(result.get, CREATED)

        there was one(solrServer).query(any[SolrQuery])
        there was one(solrServer).request(any[SolrRequest])
        there was one(storage).saveBrand(any)
      }
    }
  }

  private def getFacetFields = {
    val facetFields = new java.util.LinkedList[FacetField]()
    val brandFacet = new FacetField("brand")
    brandFacet.add("1000", 3)
    brandFacet.add("1001", 2)
    facetFields.add(brandFacet)
    facetFields
  }
}
