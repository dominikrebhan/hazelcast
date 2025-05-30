/*
 * Copyright (c) 2008-2025, Hazelcast, Inc. All Rights Reserved.
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

package com.hazelcast.jet.cdc.mysql;

import com.hazelcast.jet.cdc.AbstractCdcIntegrationTest;
import com.hazelcast.jet.cdc.mysql.MySqlCdcSources.Builder;
import com.hazelcast.jet.retry.RetryStrategies;
import com.hazelcast.jet.test.IgnoreInJenkinsOnWindows;
import com.hazelcast.test.HazelcastSerialClassRunner;
import com.hazelcast.test.annotation.ParallelJVMTest;
import org.junit.Rule;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.testcontainers.containers.MySQLContainer;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static com.hazelcast.jet.cdc.MySQLTestUtils.getMySqlConnection;
import static com.hazelcast.jet.TestedVersions.DEBEZIUM_MYSQL_IMAGE;
import static org.testcontainers.containers.MySQLContainer.MYSQL_PORT;

@Category({ParallelJVMTest.class, IgnoreInJenkinsOnWindows.class})
@RunWith(HazelcastSerialClassRunner.class)
public abstract class AbstractMySqlCdcIntegrationTest extends AbstractCdcIntegrationTest {
    @Rule
    @SuppressWarnings("resource")
    public MySQLContainer<?> mysql = namedTestContainer(
            new MySQLContainer<>(DEBEZIUM_MYSQL_IMAGE)
                    .withUsername("mysqluser")
                    .withPassword("mysqlpw")
    );

    protected Builder sourceBuilder(String name) {
        return MySqlCdcSources.mysql(name)
                .setDatabaseAddress(mysql.getHost())
                .setDatabasePort(mysql.getMappedPort(MYSQL_PORT))
                .setDatabaseUser("debezium")
                .setDatabasePassword("dbz")
                .setClusterName("dbserver1")
                .setReconnectBehavior(RetryStrategies.indefinitely(1000));
    }

    @SuppressWarnings("SqlSourceToSinkFlow")
    protected void createDb(String database) throws SQLException {
        String jdbcUrl = "jdbc:mysql://" + mysql.getHost() + ":" + mysql.getMappedPort(MYSQL_PORT) + "/";
        try (Connection connection = getMySqlConnection(jdbcUrl, "root", "mysqlpw")) {
            Statement statement = connection.createStatement();
            statement.addBatch("CREATE DATABASE " + database);
            statement.addBatch("GRANT ALL PRIVILEGES ON " + database + ".* TO 'mysqluser'@'%'");
            statement.executeBatch();
        }
    }

    static Connection getConnection(MySQLContainer<?> mysql, String database) throws SQLException {
        return getMySqlConnection(
                mysql.withDatabaseName(database).getJdbcUrl(),
                mysql.getUsername(),
                mysql.getPassword()
        );
    }

}
