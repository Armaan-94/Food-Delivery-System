package com.fooddelivery.configurationserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

@EnableConfigServer
@SpringBootApplication
public class ConfigurationserverApplication 
{    
    public static void main(String[] args) {

        SpringApplication app = new SpringApplication(ConfigurationserverApplication.class);
        
        
        String mode = System.getenv("CONFIG_MODE");
        if ("native".equalsIgnoreCase(mode)) {
            app.setAdditionalProfiles("native");
        } else {
            app.setAdditionalProfiles("git");
        }
        app.setAdditionalProfiles("native");
        app.run(args);
    }
}