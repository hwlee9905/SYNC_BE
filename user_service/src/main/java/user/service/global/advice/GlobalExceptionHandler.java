package user.service.global.advice;

import java.nio.file.AccessDeniedException;
import java.util.Locale;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import lombok.extern.slf4j.Slf4j;
import user.service.global.exception.AuthenticationFailureException;
import user.service.global.exception.AuthorizationFailureException;
import user.service.global.exception.BusinessException;
import user.service.global.exception.EntityNotFoundException;
import user.service.global.exception.IdenticalValuesCannotChangedException;
import user.service.global.exception.InvalidValueException;
import user.service.global.exception.LinkCannotBeSavedException;
import user.service.global.exception.MemberDuplicateInProjectException;
import user.service.global.exception.UnknownException;
import user.service.global.exception.UserIdDuplicatedException;
import user.service.global.exception.UserNotFoundException;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
	
    /**
     * 파라미터 바인딩 에러 발생
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.error("Validation failed for argument: {}", e.getMessage());
        final ErrorResponse response = ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE, e.getBindingResult());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
    /**
     * 파라미터 바인딩 에러 발생
     */
    @ExceptionHandler(BindException.class)
    protected ResponseEntity<ErrorResponse> handleBindException(BindException e) {
        log.error("handleBindException", e);
        final ErrorResponse response = ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE, e.getBindingResult());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * 주로 @RequestParam enum binding 실패시 발생
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    protected ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.error("handleMethodArgumentTypeMismatchException", e);
        final ErrorResponse response = ErrorResponse.of(e);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * 지원하지 않는 HTTP method 호출시 발생
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    protected ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.error("handleHttpRequestMethodNotSupportedException", e);
        final ErrorResponse response = ErrorResponse.of(ErrorCode.METHOD_NOT_ALLOWED);
        return new ResponseEntity<>(response, HttpStatus.METHOD_NOT_ALLOWED);
    }
    @ExceptionHandler(AccessDeniedException.class)
    protected ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException e) {
        log.error("handleAccessDeniedException", e);
        final ErrorResponse response = ErrorResponse.of(ErrorCode.HANDLE_ACCESS_DENIED);
        return new ResponseEntity<>(response, HttpStatus.valueOf(ErrorCode.HANDLE_ACCESS_DENIED.getStatus()));
    }

    @ExceptionHandler(BusinessException.class)
    protected ResponseEntity<ErrorResponse> handleBusinessException(final BusinessException e) {
        log.error(e.getMessage(),e);
        final ErrorCode errorCode = e.getErrorCode();
        final ErrorResponse response = ErrorResponse.of(errorCode);
        return new ResponseEntity<>(response, HttpStatus.valueOf(errorCode.getStatus()));
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("handleException", e);
        final ErrorResponse response = ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR);
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * 권한이 없는 경로에 접근했을때 발생
     */
    @ExceptionHandler(AuthorizationFailureException.class)
    protected ResponseEntity<ErrorResponse> AuthorizationException(AuthorizationFailureException e) {
        log.error("AuthorizationFailureException", e);

        final ErrorResponse response = ErrorResponse.of(ErrorCode.USER_FAILED_AUTHORIZATION);
        return new ResponseEntity<>(response, HttpStatus.valueOf(ErrorCode.USER_FAILED_AUTHORIZATION.getStatus()));
    }
    /**
     * 잘못된 아이디 입력시 발생
     */
    @ExceptionHandler(AuthenticationFailureException.class)
    protected ResponseEntity<ErrorResponse> AuthenticationFailureException(AuthenticationFailureException e) {
        final ErrorResponse response = ErrorResponse.of(ErrorCode.USER_FAILED_AUTHENTICATION);
        return new ResponseEntity<>(response, HttpStatus.valueOf(ErrorCode.USER_FAILED_AUTHENTICATION.getStatus()));
    }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> httpMessageNotReadableException(HttpMessageNotReadableException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage(), "result", false));
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> DataIntegrityViolationException(UserIdDuplicatedException e) {
        log.error(e.getMessage(),e);
        final ErrorCode errorCode = e.getErrorCode();
        final ErrorResponse response = ErrorResponse.of(errorCode);
        return new ResponseEntity<>(response, HttpStatus.valueOf(errorCode.getStatus()));
    }
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> runtimeException(RuntimeException e) {
        log.error(e.getMessage(),e);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage(), "result", false));
    }
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<?> EntityNotFoundException(EntityNotFoundException e) {
        log.error(e.getMessage(),e);
        final ErrorCode errorCode = e.getErrorCode();
        final ErrorResponse response = ErrorResponse.of(errorCode , e.getMessage());
        return new ResponseEntity<>(response, HttpStatus.valueOf(errorCode.getStatus()));
    }
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<?> UserNotFoundException(UserNotFoundException e) {
        log.error(e.getMessage(),e);
        final ErrorCode errorCode = e.getErrorCode();
        final ErrorResponse response = ErrorResponse.of(errorCode);
        return new ResponseEntity<>(response, HttpStatus.valueOf(errorCode.getStatus()));
    }
    @ExceptionHandler(MemberDuplicateInProjectException.class)
    public ResponseEntity<?> MemberDuplicateInProjectException(MemberDuplicateInProjectException e) {
        log.error(e.getMessage(),e);
        final ErrorCode errorCode = e.getErrorCode();
        final ErrorResponse response = ErrorResponse.of(errorCode);
        return new ResponseEntity<>(response, HttpStatus.valueOf(errorCode.getStatus()));
    }
    @ExceptionHandler(InvalidValueException.class)
    public ResponseEntity<?> InvalidValueException(InvalidValueException e) {
        log.error(e.getMessage(),e);
        final ErrorCode errorCode = e.getErrorCode();
        final ErrorResponse response = ErrorResponse.of(errorCode , e.getMessage());
        return new ResponseEntity<>(response, HttpStatus.valueOf(errorCode.getStatus()));
    }
    
    @ExceptionHandler(UnknownException.class)
    public ResponseEntity<?> UnknownException(UnknownException e) {
    	log.error("UnknownException", e);
    	final ErrorCode errorCode = ErrorCode.UNKNOWN_ERROR;
        final ErrorResponse response = ErrorResponse.of(errorCode);
    	return new ResponseEntity<>(response, HttpStatus.valueOf(errorCode.getStatus()));
    }
    
    @ExceptionHandler(IdenticalValuesCannotChangedException.class)
    public ResponseEntity<?> IdenticalValuesCannotChangedException(IdenticalValuesCannotChangedException e){
    	final ErrorCode errorCode = e.getErrorCode();
    	final ErrorResponse response = ErrorResponse.of(errorCode);
    	return new ResponseEntity<>(response, HttpStatus.valueOf(errorCode.getStatus()));
    }
    
    /**
     * Invite
     */
    @ExceptionHandler(LinkCannotBeSavedException.class)
    protected ResponseEntity<ErrorResponse> handleLinkCannotBeSavedException(Locale locale, LinkCannotBeSavedException e){
    	log.error(e.getMessage());
    	final ErrorResponse response = ErrorResponse.of(ErrorCode.LINK_SAVE_ERROR);
    	return new ResponseEntity<>(response, HttpStatus.valueOf(ErrorCode.LINK_SAVE_ERROR.getStatus()));
    }

}