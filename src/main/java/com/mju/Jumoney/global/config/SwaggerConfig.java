package com.mju.Jumoney.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem.HttpMethod;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        // Security Scheme 설정
        SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("발급받은 Access Token을 입력해주세요.");

        // 전역 보안 요구사항 설정
        SecurityRequirement securityRequirement = new SecurityRequirement().addList("bearerAuth");

        return new OpenAPI()
                .components(new Components().addSecuritySchemes("bearerAuth", securityScheme))
                .security(List.of(securityRequirement))
                .info(apiInfo());
    }

    @Bean
    public OpenApiCustomizer dynamicDateExamplesOpenApiCustomizer() {
        return openApi -> {
            LocalDate today = LocalDate.now();
            LocalDate yesterday = today.minusDays(1);

            applyQueryParameterExample(openApi, "/api/local/kis/smoke", HttpMethod.GET, "baseDate", yesterday);
            applyQueryParameterExample(openApi, "/api/local/kis/smoke", HttpMethod.GET, "dividendFrom", today.minusYears(1));
            applyQueryParameterExample(openApi, "/api/local/kis/smoke", HttpMethod.GET, "dividendTo", today);
            applyQueryParameterExample(openApi, "/api/local/kis/batch/hts-conditions", HttpMethod.POST, "baseDate", yesterday);
            applyQueryParameterExample(openApi, "/api/local/kis/batch/stock-indicators", HttpMethod.POST, "baseDate", yesterday);
            applyQueryParameterExample(openApi, "/api/local/kis/batch/stock-indicators/status", HttpMethod.GET, "baseDate", yesterday);
        };
    }

    private void applyQueryParameterExample(
            OpenAPI openApi,
            String path,
            HttpMethod method,
            String parameterName,
            LocalDate example
    ) {
        if (openApi.getPaths() == null || openApi.getPaths().get(path) == null) {
            return;
        }
        var operation = openApi.getPaths().get(path).readOperationsMap().get(method);
        if (operation == null || operation.getParameters() == null) {
            return;
        }
        operation.getParameters().stream()
                .filter(parameter -> parameterName.equals(parameter.getName()))
                .filter(parameter -> "query".equals(parameter.getIn()))
                .findFirst()
                .ifPresent(parameter -> parameter.setExample(example.toString()));
    }

    private Info apiInfo() {
        return new Info()
                .title("주머니(Jumoney) API 문서")
                .description("주머니의 백엔드 API 명세서입니다.")
                .version("1.0.0");
    }
}
