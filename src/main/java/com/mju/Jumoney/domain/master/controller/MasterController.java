package com.mju.Jumoney.domain.master.controller;

import com.mju.Jumoney.domain.master.dto.MasterDetailResponse;
import com.mju.Jumoney.domain.master.dto.MasterListResponse;
import com.mju.Jumoney.domain.master.dto.MasterSelectionResponse;
import com.mju.Jumoney.domain.master.service.MasterQueryService;
import com.mju.Jumoney.domain.master.service.MasterSelectionService;
import com.mju.Jumoney.global.exception.CustomException;
import com.mju.Jumoney.global.jwt.UserPrincipal;
import com.mju.Jumoney.global.response.ApiResponse;
import com.mju.Jumoney.global.response.ErrorCode;
import com.mju.Jumoney.global.response.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "거장 정보", description = "거장 정보 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/master")
public class MasterController {

    private final MasterQueryService masterQueryService;
    private final MasterSelectionService masterSelectionService;

    @Operation(summary = "거장 목록 조회", description = "거장 목록 화면에 필요한 거장 기본 정보를 조회합니다.")
    @GetMapping("/masters")
    public ResponseEntity<ApiResponse<List<MasterListResponse>>> getMasters() {
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.OK, masterQueryService.getMasterList()));
    }

    @Operation(summary = "거장 상세정보 조회", description = "거장 상세 화면 상단에 필요한 정보를 조회합니다.")
    @GetMapping("/masters/{masterId}/detail")
    public ResponseEntity<ApiResponse<MasterDetailResponse>> getMasterDetail(
            @PathVariable Long masterId
    ) {
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.OK, masterQueryService.getMasterDetail(masterId)));
    }

    @Operation(summary = "거장 선택", description = "로그인 사용자가 자신의 팀으로 사용할 거장을 선택하거나 변경합니다.")
    @PostMapping("/masters/{masterId}/selection")
    public ResponseEntity<ApiResponse<MasterSelectionResponse>> selectMaster(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long masterId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                SuccessCode.OK,
                masterSelectionService.selectMaster(getAuthenticatedUserId(userPrincipal), masterId)
        ));
    }

    // ========== 인증 메서드 ==========
    private Long getAuthenticatedUserId(UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        return userPrincipal.userId();
    }
}
