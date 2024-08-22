package io.delta.kernel.types

import org.scalatest.funsuite.AnyFunSuite

class StructTypeSuite extends AnyFunSuite {

  test("check toJson method with primitive types") {
    val structType = new StructType()
      .add("c1", BinaryType.BINARY, true)
      .add("c2", BooleanType.BOOLEAN, false)
      .add("c3", ByteType.BYTE, false)
      .add("c4", DateType.DATE, true)
      .add("c5", DecimalType.USER_DEFAULT, false)
      .add("c6", DoubleType.DOUBLE, false)
      .add("c7", FloatType.FLOAT, false)
      .add("c8", IntegerType.INTEGER, true)
      .add("c9", LongType.LONG, true)
      .add("c10", ShortType.SHORT, true)
      .add("c11", StringType.STRING, true)
      .add("c12", TimestampNTZType.TIMESTAMP_NTZ, false)
      .add("c13", TimestampType.TIMESTAMP, false)
      .add("c14", VariantType.VARIANT, false)
    val toJson =
      """{
        |  "type" : "struct",
        |  "fields" : [ {
        |  "name" : "c1",
        |  "type" : "binary",
        |  "nullable" : true,
        |  "metadata" : {}
        |},
        |{
        |  "name" : "c2",
        |  "type" : "boolean",
        |  "nullable" : false,
        |  "metadata" : {}
        |},
        |{
        |  "name" : "c3",
        |  "type" : "byte",
        |  "nullable" : false,
        |  "metadata" : {}
        |},
        |{
        |  "name" : "c4",
        |  "type" : "date",
        |  "nullable" : true,
        |  "metadata" : {}
        |},
        |{
        |  "name" : "c5",
        |  "type" : "decimal(10, 0)",
        |  "nullable" : false,
        |  "metadata" : {}
        |},
        |{
        |  "name" : "c6",
        |  "type" : "double",
        |  "nullable" : false,
        |  "metadata" : {}
        |},
        |{
        |  "name" : "c7",
        |  "type" : "float",
        |  "nullable" : false,
        |  "metadata" : {}
        |},
        |{
        |  "name" : "c8",
        |  "type" : "integer",
        |  "nullable" : true,
        |  "metadata" : {}
        |},
        |{
        |  "name" : "c9",
        |  "type" : "long",
        |  "nullable" : true,
        |  "metadata" : {}
        |},
        |{
        |  "name" : "c10",
        |  "type" : "short",
        |  "nullable" : true,
        |  "metadata" : {}
        |},
        |{
        |  "name" : "c11",
        |  "type" : "string",
        |  "nullable" : true,
        |  "metadata" : {}
        |},
        |{
        |  "name" : "c12",
        |  "type" : "timestamp_ntz",
        |  "nullable" : false,
        |  "metadata" : {}
        |},
        |{
        |  "name" : "c13",
        |  "type" : "timestamp",
        |  "nullable" : false,
        |  "metadata" : {}
        |},
        |{
        |  "name" : "c14",
        |  "type" : "variant",
        |  "nullable" : false,
        |  "metadata" : {}
        |} ]
        |}""".stripMargin

    assert(structType.toJson == toJson)
  }

