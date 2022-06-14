package com.salaboy.conferences.c4p.dev;

import com.salaboy.conferences.c4p.config.ServiceConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.reactive.function.client.WebClient;

@Profile("dev")
//@EnableAutoConfiguration(exclude = {
//        SecurityAutoConfiguration.class, TracerAutoConfiguration.class,
//        ReactiveManagementWebSecurityAutoConfiguration.class})
@Configuration
public class DevProfile {

    @Bean
    public WebClient getWebClient() {
        return WebClient.builder().build();
    }

//    @Bean
//    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
//        return http.csrf().disable()
//                .authorizeExchange()
//                .anyExchange().permitAll()
//                .and()
//                .build();
//    }
}
