package com.fulfillops.support;

import com.fulfillops.FulfillOpsApplication;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(classes = FulfillOpsApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractPostgresApplicationIntegrationTest {

    private static final PostgresDatabaseCleaner DATABASE_CLEANER = new PostgresDatabaseCleaner();

    @Autowired
    private DataSource sharedApplicationDataSource;

    @DynamicPropertySource
    static void configureSharedApplicationDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", SharedApplicationPostgres::jdbcUrl);
        registry.add("spring.datasource.username", SharedApplicationPostgres::username);
        registry.add("spring.datasource.password", SharedApplicationPostgres::password);
    }

    @BeforeEach
    final void cleanSharedApplicationDatabase() {
        DATABASE_CLEANER.clean(sharedApplicationDataSource);
    }
}
