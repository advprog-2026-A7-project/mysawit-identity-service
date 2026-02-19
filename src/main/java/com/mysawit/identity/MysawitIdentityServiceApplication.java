package com.mysawit.identity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class MysawitIdentityServiceApplication {
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(MysawitIdentityServiceApplication.class, args);
        if (context.getEnvironment().getProperty("app.test.close-context", Boolean.class, false)) {
            context.close();
        }
    }
}
