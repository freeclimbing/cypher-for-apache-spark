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
package org.opencypher.okapi.api.graph

import org.opencypher.okapi.api.io._
import org.opencypher.okapi.api.table.CypherRecords
import org.opencypher.okapi.api.value.CypherValue._
import org.opencypher.okapi.impl.exception.{IllegalArgumentException, UnsupportedOperationException}
import org.opencypher.okapi.impl.io.SessionGraphDataSource
import org.opencypher.okapi.impl.io.SessionGraphDataSource.{Namespace => SessionNamespace}

/**
  * The Cypher Session is the main API for a Cypher-based application. It manages graphs which can be queried using
  * Cypher. Graphs can be read from / written to different data sources (e.g. CSV) and also stored in / retrieved from
  * the session-local storage.
  */
trait CypherSession {

  /**
    * The [[org.opencypher.okapi.api.graph.Namespace]] used to to store graphs within this session.
    *
    * @return session namespace
    */
  def sessionNamespace: Namespace

  /**
    * Stores a mutable mapping between a data source [[org.opencypher.okapi.api.graph.Namespace]] and the specific [[org.opencypher.okapi.api.io.PropertyGraphDataSource]].
    *
    * This mapping also holds the [[org.opencypher.okapi.impl.io.SessionGraphDataSource]] by default.
    */
  protected var dataSourceMapping: Map[Namespace, PropertyGraphDataSource] =
    Map(sessionNamespace -> new SessionGraphDataSource)

  /**
    * Register the given [[org.opencypher.okapi.api.io.PropertyGraphDataSource]] under the specific [[org.opencypher.okapi.api.graph.Namespace]] within this session.
    *
    * This enables a user to refer to that [[org.opencypher.okapi.api.io.PropertyGraphDataSource]] within a Cypher query.
    *
    * Note, that it is not allowed to overwrite an already registered [[org.opencypher.okapi.api.graph.Namespace]].
    * Use [[CypherSession#deregisterSource]] first.
    *
    * @param namespace  namespace for lookup
    * @param dataSource property graph data source
    */
  def registerSource(namespace: Namespace, dataSource: PropertyGraphDataSource): Unit = dataSourceMapping.get(namespace) match {
    case Some(p) => throw IllegalArgumentException(s"no data source registered with namespace '$namespace'", p)
    case None => dataSourceMapping = dataSourceMapping.updated(namespace, dataSource)
  }

  /**
    * De-registers a [[org.opencypher.okapi.api.io.PropertyGraphDataSource]] from the session by its given [[org.opencypher.okapi.api.graph.Namespace]].
    *
    * @param namespace namespace for lookup
    */
  def deregisterSource(namespace: Namespace): Unit = {
    if (namespace == sessionNamespace) throw UnsupportedOperationException("de-registering the session data source")
    dataSourceMapping.get(namespace) match {
      case Some(_) => dataSourceMapping = dataSourceMapping - namespace
      case None => throw IllegalArgumentException(s"a data source registered with namespace '$namespace'")
    }
  }

  /**
    * Returns all [[org.opencypher.okapi.api.graph.Namespace]]s registered at this session.
    *
    * @return registered namespaces
    */
  def namespaces: Set[Namespace] = dataSourceMapping.keySet

  /**
    * Returns the [[org.opencypher.okapi.api.io.PropertyGraphDataSource]] that is registered under the given [[org.opencypher.okapi.api.graph.Namespace]].
    *
    * @param namespace namespace for lookup
    * @return property graph data source
    */
  def dataSource(namespace: Namespace): PropertyGraphDataSource = dataSourceMapping.getOrElse(namespace,
    throw IllegalArgumentException(s"a data source registered with namespace '$namespace'"))

  /**
    * Executes a Cypher query in this session on the current ambient graph.
    *
    * @param query      Cypher query to execute
    * @param parameters parameters used by the Cypher query
    * @return result of the query
    */
  def cypher(query: String, parameters: CypherMap = CypherMap.empty, drivingTable: Option[CypherRecords] = None): CypherResult

