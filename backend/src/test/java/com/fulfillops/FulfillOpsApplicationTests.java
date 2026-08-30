package com.fulfillops;

import com.fulfillops.support.NoDatabaseTestApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = NoDatabaseTestApplication.class)
@ActiveProfiles("no-db")
class FulfillOpsApplicationTests {

    @Test
    void contextLoads() {
    }
}
