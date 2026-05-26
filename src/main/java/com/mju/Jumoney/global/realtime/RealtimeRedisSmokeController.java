package com.mju.Jumoney.global.realtime;

import com.mju.Jumoney.global.response.ApiResponse;
import com.mju.Jumoney.global.response.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.DataType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@Tag(name = "Local Realtime Redis Smoke", description = "local 프로필 전용 Redis 읽기 검증")
@RestController
@Profile("local")
@RequiredArgsConstructor
@RequestMapping("/api/local/realtime-redis")
public class RealtimeRedisSmokeController {

    private final RealtimeRedisReader realtimeRedisReader;

    @Operation(
            summary = "실시간 Redis String 조회",
            description = """
                    실시간 Redis의 String key를 읽습니다. 쓰기 작업은 수행하지 않습니다.
                    
                    사용 목적:
                    - SSH 터널/비밀번호/포트 설정이 정상인지 확인
                    - Node 서버가 최신 종목 스냅샷을 적재했는지 확인
                    - Spring 현재가 fallback 전에 사용할 Redis 현재가/등락률 원천값 확인
                    
                    대표 key:
                    - stock:latest:005930
                    
                    응답 해석:
                    - type=string, exists=true: 해당 key를 정상 조회했습니다.
                    - exists=false: Redis 연결은 됐지만 key가 없거나 아직 적재되지 않았습니다.
                    """
    )
    @GetMapping("/value")
    public ResponseEntity<ApiResponse<RealtimeRedisValueSmokeResponse>> getValue(
            @Parameter(description = "조회할 Redis String key", example = "stock:latest:005930")
            @RequestParam String key
    ) {
        DataType type = realtimeRedisReader.type(key);
        String value = realtimeRedisReader.getRaw(key).orElse(null);

        return ResponseEntity.ok(ApiResponse.success(
                SuccessCode.OK,
                new RealtimeRedisValueSmokeResponse(key, type == null ? null : type.code(), value != null, value)
        ));
    }

    @Operation(
            summary = "실시간 Redis Hash field 조회",
            description = """
                    실시간 Redis의 Hash key에서 특정 field를 읽습니다. 쓰기 작업은 수행하지 않습니다.
                    
                    사용 목적:
                    - Hash 구조로 저장된 뉴스/분석 데이터를 필드 단위로 확인
                    - key의 자료구조가 hash인지 확인
                    
                    대표 key와 field:
                    - key=news:analysis:today, field=summary
                    - key=news:item:{newsId}, field=title
                    
                    응답 해석:
                    - type=hash, exists=true: 해당 field를 정상 조회했습니다.
                    - type=hash, exists=false: Hash key는 있으나 field가 없을 수 있습니다.
                    - WRONGTYPE 오류: 해당 key가 Hash가 아닙니다.
                    """
    )
    @GetMapping("/hash")
    public ResponseEntity<ApiResponse<RealtimeRedisHashSmokeResponse>> getHash(
            @Parameter(description = "조회할 Redis Hash key", example = "news:analysis:today")
            @RequestParam String key,

            @Parameter(description = "조회할 Hash field", example = "summary")
            @RequestParam String field
    ) {
        DataType type = realtimeRedisReader.type(key);
        String value = realtimeRedisReader.hashGetRaw(key, field).orElse(null);

        return ResponseEntity.ok(ApiResponse.success(
                SuccessCode.OK,
                new RealtimeRedisHashSmokeResponse(key, field, type == null ? null : type.code(), value != null, value)
        ));
    }

    @Operation(
            summary = "실시간 Redis ZSET 범위 조회",
            description = """
                    실시간 Redis의 ZSET member 범위를 읽습니다. 쓰기 작업은 수행하지 않습니다.
                    
                    사용 목적:
                    - Node 서버가 적재한 종목별 최근 미확정 분봉 확인
                    - 시간순 인덱스 형태의 데이터 확인
                    
                    대표 key:
                    - stock:minute-candles:005930
                    - news:today
                    
                    조회 팁:
                    - stock:minute-candles:{종목코드}는 reverse=true로 조회하면 최신 분봉부터 확인할 수 있습니다.
                    - start=0, end=9는 최대 10개 member를 조회합니다.
                    
                    응답 해석:
                    - type=zset, values가 비어있지 않음: 해당 범위의 member를 정상 조회했습니다.
                    - type=zset, values=[]: key가 비었거나 요청 범위에 member가 없습니다.
                    - WRONGTYPE 오류: 해당 key가 ZSET이 아닙니다.
                    """
    )
    @GetMapping("/zset")
    public ResponseEntity<ApiResponse<RealtimeRedisZSetSmokeResponse>> getZSet(
            @Parameter(description = "조회할 Redis ZSET key", example = "stock:minute-candles:005930")
            @RequestParam String key,

            @Parameter(description = "시작 index", example = "0")
            @RequestParam(defaultValue = "0") long start,

            @Parameter(description = "종료 index", example = "9")
            @RequestParam(defaultValue = "9") long end,

            @Parameter(description = "true면 score가 큰 member부터 조회합니다.", example = "true")
            @RequestParam(defaultValue = "true") boolean reverse
    ) {
        DataType type = realtimeRedisReader.type(key);
        Set<String> values = reverse
                ? realtimeRedisReader.zSetReverseRangeRaw(key, start, end)
                : realtimeRedisReader.zSetRangeRaw(key, start, end);

        return ResponseEntity.ok(ApiResponse.success(
                SuccessCode.OK,
                new RealtimeRedisZSetSmokeResponse(key, type == null ? null : type.code(), start, end, reverse, values)
        ));
    }

    public record RealtimeRedisValueSmokeResponse(
            String key,
            String type,
            boolean exists,
            String value
    ) {
    }

    public record RealtimeRedisHashSmokeResponse(
            String key,
            String field,
            String type,
            boolean exists,
            String value
    ) {
    }

    public record RealtimeRedisZSetSmokeResponse(
            String key,
            String type,
            long start,
            long end,
            boolean reverse,
            Set<String> values
    ) {
    }

}
