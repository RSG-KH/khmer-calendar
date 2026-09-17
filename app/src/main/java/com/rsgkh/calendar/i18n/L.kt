// Copyright (c) 2026 RSG-KH | Apache-2.0 License
package com.rsgkh.calendar.i18n

import java.util.Base64

/** Generated UTF-8 catalogs are shared by Compose, calendar labels and reminders. */
object L {
    private val words by lazy { read("translations.tsv") }
    private val token = Regex("\\{([A-Za-z][A-Za-z0-9_]*)\\}")

    private fun read(file: String): Map<String, Pair<String, String>> {
        val stream = checkNotNull(L::class.java.getResourceAsStream("/$file")) { "Missing translation catalog: $file" }
        return stream.bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines.filter { it.isNotBlank() && !it.startsWith('#') }.associate { line ->
                val parts = line.split('\t')
                check(parts.size == 3) { "Invalid translation row in $file" }
                fun decode(value: String) = String(Base64.getDecoder().decode(value), Charsets.UTF_8)
                parts[0] to (decode(parts[1]) to decode(parts[2]))
            }
        }
    }

    internal fun template(key: String, khmer: Boolean): String {
        val pair = checkNotNull(words[key]) { "Unknown translation: $key" }
        return if (khmer) pair.first else pair.second
    }

    fun text(key: String, khmer: Boolean, vararg values: Pair<String, Any>): String {
        val args = values.toMap()
        return token.replace(template(key, khmer)) { match ->
            checkNotNull(args[match.groupValues[1]]) { "Missing ${match.value} for $key" }.toString()
        }
    }
}
