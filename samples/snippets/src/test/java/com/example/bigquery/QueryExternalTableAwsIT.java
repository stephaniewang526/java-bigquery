/*
 * Copyright 2020 Google LLC
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

package com.example.bigquery;

import static com.google.common.truth.Truth.assertThat;
import static junit.framework.TestCase.assertNotNull;

import com.google.cloud.bigquery.CsvOptions;
import com.google.cloud.bigquery.ExternalTableDefinition;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.StandardSQLTypeName;
import com.google.cloud.bigquery.connection.v1.AwsCrossAccountRole;
import com.google.cloud.bigquery.connection.v1.AwsProperties;
import com.google.cloud.bigquery.connection.v1.Connection;
import com.google.cloud.bigquery.connection.v1.CreateConnectionRequest;
import com.google.cloud.bigquery.connection.v1.DeleteConnectionRequest;
import com.google.cloud.bigquery.connection.v1.LocationName;
import com.google.cloud.bigqueryconnection.v1.ConnectionServiceClient;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class QueryExternalTableAwsIT {

  private static final String ID = UUID.randomUUID().toString().substring(0, 8);
  private static final String LOCATION = "aws-us-east-1";
  private final Logger log = Logger.getLogger(this.getClass().getName());
  private String datasetName;
  private String tableName;
  private String connectionName;
  private ByteArrayOutputStream bout;
  private PrintStream out;
  private PrintStream originalPrintStream;

  private static final String PROJECT_ID = requireEnvVar("OMNI_PROJECT_ID");
  private static final String AWS_ACCOUNT_ID = requireEnvVar("AWS_ACCOUNT_ID");
  private static final String AWS_ROLE_ID = requireEnvVar("AWS_ROLE_ID");

  private static String requireEnvVar(String varName) {
    String value = System.getenv(varName);
    assertNotNull(
        "Environment variable " + varName + " is required to perform these tests.",
        System.getenv(varName));
    return value;
  }

  @BeforeClass
  public static void checkRequirements() {
    requireEnvVar("OMNI_PROJECT_ID");
    requireEnvVar("AWS_ACCOUNT_ID");
    requireEnvVar("AWS_ROLE_ID");
  }

  @Before
  public void setUp() throws IOException {
    datasetName = "QUERY_EXTERNAL_TABLE_AWS_TEST_" + ID;
    tableName = "QUERY_EXTERNAL_TABLE_AWS_TEST_" + ID;
    connectionName = "QUERY_EXTERNAL_TABLE_AWS_TEST_" + ID;
    bout = new ByteArrayOutputStream();
    out = new PrintStream(bout);
    originalPrintStream = System.out;
    System.setOut(out);
    // create a temporary aws connection
    try (ConnectionServiceClient client = ConnectionServiceClient.create()) {
      LocationName parent = LocationName.of(PROJECT_ID, LOCATION);
      String iamRoleId = String.format("arn:aws:iam::%s:role/%s", AWS_ACCOUNT_ID, AWS_ROLE_ID);
      AwsCrossAccountRole role = AwsCrossAccountRole.newBuilder().setIamRoleId(iamRoleId).build();
      AwsProperties awsProperties = AwsProperties.newBuilder().setCrossAccountRole(role).build();
      Connection connection = Connection.newBuilder().setAws(awsProperties).build();
      CreateConnectionRequest request =
          CreateConnectionRequest.newBuilder()
              .setParent(parent.toString())
              .setConnection(connection)
              .setConnectionId(connectionName)
              .build();
      Connection response = client.createConnection(request);
      connectionName = response.getName();
      AwsCrossAccountRole accountRole = response.getAws().getCrossAccountRole();
      System.out.println(
          "Aws connection created successfully : Aws userId :"
              + accountRole.getIamUserId()
              + " Aws externalId :"
              + accountRole.getExternalId());
    }
    // create a temporary dataset
    CreateDatasetAws.createDatasetAws(PROJECT_ID, datasetName, LOCATION);
  }

  @After
  public void tearDown() throws IOException {
    // delete a temporary aws connection
    try (ConnectionServiceClient client = ConnectionServiceClient.create()) {
      DeleteConnectionRequest request =
          DeleteConnectionRequest.newBuilder().setName(connectionName).build();
      client.deleteConnection(request);
      System.out.println("Connection deleted successfully");
    }
    // Clean up
    DeleteTable.deleteTable(datasetName, tableName);
    DeleteDataset.deleteDataset(PROJECT_ID, datasetName);
    // restores print statements in the original method
    System.out.flush();
    System.setOut(originalPrintStream);
    log.log(Level.INFO, bout.toString());
  }

  @Test
  public void testQueryExternalTableAws() {
    String sourceUri = "s3://cloud-samples-tests/us-states.csv";
    Schema schema =
        Schema.of(
            Field.of("name", StandardSQLTypeName.STRING),
            Field.of("post_abbr", StandardSQLTypeName.STRING));
    CsvOptions options = CsvOptions.newBuilder().setSkipLeadingRows(1).build();
    ExternalTableDefinition externalTable =
        ExternalTableDefinition.newBuilder(sourceUri, options)
            .setConnectionId(connectionName)
            .setSchema(schema)
            .build();
    String query =
        String.format(
            "SELECT * FROM %s:%s.%s WHERE name LIKE 'W%%'", PROJECT_ID, datasetName, tableName);
    QueryExternalTableAws.queryExternalTableAws(
        PROJECT_ID, datasetName, tableName, externalTable, query);
    assertThat(bout.toString())
        .contains("Query on aws external permanent table performed successfully.");
  }
}
