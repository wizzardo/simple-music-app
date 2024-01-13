package com.example.be.service

import com.wizzardo.epoll.ByteBufferProvider
import com.wizzardo.epoll.ByteBufferWrapper
import com.wizzardo.epoll.readable.ReadableBuilder
import com.wizzardo.http.HttpConnection
import com.wizzardo.tools.io.IOTools
import com.wizzardo.tools.misc.pool.Pool
import com.wizzardo.tools.misc.pool.PoolBuilder
import com.wizzardo.tools.misc.pool.SimpleHolder
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.max

class StreamHandlingService {
    protected val RN: ByteArray = "\r\n".toByteArray(StandardCharsets.UTF_8)

    protected val buffers: Pool<ByteBufferProvider> = PoolBuilder<ByteBufferProvider>()
        .limitSize(16)
        .queue(PoolBuilder.createSharedQueueSupplier())
        .holder({ pool, value -> SimpleHolder(pool, value) })
        .resetter({ it.buffer.clear() })
        .supplier {
            val buffer = ByteBufferWrapper(8 * 1024)
            (ByteBufferProvider { buffer })
        }
        .build()

    protected val threads: ExecutorService

    init {
        var threadPool: ExecutorService
        try {
            val method = Executors::class.java.getMethod("newVirtualThreadPerTaskExecutor")
            threadPool = method.invoke(null) as ExecutorService
            println("created newVirtualThreadPerTaskExecutor")
        } catch (e: Exception) {
            threadPool = Executors.newCachedThreadPool({
                val thread = Thread(it)
                thread.isDaemon = true
                thread.setUncaughtExceptionHandler { t, e -> e.printStackTrace() }
                thread
            })
            println("created newCachedThreadPool")
        }
        threads = threadPool
    }


    fun sendInputStream(connection: HttpConnection<*, *, *>, data: InputStream) {
        connection.onDisconnect({ c, bp ->
            IOTools.close(data)
        })

        readAndSendChunk(connection, data)
    }

    protected fun readAndSendChunk(connection: HttpConnection<*, *, *>, data: InputStream) {
        threads.execute {
            if (!connection.isAlive) {
                IOTools.close(data)
                return@execute
            }

            val buf = ByteArray(1024 * 8)
            val r = data.read(buf, 0, buf.size)
            if (r == -1) {
                IOTools.close(data)
            }

            val rb = object : ReadableBuilder() {
                override fun onComplete() {
                    if (r == -1) {
                        IOTools.close(connection)
                    } else
                        readAndSendChunk(connection, data)
                }
            }

            val toAppend = max(r, 0)
            rb.append(Integer.toHexString(toAppend).toByteArray(StandardCharsets.UTF_8))
            rb.append(RN)
            rb.append(buf, 0, toAppend)
            rb.append(RN)

            buffers.holder().use {
                connection.write(rb, it.get())
            }
        }
    }

}