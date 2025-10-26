package com.metalworkshop.api_gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "auth.service.url="
})
class ApiGatewayApplicationTests {

	@Test
	void contextLoads() {
	}

}
