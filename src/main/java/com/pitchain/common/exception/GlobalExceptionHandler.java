package com.pitchain.common.exception;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.pitchain.common.apiPayload.CustomResponse;
import com.pitchain.common.apiPayload.ErrorResponseDTO;
import com.pitchain.common.apiPayload.ErrorStatus;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    @ExceptionHandler(GeneralException.class)
    public ResponseEntity<CustomResponse> handleGeneralException(GeneralException e) {
        e.printStackTrace();

        ErrorResponseDTO error = e.getErrorResponse(messageSource);
        CustomResponse customResponse = CustomResponse.onFailure(error.getCode(), error.getMessage());

        return ResponseEntity
                .status(error.getHttpStatus())
                .body(customResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CustomResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        e.printStackTrace();

        ObjectError first = e.getBindingResult().getAllErrors().stream().findFirst().get();
        String errorMessage = first.getDefaultMessage();

        CustomResponse customResponse = CustomResponse.onFailure(ErrorStatus._BAD_REQUEST.name(), errorMessage);

        return ResponseEntity
                .status(e.getStatusCode())
                .body(customResponse);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<CustomResponse> handleConstraintViolationException(ConstraintViolationException e) {
        e.printStackTrace();

        String errorMessage = e.getConstraintViolations().stream()
                .map(constraintViolation -> constraintViolation.getMessage())
                .findFirst()
                .orElse("ConstraintViolationException(제약 조건 위반 오류)가 발생했습니다.");

        HttpStatus httpStatus = HttpStatus.BAD_REQUEST;
        CustomResponse customResponse = CustomResponse.onFailure(httpStatus.name(), errorMessage);

        return ResponseEntity
                .status(httpStatus)
                .body(customResponse);
    }

    @ExceptionHandler(JWTVerificationException.class)
    public ResponseEntity<CustomResponse> handleJWTVerificationException(JWTVerificationException e) {
        ErrorResponseDTO error = ErrorStatus.TOKEN_UNVERIFIED.getCustomResponseDTO(messageSource);
        CustomResponse customResponse = CustomResponse.onFailure(error.getCode(), error.getMessage());

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(customResponse);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<CustomResponse> handleDataIntegrityViolationException(DataIntegrityViolationException e) {
        log.error("DataIntegrityViolationException = {}", e.getMessage());
        String errorMessage = "DataIntegrityViolationException(제약 조건 위반 오류)가 발생했습니다.";

        HttpStatus httpStatus = HttpStatus.BAD_REQUEST;
        CustomResponse customResponse = CustomResponse.onFailure(httpStatus.name(), errorMessage);

        return ResponseEntity
                .status(httpStatus)
                .body(customResponse);
    }
}
