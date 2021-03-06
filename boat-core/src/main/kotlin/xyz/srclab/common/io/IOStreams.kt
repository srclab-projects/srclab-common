@file:JvmName("IOStreams")

package xyz.srclab.common.io

import org.apache.commons.io.IOUtils
import xyz.srclab.common.lang.Defaults
import java.io.*
import java.nio.charset.Charset
import kotlin.io.readBytes as readBytesKt

@JvmOverloads
fun InputStream.toReader(charset: Charset = Defaults.charset): Reader {
    return InputStreamReader(this, charset)
}

@JvmOverloads
fun OutputStream.toWriter(charset: Charset = Defaults.charset): Writer {
    return OutputStreamWriter(this, charset)
}

@JvmOverloads
fun InputStream.toBuffered(bufferSize: Int = DEFAULT_BUFFER_SIZE): BufferedInputStream {
    return BufferedInputStream(this, bufferSize)
}

@JvmOverloads
fun OutputStream.toBuffered(bufferSize: Int = DEFAULT_BUFFER_SIZE): BufferedOutputStream {
    return BufferedOutputStream(this, bufferSize)
}

@JvmOverloads
fun InputStream.readTo(bytes: ByteArray, offset: Int = 0, length: Int = bytes.size - offset): ByteArray {
    IOUtils.readFully(this, bytes, offset, length)
    return bytes
}

@JvmOverloads
fun InputStream.readTo(output: OutputStream, bufferSize: Int = DEFAULT_BUFFER_SIZE): Long {
    return this.copyTo(output, bufferSize)
}

@JvmOverloads
fun Reader.readTo(chars: CharArray, offset: Int = 0, length: Int = chars.size - offset): CharArray {
    IOUtils.readFully(this, chars, offset, length)
    return chars
}

@JvmOverloads
fun Reader.readTo(output: Writer, bufferSize: Int = DEFAULT_BUFFER_SIZE): Long {
    return this.copyTo(output, bufferSize)
}

fun InputStream.readBytes(): ByteArray {
    return this.readBytesKt()
}

@JvmOverloads
fun InputStream.readString(charset: Charset = Defaults.charset): String {
    return toReader(charset).readText()
}

@JvmOverloads
fun InputStream.readLines(charset: Charset = Defaults.charset): List<String> {
    return IOUtils.readLines(this, charset)
}

fun Reader.readString(): String {
    return this.readText()
}

fun Reader.readLines(): List<String> {
    return IOUtils.readLines(this)
}