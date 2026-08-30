package com.fulfillops.support;

import com.fulfillops.common.api.GlobalExceptionHandler;
import com.fulfillops.common.web.RequestIdFilter;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

@SpringBootConfiguration(proxyBeanMethods = false)
@EnableAutoConfiguration
@ComponentScan(basePackageClasses = {
        GlobalExceptionHandler.class,
        RequestIdFilter.class
})
public class NoDatabaseTestApplication {
}
