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

@Tag(name = "Local Realtime Redis Smoke", description = "local 프로필 전용 실시간 피드 Redis 읽기 검증")
@RestController
@Profile("local")
@RequiredArgsConstructor
@RequestMapping("/api/local/realtime-redis")
public class RealtimeRedisSmokeController {

    private final RealtimeRedisReader realtimeRedisReader;

    @Operation(
            summary = "실시간 Redis String 조회",
            description = "SSH 터널로 연결한 실시간 피드 Redis에서 String value를 읽습니다. 쓰기 작업은 수행하지 않습니다."
    )
    @GetMapping("/value")
    public ResponseEntity<ApiResponse<RealtimeRedisValueSmokeResponse>> getValue(
            @Parameter(description = "조회할 Redis key")
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
            description = "SSH 터널로 연결한 실시간 피드 Redis에서 Hash field를 읽습니다. 쓰기 작업은 수행하지 않습니다."
    )
    @GetMapping("/hash")
    public ResponseEntity<ApiResponse<RealtimeRedisHashSmokeResponse>> getHash(
            @Parameter(description = "조회할 Redis hash key")
            @RequestParam String key,

            @Parameter(description = "조회할 hash field")
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
            description = "SSH 터널로 연결한 실시간 피드 Redis에서 ZSET member 범위를 읽습니다. 쓰기 작업은 수행하지 않습니다."
    )
    @GetMapping("/zset")
    public ResponseEntity<ApiResponse<RealtimeRedisZSetSmokeResponse>> getZSet(
            @Parameter(description = "조회할 Redis zset key")
            @RequestParam String key,

            @Parameter(description = "시작 index")
            @RequestParam(defaultValue = "0") long start,

            @Parameter(description = "종료 index")
            @RequestParam(defaultValue = "9") long end,

            @Parameter(description = "내림차순 조회 여부")
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
