/*
 * Copyright (c) 2016-2018 "Neo4j, Inc." [https://neo4j.com]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Attribution Notice under the terms of the Apache License 2.0
 *
 * This work was created by the collective efforts of the openCypher community.
 * Without limiting the terms of Section 6, any Derivative Work that is not
 * approved by the public consensus process of the openCypher Implementers Group
 * should not be described as “Cypher” (and Cypher® is a registered trademark of
 * Neo4j Inc.) or as "openCypher". Extensions by implementers or prototypes or
 * proposals for change that have been documented or implemented should only be
 * described as "implementation extensions to Cypher" or as "proposed changes to
 * Cypher that are not yet approved by the openCypher community".
 */
package org.opencypher.okapi.ir.impl

import org.opencypher.okapi.api.graph.{Namespace, PropertyGraph, QualifiedGraphName}
import org.opencypher.okapi.api.io.PropertyGraphDataSource
import org.opencypher.okapi.api.schema.Schema

case class QueryCatalog(dataSourceMapping: Map[Namespace, PropertyGraphDataSource], registeredSchemas: Map[QualifiedGraphName, Schema]) {
  def schema(qgn: QualifiedGraphName): Schema = {
    registeredSchemas.getOrElse(
      qgn,
      schemaFromDataSource(qgn)
    )
  }

  def graph(qgn: QualifiedGraphName): PropertyGraph = dataSourceMapping(qgn.namespace).graph(qgn.graphName)

  private def schemaFromDataSource(qgn: QualifiedGraphName): Schema = {
    val dataSource = dataSourceMapping(qgn.namespace)
    val graphName = qgn.graphName
    val schema: Schema = dataSource.schema(graphName) match {
      case Some(s) => s
      case None => dataSource.graph(graphName).schema
    }
    schema
  }

  def withSchema(qgn: QualifiedGraphName, schema: Schema): QueryCatalog = {
    copy(registeredSchemas = registeredSchemas.updated(qgn, schema))
  }
}

object QueryCatalog {
  def apply(dataSourceMapping:  Map[Namespace, PropertyGraphDataSource]): QueryCatalog =
    QueryCatalog(dataSourceMapping, Map.empty)
}
