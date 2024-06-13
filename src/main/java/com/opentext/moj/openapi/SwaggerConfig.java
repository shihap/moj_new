package com.opentext.moj.openapi;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;

@OpenAPIDefinition(
        info = @Info(
                description = "MOJ RestAPI Swagger Documentation",
                title = "MOJ RestAPI",
                version = "1.0"
        ),
        servers = {
                @Server(
                        description = "Local Env",
                        url = "http://localhost:8080/MOJ"
                )
        }
)
public class SwaggerConfig {
}
