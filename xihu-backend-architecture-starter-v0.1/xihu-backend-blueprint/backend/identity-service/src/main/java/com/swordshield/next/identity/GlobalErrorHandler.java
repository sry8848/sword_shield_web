package com.swordshield.next.identity;
import com.swordshield.next.common.ApiFailure;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
@RestControllerAdvice
public class GlobalErrorHandler {
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiFailure> validation(MethodArgumentNotValidException exception) {
    return ResponseEntity.badRequest().body(ApiFailure.of("INVALID_ARGUMENT","请求参数格式错误"));
  }
  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ApiFailure> known(ResponseStatusException exception) {
    HttpStatus status=HttpStatus.resolve(exception.getStatusCode().value());
    String code=switch (exception.getStatusCode().value()) {
      case 401 -> "UNAUTHENTICATED"; case 403 -> "FORBIDDEN"; case 404 -> "RESOURCE_NOT_FOUND";
      case 409 -> "BUSINESS_CONFLICT"; case 422 -> "BUSINESS_RULE_VIOLATION"; default -> "INVALID_ARGUMENT";
    };
    return ResponseEntity.status(exception.getStatusCode()).body(ApiFailure.of(code,exception.getReason()==null?"请求无法处理":exception.getReason()));
  }
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiFailure> unexpected(Exception exception) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiFailure.of("INTERNAL_ERROR","服务暂时不可用"));
  }
}
