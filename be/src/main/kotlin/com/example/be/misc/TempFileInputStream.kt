package com.example.be.misc

import java.io.File
import java.io.FileInputStream
import java.io.InputStream

class TempFileInputStream(val file: File) : InputStream() {
    val stream: InputStream

    init {
        stream = FileInputStream(file)
    }

    override fun read(): Int {
        val read = stream.read()
        if (read == -1)
            onEnd()
        return read
    }


    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val read = stream.read(b, off, len)
        if (read == -1)
            onEnd()
        return read
    }

    override fun available(): Int {
        return stream.available()
    }

    override fun close() {
        onEnd()
    }

    fun onEnd() {
        stream.close()
        file.delete()
    }

    fun length(): Long {
        return file.length()
    }

}