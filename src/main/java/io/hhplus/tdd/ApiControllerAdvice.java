package io.hhplus.tdd;

import static org.springframework.http.HttpStatus.*;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
class ApiControllerAdvice extends ResponseEntityExceptionHandler {
    @ExceptionHandler(value = IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception e) {
        return ResponseEntity
                .status(BAD_REQUEST)
                .body(new ErrorResponse("400", e.getMessage() != null ? e.getMessage() : "잘못된 요청입니다."));
    }

    @ExceptionHandler(value = IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleCONFLICT(Exception e) {
        return ResponseEntity
                .status(CONFLICT)
                .body(new ErrorResponse("409", e.getMessage() != null ? e.getMessage() : "잘못된 상태입니다."));
    }

    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        return ResponseEntity.status(500).body(new ErrorResponse("500", "에러가 발생했습니다."));
    }
}
