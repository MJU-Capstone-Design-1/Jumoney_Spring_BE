package com.mju.Jumoney.global.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;

// WebClient 설정 클래스 (외부 API 연동)
@Slf4j
@Configuration
public class WebClientConfig {

    @Value("${kis.url}")
    private String kisUrl;

    @Value("${kis.client.connect-timeout-millis}")
    private int connectTimeoutMillis;

    @Value("${kis.client.read-timeout-seconds}")
    private int readTimeoutSeconds;

    @Value("${kis.client.write-timeout-seconds}")
    private int writeTimeoutSeconds;

    @Value("${kis.client.response-timeout-seconds}")
    private long responseTimeoutSeconds;

    @Value("${kis.client.max-in-memory-size}")
    private int maxInMemorySize;

    @Value("${kis.client.pool.max-connections}")
    private int poolMaxConnections;

    @Value("${kis.client.pool.max-idle-seconds}")
    private long poolMaxIdleSeconds;

    // 커넥션 풀 설정 빈
    // @destroyMethod = "dispose": 서버 종료 시 스프링이 안전하게 자원을 반환하도록 처리
    @Bean(destroyMethod = "dispose")
    public ConnectionProvider kisConnectionProvider() {
        return ConnectionProvider.builder("kis-connection-pool")
                .maxConnections(poolMaxConnections)                    // 최대 동시 유지 커넥션 수
                .pendingAcquireTimeout(Duration.ofSeconds(10))         // 커넥션 풀 고갈 시 풀 확보 대기 시간
                .maxIdleTime(Duration.ofSeconds(poolMaxIdleSeconds))   // 유휴 커넥션 유지 시간 (경과 시 반환)
                .build();
    }

    // KIS API 전용 WebClient 빈
    @Bean(name = "kisWebClient")
    public WebClient kisWebClient(ConnectionProvider provider) {
        
        // HttpClient 세부 튜닝: TCP/IP 레벨에서의 타임아웃 방어막 설정
        HttpClient httpClient = HttpClient.create(provider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMillis) // 연결 맺기 타임아웃
                .responseTimeout(Duration.ofSeconds(responseTimeoutSeconds))        // 응답 대기 타임아웃
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(readTimeoutSeconds)) // 읽기 타임아웃
                        .addHandlerLast(new WriteTimeoutHandler(writeTimeoutSeconds)) // 쓰기 타임아웃
                );

        // WebClient 빌드
        return WebClient.builder()
                .baseUrl(kisUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(maxInMemorySize)) // 대용량 응답 버퍼 확보
                .filter(logRequest()) // 디버깅용 외부 통신 로깅 필터 부착
                .build();
    }

    // 외부 API 통신 시 Request URL을 로깅하기 위한 커스텀 필터
    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            log.info("[KIS WebClient Request] {} {}", clientRequest.method(), clientRequest.url());
            return Mono.just(clientRequest);
        });
    }
}
