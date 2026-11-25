package com.example.student_management;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/** Starts the whole app against a throwaway Postgres: Flyway migrates, then Hibernate validates the schema. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class StudentManagementApplicationTests {

	@Test
	void contextLoads() {
	}

}
