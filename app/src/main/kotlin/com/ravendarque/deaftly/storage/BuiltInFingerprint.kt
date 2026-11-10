package com.ravendarque.deaftly.storage

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.security.MessageDigest

fun TemplateDto.contentFingerprint(): String {
    val bytes = ByteArrayOutputStream()

    fun write(value: String) {
        val encoded = value.encodeToByteArray()
        bytes.write(ByteBuffer.allocate(Int.SIZE_BYTES).putInt(encoded.size).array())
        bytes.write(encoded)
    }

    fun write(value: Int) = write(value.toString())

    write(title)
    write(category)
    write(body)
    write(placeholders.size)
    placeholders.forEach { placeholder ->
        write(placeholder.key)
        write(placeholder.label)
        write(placeholder.type.name)
        write(placeholder.options.size)
        placeholder.options.forEach(::write)
        write(if (placeholder.repeatable) "1" else "0")
    }
    return MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()).joinToString("") { "%02x".format(it) }
}
