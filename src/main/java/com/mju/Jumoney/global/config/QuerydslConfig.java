package com.mju.Jumoney.global.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class QuerydslConfig {

    // EntityManager를 영속성 컨텍스트에서 주입
    private final EntityManager entityManager;

    // JPAQueryFactory를 빈으로 등록 (리포지토리에서 의존성 주입하여 사용)
    @Bean
    public JPAQueryFactory jpaQueryFactory() {
        return new JPAQueryFactory(entityManager);
    }
}