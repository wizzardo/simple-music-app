package com.example.be.misc

import java.text.SimpleDateFormat
import java.util.*

object HttpDateFormatterHolder {
    var formatter: ThreadLocal<SimpleDateFormat> = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat {
            val format = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US)
            format.timeZone = TimeZone.getTimeZone("GMT")
            return format
        }
    }

    fun get(): SimpleDateFormat {
        return formatter.get()
    }
}
