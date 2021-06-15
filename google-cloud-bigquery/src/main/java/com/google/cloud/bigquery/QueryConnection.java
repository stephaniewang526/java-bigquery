/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.bigquery;

import com.google.api.services.bigquery.model.QueryParameter;
import java.sql.ResultSet;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * A Connection is a session between a Java application and BigQuery. SQL statements are executed
 * and results are returned within the context of a connection.
 */
public interface QueryConnection {

  /** Sets the timeout for query request */
  void setSynchronousResponseTimeout(Long timeout);

  /** Returns the timeout associated with this query */
  Long getSynchronousResponseTimeout();

  /**
   * Sets a connection-level property to customize query behavior. Under JDBC, these correspond
   * directly to connection properties passed to the DriverManager.
   *
   * @param connectionProperties connectionProperties or {@code null} for none
   */
  void setConnectionProperties(List<ConnectionProperty> connectionProperties);

  /** Returns the connection properties for connection string with this query */
  List<ConnectionProperty> getConnectionProperties();

  /**
   * Sets the default dataset. This dataset is used for all unqualified table names used in the
   * query.
   */
  void setDefaultDataset();

  /** Returns the default dataset */
  DatasetId getDefaultDataset();

  /**
   * Sets whether the query has to be dry run or not. If set, the query is not executed. A valid
   * query will return a mostly empty response with some processing statistics, while an invalid
   * query will return the same error it would if it wasn't a dry run.
   */
  void setDryRun();

  /** Returns whether the query has to be dry run or not */
  Boolean getDryRun();

  /**
   * Sets the labels associated with this query. You can use these to organize and group your
   * queries. Label keys and values can be no longer than 63 characters, can only contain lowercase
   * letters, numeric characters, underscores and dashes. International characters are allowed.
   * Label values are optional. Label keys must start with a letter and each label in the list must
   * have a different key.
   *
   * @param labels labels or {@code null} for none
   */
  void setLabels(Map<String, String> labels);

  /** Returns the labels associated with this query */
  Map<String, String> getLabels();

  /** Clear the labels associated with this query */
  void cleaLabels();

  /**
   * Limits the bytes billed for this job. Queries that will have bytes billed beyond this limit
   * will fail (without incurring a charge). If unspecified, this will be set to your project
   * default.
   *
   * @param maximumBytesBilled maximum bytes billed for this job
   */
  void setMaximumBytesBilled(Long maximumBytesBilled);

  /** Returns the limits the bytes billed for this job */
  Long getMaximumBytesBilled();

  /**
   * Sets the maximum number of rows of data to return per page of results. Setting this flag to a
   * small value such as 1000 and then paging through results might improve reliability when the
   * query result set is large. In addition to this limit, responses are also limited to 10 MB. By
   * default, there is no maximum row count, and only the byte limit applies.
   *
   * @param maxResults maxResults or {@code null} for none
   */
  void setMaxResults(Long maxResults);

  /** Returns the maximum number of rows of data */
  Long getMaxResults();

  /**
   * Sets query parameters for standard SQL queries.
   *
   * @param queryParameters queryParameters or {@code null} for none
   */
  void setQueryParameters(List<QueryParameter> queryParameters);

  /** Returns query parameters for standard SQL queries */
  List<QueryParameter> getQueryParameters();

  /**
   * Sets whether to look for the result in the query cache. The query cache is a best-effort cache
   * that will be flushed whenever tables in the query are modified. Moreover, the query cache is
   * only available when {@link QueryJobConfiguration.Builder#setDestinationTable(TableId)} is not
   * set.
   *
   * @see <a href="https://cloud.google.com/bigquery/querying-data#querycaching">Query Caching</a>
   */
  void setUseQueryCache(Boolean useQueryCache);

  /** Returns whether to look for the result in the query cache */
  Boolean getUseQueryCache();

  /**
   * Sets whether to use BigQuery's legacy SQL dialect for this query. By default this property is
   * set to {@code false}. If set to {@code false}, the query will use BigQuery's <a
   * href="https://cloud.google.com/bigquery/sql-reference/">Standard SQL</a>. When set to {@code
   * false}, the values of {@link #allowLargeResults()} and {@link #flattenResults()} are ignored;
   * query will be run as if {@link #allowLargeResults()} is {@code true} and {@link
   * #flattenResults()} is {@code false}. If set to {@code null} or {@code true}, legacy SQL dialect
   * is used. This property is experimental and might be subject to change.
   */
  void setUseLegacySql(Boolean useLegacySql);

  /** Returns whether to use BigQuery's legacy SQL dialect for this query */
  Boolean getUseLegacySql();

  /**
   * Sets the value of the client info property specified by name to the value specified by value.
   *
   * @param name The name of the client info property to set
   * @param value The value to set the client info property to. If the value is null, the current
   *     value of the specified property is cleared.
   * @exception SQLClientInfoException if the BigQuery server returns an error while setting the
   *     client info value on the BigQuery server or this method is called on a closed connection
   */
  void setClientInfo(String name, String value) throws SQLClientInfoException;

  /**
   * Returns the value of the client info property specified by name. This method may return null if
   * the specified client info property has not been set and does not have a default value. This
   * method will also return null if the specified client info property name is not supported.
   *
   * @param name The name of the client info property to retrieve
   * @exception SQLException if the BigQuery server returns an error when fetching the client info
   *     value from the BigQuery server or this method is called on a closed connection
   */
  String getClientInfo(String name) throws SQLException;

  /**
   * Execute a SQL statement that returns a single ResultSet
   *
   * @param sql typically a static SQL SELECT statement
   * @return a ResultSet that contains the data produced by the query
   * @exception BigQueryException if a database access error occurs
   */
  ResultSet executeSelect(String sql) throws BigQueryException;
}
