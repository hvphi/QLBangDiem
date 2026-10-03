package app.webbangdiem.domain;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    ProblemDetail responseStatus(ResponseStatusException error) {
        return ProblemDetail.forStatusAndDetail(error.getStatusCode(), error.getReason() == null ? "Request failed" : error.getReason());
    }

    @ExceptionHandler(EmptyResultDataAccessException.class)
    ProblemDetail notFound(EmptyResultDataAccessException error) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Không tìm thấy dữ liệu yêu cầu.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail invalidArgument(IllegalArgumentException error) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, error.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail conflict(DataIntegrityViolationException error) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Mã lớp học phần đã tồn tại hoặc quan hệ tham chiếu không hợp lệ.");
    }
}
