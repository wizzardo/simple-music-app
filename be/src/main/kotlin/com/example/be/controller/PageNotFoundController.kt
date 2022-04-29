package com.example.be.controller

import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.util.ResourceUtils
import org.springframework.util.StreamUtils
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.servlet.NoHandlerFoundException
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.charset.StandardCharsets


@ControllerAdvice
class PageNotFoundController {

    companion object {
        val INDEX_HTML: String

        init {
            val indexFile: File = ResourceUtils.getFile("classpath:public/index.html")
            val inputStream = FileInputStream(indexFile)
            INDEX_HTML = StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8)
        }
    }

    @ExceptionHandler(NoHandlerFoundException::class)
    fun renderDefaultPage(): ResponseEntity<String>? {
        return try {
            ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(INDEX_HTML)
        } catch (e: IOException) {
            e.printStackTrace()
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("There was an error completing the action.")
        }
    }
}