  /**
    * Stores the given [[org.opencypher.okapi.api.graph.PropertyGraph]] using the [[org.opencypher.okapi.api.io.PropertyGraphDataSource]] registered under the [[org.opencypher.okapi.api.graph.Namespace]] of the specified string representation of a [[org.opencypher.okapi.api.graph.QualifiedGraphName]].
    *
    * If the given qualified graph name does not contain any period character (`.`),
    * the default [[org.opencypher.okapi.impl.io.SessionGraphDataSource]] will be used.
    *
    * @param qualifiedGraphName qualified graph name
    * @param graph              property graph to store
    */
  def store(qualifiedGraphName: String, graph: PropertyGraph): Unit = {
    store(QualifiedGraphName(qualifiedGraphName), graph)
  }

  /**
    * Stores the given [[org.opencypher.okapi.api.graph.PropertyGraph]] using the [[org.opencypher.okapi.api.io.PropertyGraphDataSource]] registered under the [[org.opencypher.okapi.api.graph.Namespace]] of the
    * specified [[org.opencypher.okapi.api.graph.QualifiedGraphName]].
    *
    * @param qualifiedGraphName qualified graph name
    * @param graph              property graph to store
    */
  def store(qualifiedGraphName: QualifiedGraphName, graph: PropertyGraph): Unit =
    dataSource(qualifiedGraphName.namespace).store(qualifiedGraphName.graphName, graph)

  /**
    * Removes the [[org.opencypher.okapi.api.graph.PropertyGraph]] associated with the given qualified graph name.
    *
    * If the given qualified graph name does not contain any period character (`.`),
    * the default [[org.opencypher.okapi.impl.io.SessionGraphDataSource]] will be used.
    *
    * @param qualifiedGraphName name of the graph within the session.
    */
  def delete(qualifiedGraphName: String): Unit =
    delete(QualifiedGraphName(qualifiedGraphName))

  /**
    * Removes the [[org.opencypher.okapi.api.graph.PropertyGraph]] with the given qualified name from the data source associated with the specified
    * [[org.opencypher.okapi.api.graph.Namespace]].
    *
    * @param qualifiedGraphName qualified graph name
    */
  def delete(qualifiedGraphName: QualifiedGraphName): Unit =
    dataSource(qualifiedGraphName.namespace).delete(qualifiedGraphName.graphName)

  /**
    * Returns the [[org.opencypher.okapi.api.graph.PropertyGraph]] that is stored under the given string representation of a [[org.opencypher.okapi.api.graph.QualifiedGraphName]].
    *
    * If the given qualified graph name does not contain any period character (`.`),
    * the default [[org.opencypher.okapi.impl.io.SessionGraphDataSource]] will be used.
    *
    * @param qualifiedGraphName qualified graph name
    * @return property graph
    */
  def graph(qualifiedGraphName: String): PropertyGraph =
    graph(QualifiedGraphName(qualifiedGraphName))

  /**
    * Returns the [[org.opencypher.okapi.api.graph.PropertyGraph]] that is stored under the given [[org.opencypher.okapi.api.graph.QualifiedGraphName]].
    *
    * @param qualifiedGraphName qualified graph name
    * @return property graph
    */
  def graph(qualifiedGraphName: QualifiedGraphName): PropertyGraph =
    dataSource(qualifiedGraphName.namespace).graph(qualifiedGraphName.graphName)

  /**
    * Executes a Cypher query in this session, using the argument graph as the ambient graph.
    *
    * The ambient graph is the graph that is used for graph matching and updating,
    * unless another graph is explicitly selected by the query.
    *
    * @param graph      ambient graph for this query
    * @param query      Cypher query to execute
    * @param parameters parameters used by the Cypher query
    * @return result of the query
    */
  private[graph] def cypherOnGraph(
    graph: PropertyGraph,
    query: String,
    parameters: CypherMap = CypherMap.empty,
    drivingTable: Option[CypherRecords]): CypherResult
}