  test("check toJson method with complex types") {
    val structType = new StructType()
      .add("a1", StringType.STRING, true)
      .add("a2", new StructType()
        .add("b1", new MapType(
          new ArrayType(
            new ArrayType(StringType.STRING, true), true),
          new StructType()
            .add("c1", StringType.STRING, false)
            .add("c2", StringType.STRING, true), true))
        .add("b2", LongType.LONG), true)
      .add("a3", new ArrayType(
        new MapType(
          StringType.STRING,
          new StructType()
            .add("b1", DateType.DATE, false), false), false), true)
    val toJson =
      """{
        |  "type" : "struct",
        |  "fields" : [ {
        |  "name" : "a1",
        |  "type" : "string",
        |  "nullable" : true,
        |  "metadata" : {}
        |},
        |{
        |  "name" : "a2",
        |  "type" : {
        |  "type" : "struct",
        |  "fields" : [ {
        |  "name" : "b1",
        |  "type" : {"type": "map","keyType": {"type": "array","elementType": {"type": "array","elementType": "string","containsNull": true},"containsNull": true},"valueType": {
        |  "type" : "struct",
        |  "fields" : [ {
        |  "name" : "c1",
        |  "type" : "string",
        |  "nullable" : false,
        |  "metadata" : {}
        |},
        |{
        |  "name" : "c2",
        |  "type" : "string",
        |  "nullable" : true,
        |  "metadata" : {}
        |} ]
        |},"valueContainsNull": true},
        |  "nullable" : true,
        |  "metadata" : {}
        |},
        |{
        |  "name" : "b2",
        |  "type" : "long",
        |  "nullable" : true,
        |  "metadata" : {}
        |} ]
        |},
        |  "nullable" : true,
        |  "metadata" : {}
        |},
        |{
        |  "name" : "a3",
        |  "type" : {"type": "array","elementType": {"type": "map","keyType": "string","valueType": {
        |  "type" : "struct",
        |  "fields" : [ {
        |  "name" : "b1",
        |  "type" : "date",
        |  "nullable" : false,
        |  "metadata" : {}
        |} ]
        |},"valueContainsNull": false},"containsNull": false},
        |  "nullable" : true,
        |  "metadata" : {}
        |} ]
        |}""".stripMargin

    assert(structType.toJson == toJson)
  }

  test("check toJson method with complex types and collated strings") {
    val structType = new StructType()
      .add("a1", StringType.STRING, true)
      .add("a2", new StructType()
        .add("b1", new MapType(
          new ArrayType(
            new ArrayType(
              new StringType("UTF8_LCASE"), true), true),
          new StructType()
            .add("c1", new StringType("UTF8_LCASE"), false)
            .add("c2", new StringType("UNICODE"), true)
            .add("c3", StringType.STRING), true))
        .add("b2", LongType.LONG), true)
      .add("a3", new ArrayType(
        new MapType(
          new StringType("UNICODE_CI"),
          new StructType()
            .add("b1", new StringType("UTF8_LCASE"), false), false), false), true)
    val toJson =
      """{
        |  "type" : "struct",
        |  "fields" : [ {
        |  "name" : "a1",
        |  "type" : "string",
        |  "nullable" : true,
        |  "metadata" : {}
        |},
        |{
        |  "name" : "a2",
        |  "type" : {
        |  "type" : "struct",
        |  "fields" : [ {
        |  "name" : "b1",
        |  "type" : {"type": "map","keyType": {"type": "array","elementType": {"type": "array","elementType": "string","containsNull": true},"containsNull": true},"valueType": {
        |  "type" : "struct",
        |  "fields" : [ {
        |  "name" : "c1",
        |  "type" : "string",
        |  "nullable" : false,
        |  "metadata" : {"__COLLATIONS" : {"c1" : "UTF8_LCASE"}}
        |},
        |{
        |  "name" : "c2",
        |  "type" : "string",
        |  "nullable" : true,
        |  "metadata" : {"__COLLATIONS" : {"c2" : "UNICODE"}}
        |},
        |{
        |  "name" : "c3",
        |  "type" : "string",
        |  "nullable" : true,
        |  "metadata" : {}
        |} ]
        |},"valueContainsNull": true},
        |  "nullable" : true,
        |  "metadata" : {"__COLLATIONS" : {"b1.key.element.element" : "UTF8_LCASE"}}
        |},
        |{
        |  "name" : "b2",
        |  "type" : "long",
        |  "nullable" : true,
        |  "metadata" : {}
        |} ]
        |},
        |  "nullable" : true,
        |  "metadata" : {}
        |},
        |{
        |  "name" : "a3",
        |  "type" : {"type": "array","elementType": {"type": "map","keyType": "string","valueType": {
        |  "type" : "struct",
        |  "fields" : [ {
        |  "name" : "b1",
        |  "type" : "string",
        |  "nullable" : false,
        |  "metadata" : {"__COLLATIONS" : {"b1" : "UTF8_LCASE"}}
        |} ]
        |},"valueContainsNull": false},"containsNull": false},
        |  "nullable" : true,
        |  "metadata" : {"__COLLATIONS" : {"a3.element.key" : "UNICODE_CI"}}
        |} ]
        |}""".stripMargin

    assert(structType.toJson == toJson)
  }
}
