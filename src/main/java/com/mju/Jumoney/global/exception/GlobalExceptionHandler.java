package com.mju.Jumoney.global.exception;

import com.mju.Jumoney.global.response.ApiResponse;
import com.mju.Jumoney.global.response.BaseErrorCode;
import com.mju.Jumoney.global.response.ErrorCode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    // 비즈니스 로직 예외
    // ex) throw new CustomException(ErrorCode.XXX)했을 때
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Void>> handleCustomException(
            CustomException ex, WebRequest request) {

        BaseErrorCode errorCode = ex.getErrorCode();

        log.warn("Business Exception: code={}, message={}, uri={}",
                errorCode.getCode(), errorCode.getMessage(), getRequestURI(request));

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.error(errorCode));
    }

    // @Valid + @RequestBody 유효성 검증 실패 시
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        List<String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .distinct()
                .toList();
        String errorMessage = fieldErrors.isEmpty()
                ? ErrorCode.VALIDATION_FAILED.getMessage()
                : String.join(", ", fieldErrors);

        log.warn("Validation failed: uri={}, errors={}", getRequestURI(request), fieldErrors);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ErrorCode.VALIDATION_FAILED, errorMessage));
    }

    // @Validated + @PathVariable, @RequestParam 유효성 검증 실패 시
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(
            ConstraintViolationException ex, WebRequest request) {

        List<String> violations = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .distinct()
                .toList();
        String errorMessage = violations.isEmpty()
                ? ErrorCode.VALIDATION_FAILED.getMessage()
                : String.join(", ", violations);

        log.warn("Constraint violation: uri={}, errors={}", getRequestURI(request), violations);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ErrorCode.VALIDATION_FAILED, errorMessage));
    }

    // @RequestParam 타입 불일치 시
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex, WebRequest request) {

        String expectedType = Optional.ofNullable(ex.getRequiredType())
                .map(Class::getSimpleName)
                .orElse("unknown");
        String errorMessage = String.format("파라미터 '%s'는 %s 타입이어야 합니다.", ex.getName(), expectedType);

        log.warn("Type mismatch: uri={}, param={}, expectedType={}", getRequestURI(request), ex.getName(), expectedType);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ErrorCode.INVALID_PARAMETER_TYPE, errorMessage));
    }

    // 필수 @RequestParam 누락 시
    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        String errorMessage = String.format("필수 파라미터 '%s'가 누락되었습니다.", ex.getParameterName());

        log.warn("Missing parameter: uri={}, param={}", getRequestURI(request), ex.getParameterName());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ErrorCode.BAD_REQUEST, errorMessage));
    }

    // @RequestBody의 JSON 형식 오류 시
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        log.warn("JSON parsing error: uri={}, message={}", getRequestURI(request), ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ErrorCode.INVALID_REQUEST_FORMAT));
    }

    // 나머지 오류
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGlobalException(
            Exception ex, WebRequest request) {

        log.error("Unexpected exception: uri={}, type={}, message={}, stackTrace={}",
                getRequestURI(request), ex.getClass().getSimpleName(), ex.getMessage(), getStackTrace(ex));

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR));
    }

    // ========== 헬퍼 메서드 ==========
    private static String getRequestURI(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }
    private static String getStackTrace(Exception ex) {
        return stream(ex.getStackTrace())
                .limit(5)
                .map(StackTraceElement::toString)
                .collect(Collectors.joining(" | "));
    }
}