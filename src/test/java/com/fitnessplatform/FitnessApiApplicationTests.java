package com.fitnessplatform;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class FitnessApiApplicationTests {

	@Autowired
	private DataSource dataSource;

	@Test
	void contextLoads() {
	}

	@Test
	void connectsToPostgreSQL() throws SQLException {
		try (Connection connection = dataSource.getConnection()) {
			assertTrue(connection.isValid(2));
			assertEquals(
					"PostgreSQL",
					connection.getMetaData().getDatabaseProductName()
			);
		}
	}
}
