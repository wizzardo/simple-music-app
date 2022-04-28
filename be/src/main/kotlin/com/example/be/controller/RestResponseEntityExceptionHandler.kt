package com.example.be.controller

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@ControllerAdvice
class RestResponseEntityExceptionHandler(
) {

    private val log = LoggerFactory.getLogger(RestResponseEntityExceptionHandler::class.java)


    @ExceptionHandler(Exception::class)
    fun handleExceptionInternal(ex: Exception, request: WebRequest): ResponseEntity<Any> {
        if (ex.message == "Missing request attribute 'user' of type UserAccount")
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()

        log.debug(ex.message, ex)
        return ResponseEntity(ErrorPojo(ex, HttpStatus.INTERNAL_SERVER_ERROR.value()), HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleMethodArgumentTypeMismatch(ex: MethodArgumentTypeMismatchException, request: WebRequest): ResponseEntity<Any> {
        log.debug(ex.message, ex)
        return ResponseEntity(ErrorPojo(ex, HttpStatus.BAD_REQUEST.value()), HttpStatus.BAD_REQUEST)
    }


    class ErrorPojo(t: Throwable, status: Int? = null) {
        var type: String
        var message: String?
        var stacktrace: Array<StackTraceElement>
        var cause: ErrorPojo?
        var status: Int?

        init {
            this.status = status
            type = t.javaClass.canonicalName
            message = t.message
            stacktrace = t.stackTrace
            val cause = t.cause
            if (cause != null)
                this.cause = ErrorPojo(cause)
            else
                this.cause = null
        }
    }

}
