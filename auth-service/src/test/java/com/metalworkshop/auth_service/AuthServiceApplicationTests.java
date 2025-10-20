package com.metalworkshop.auth_service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest
@TestPropertySource(properties = {
        "jwt.secret=uYcu8cYldI1Xo8C7sqREpGjy4HQlIu86kzaCiXfHpI2RIOW5knSgOevflPqSMamI0nf4tnAF" +
                "w8IXzXtkK3ZsqhXD5z8UJUNn4KAnbTJSHa8rUnkF9mRmKkkK8kpjycynaWboovfuf02BW4ySZbI" +
                "jcpgI7SoZ42ybzBpnLyIXVGqo2kJF3EArbWiaq9kPxrpepSx3GVd651ZMAWhkDiglVfrjPsvwJ8" +
                "Bjb6voi8ZeyrxhLh3IHKfOokzFUN8bgXn9"
})
class AuthServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
