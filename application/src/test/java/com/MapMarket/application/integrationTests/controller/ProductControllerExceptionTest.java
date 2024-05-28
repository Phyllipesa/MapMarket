package com.MapMarket.application.integrationTests.controller;

import com.MapMarket.application.configs.TestConfigs;
import com.MapMarket.application.integrationTests.testContainers.AbstractIntegrationTest;
import com.MapMarket.application.integrationTests.vo.AccountCredentialsVO;
import com.MapMarket.application.integrationTests.vo.TokenVO;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.LogDetail;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.context.SpringBootTest;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ProductControllerExceptionTest extends AbstractIntegrationTest {

  private static RequestSpecification specification;

  @Test
  @Order(0)
  public void authorization() {
    AccountCredentialsVO user = new AccountCredentialsVO("phyllipe", "admin123");

    var accessToken =
        given()
            .basePath("/auth/signin")
            .port(TestConfigs.SERVER_PORT)
            .contentType(TestConfigs.CONTENT_TYPE_JSON)
            .body(user)
            .when()
            .post()
            .then()
            .statusCode(200)
            .extract()
            .body()
            .as(TokenVO.class)
            .getAccessToken();

    specification = new RequestSpecBuilder()
        .addHeader(TestConfigs.HEADER_PARAM_AUTHORIZATION, "Bearer " + accessToken)
        .setBasePath("api/v1/produto")
        .setPort(TestConfigs.SERVER_PORT)
        .addFilter(new RequestLoggingFilter(LogDetail.ALL))
        .addFilter(new ResponseLoggingFilter(LogDetail.ALL))
        .build();
  }

  @Test
  @Order(1)
  public void test_create_WITH_PARAMETER_name_NULL() {
    //GIVEN
    String payloadNomeAusente = "{\"preco\": \"14.20\"}";

    //WHEN
    var content =
        given()
            .spec(specification)
            .contentType(TestConfigs.CONTENT_TYPE_JSON)
            .body(payloadNomeAusente)
            .when()
            .post()
            .then()
            .statusCode(400)
            .extract()
            .body()
            .asString();

    //THEN
    assertNotNull(content);
    assertTrue(content.contains("Required parameter 'nome' is null or blank!"));
  }

  @Test
  @Order(2)
  public void test_create_WITH_PARAMETER_price_NULL() {
    //GIVEN
    String payloadPrecoAusente = "{\"nome\": \"Lentilha\"}";

    //WHEN
    var content =
        given()
            .spec(specification)
            .contentType(TestConfigs.CONTENT_TYPE_JSON)
            .body(payloadPrecoAusente)
            .when()
            .post()
            .then()
            .statusCode(400)
            .extract()
            .body()
            .asString();

    //THEN
    assertNotNull(content);
    assertTrue(content.contains("Required parameter 'preco' is null or blank!"));
  }

  @Test
  @Order(3)
  public void test_create_WITH_PARAMETER_name_BLANK() {
    //GIVEN
    String payloadNomeEmBranco = "{\"nome\": \"\", \"preco\": \"14.20\"}";

    //WHEN
    var content =
        given()
            .spec(specification)
            .contentType(TestConfigs.CONTENT_TYPE_JSON)
            .body(payloadNomeEmBranco)
            .when()
            .post()
            .then()
            .statusCode(400)
            .extract()
            .body()
            .asString();

    //THEN
    assertNotNull(content);
    assertTrue(content.contains("Required parameter 'nome' is null or blank!"));
  }

  @Test
  @Order(4)
  public void test_create_WITH_PARAMETER_price_BLANK() {
    //GIVEN
    String payloadPrecoEmBranco = "{\"nome\": \"Lentilha\", \"preco\": \"\"}";

    //WHEN
    var content =
        given()
            .spec(specification)
            .contentType(TestConfigs.CONTENT_TYPE_JSON)
            .body(payloadPrecoEmBranco)
            .when()
            .post()
            .then()
            .statusCode(400)
            .extract()
            .body()
            .asString();

    //THEN
    assertNotNull(content);
    assertTrue(content.contains("Required parameter 'preco' is null or blank!"));
  }

  @Test
  @Order(5)
  public void test_create_WITH_NULL_REQUEST() {
    //GIVEN
    //WHEN
    var content =
        given()
            .spec(specification)
            .contentType(TestConfigs.CONTENT_TYPE_JSON)
            .body("")
            .when()
            .post()
            .then()
            .statusCode(400)
            .extract()
            .body()
            .asString();

    //THEN
    assertNotNull(content);
    assertTrue(content.contains("Failed to read request"));
  }

  @Test
  @Order(6)
  public void test_findById_NOT_FOUND() {
    //WHEN
    var content =
        given().spec(specification)
            .contentType(TestConfigs.CONTENT_TYPE_JSON)
            .pathParam("id", 85)
            .when()
            .get("{id}")
            .then()
            .statusCode(404)
            .extract()
            .body()
            .asString();

    //THEN
    assertNotNull(content);
    assertTrue(content.contains("Product not found with id 85"));
  }

  @Test
  @Order(7)
  public void test_findAll_WITHOUT_TOKEN() {
    RequestSpecification specificationWithoutToken = new RequestSpecBuilder()
        .setBasePath("api/v1/produto")
        .setPort(TestConfigs.SERVER_PORT)
        .addFilter(new RequestLoggingFilter(LogDetail.ALL))
        .addFilter(new ResponseLoggingFilter(LogDetail.ALL))
        .build();

    given()
        .spec(specificationWithoutToken)
        .contentType(TestConfigs.CONTENT_TYPE_JSON)
        .when()
        .get()
        .then()
        .statusCode(403);
  }
}