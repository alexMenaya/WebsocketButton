package com.example.websocketsbutton

import okhttp3.internal.ws.WebSocketProtocol
import okio.*
import java.io.IOException
import java.util.*

class WebSocketWriter(
    private val isClient: Boolean,
    val sink: BufferedSink,
    val random: Random
) {
    /** The [Buffer] of [sink]. Write to this and then flush/emit [sink]. */
    private val sinkBuffer: Buffer = sink.buffer
    private var writerClosed = false

    val buffer = Buffer()
    private val frameSink = FrameSink()

    var activeWriter: Boolean = false

    // Masks are only a concern for client writers.
    private val maskKey: ByteArray? = if (isClient) ByteArray(4) else null
    private val maskCursor: Buffer.UnsafeCursor? = if (isClient) Buffer.UnsafeCursor() else null

    /** Send a ping with the supplied [payload]. */
    @Throws(IOException::class)
    fun writePing(payload: ByteString) {
        writeControlFrame(CustomWebSocketProtocol.OPCODE_CONTROL_PING, payload)
    }

    /** Send a pong with the supplied [payload]. */
    @Throws(IOException::class)
    fun writePong(payload: ByteString) {
        writeControlFrame(CustomWebSocketProtocol.OPCODE_CONTROL_PONG, payload)
    }

    /**
     * Send a close frame with optional code and reason.
     *
     * @param code Status code as defined by
     *     [Section 7.4 of RFC 6455](http://tools.ietf.org/html/rfc6455#section-7.4) or `0`.
     * @param reason Reason for shutting down or `null`.
     */
    @Throws(IOException::class)
    fun writeClose(code: Int, reason: ByteString?) {
        var payload = ByteString.EMPTY
        if (code != 0 || reason != null) {
            if (code != 0) {
                WebSocketProtocol.validateCloseCode(code)
            }
            payload = Buffer().run {
                writeShort(code)
                if (reason != null) {
                    write(reason)
                }
                readByteString()
            }
        }

        try {
            writeControlFrame(CustomWebSocketProtocol.OPCODE_CONTROL_CLOSE, payload)
        } finally {
            writerClosed = true
        }
    }

    @Throws(IOException::class)
    private fun writeControlFrame(opcode: Int, payload: ByteString) {
        if (writerClosed) throw IOException("closed")

        val length = payload.size
        require(length <= CustomWebSocketProtocol.PAYLOAD_BYTE_MAX) {
            "Payload size must be less than or equal to ${CustomWebSocketProtocol.PAYLOAD_BYTE_MAX}"
        }

        val b0 = CustomWebSocketProtocol.B0_FLAG_FIN or opcode
        sinkBuffer.writeByte(b0)

        var b1 = length
        if (isClient) {
            b1 = b1 or CustomWebSocketProtocol.B1_FLAG_MASK
            sinkBuffer.writeByte(b1)

            random.nextBytes(maskKey!!)
            sinkBuffer.write(maskKey)

            if (length > 0) {
                val payloadStart = sinkBuffer.size
                sinkBuffer.write(payload)

                sinkBuffer.readAndWriteUnsafe(maskCursor!!)
                maskCursor.seek(payloadStart)
                WebSocketProtocol.toggleMask(maskCursor, maskKey)
                maskCursor.close()
            }
        } else {
            sinkBuffer.writeByte(b1)
            sinkBuffer.write(payload)
        }

        sink.flush()
    }

    /**
     * Stream a message payload as a series of frames. This allows control frames to be interleaved
     * between parts of the message.
     */
    fun newMessageSink(formatOpcode: Int, contentLength: Long): Sink {
        check(!activeWriter) { "Another message writer is active. Did you call close()?" }
        activeWriter = true

        // Reset FrameSink state for a new writer.
        frameSink.formatOpcode = formatOpcode
        frameSink.contentLength = contentLength
        frameSink.isFirstFrame = true
        frameSink.closed = false

        return frameSink
    }

    @Throws(IOException::class)
    fun writeMessageFrame(
        formatOpcode: Int,
        byteCount: Long,
        isFirstFrame: Boolean,
        isFinal: Boolean
    ) {
        if (writerClosed) throw IOException("closed")

        var b0 = if (isFirstFrame) formatOpcode else CustomWebSocketProtocol.OPCODE_CONTINUATION
        if (isFinal) {
            b0 = b0 or CustomWebSocketProtocol.B0_FLAG_FIN
        }
        sinkBuffer.writeByte(b0)

        var b1 = 0
        if (isClient) {
            b1 = b1 or CustomWebSocketProtocol.B1_FLAG_MASK
        }
        when {
            byteCount <= CustomWebSocketProtocol.PAYLOAD_BYTE_MAX -> {
                b1 = b1 or byteCount.toInt()
                sinkBuffer.writeByte(b1)
            }
            byteCount <= CustomWebSocketProtocol.PAYLOAD_SHORT_MAX -> {
                b1 = b1 or CustomWebSocketProtocol.PAYLOAD_SHORT
                sinkBuffer.writeByte(b1)
                sinkBuffer.writeShort(byteCount.toInt())
            }
            else -> {
                b1 = b1 or CustomWebSocketProtocol.PAYLOAD_LONG
                sinkBuffer.writeByte(b1)
                sinkBuffer.writeLong(byteCount)
            }
        }

        if (isClient) {
            random.nextBytes(maskKey!!)
            sinkBuffer.write(maskKey)

            if (byteCount > 0L) {
                val bufferStart = sinkBuffer.size
                sinkBuffer.write(buffer, byteCount)

                sinkBuffer.readAndWriteUnsafe(maskCursor!!)
                maskCursor.seek(bufferStart)
                WebSocketProtocol.toggleMask(maskCursor, maskKey)
                maskCursor.close()
            }
        } else {
            sinkBuffer.write(buffer, byteCount)
        }

        sink.emit()
    }

    internal inner class FrameSink : Sink {
        var formatOpcode = 0
        var contentLength = 0L
        var isFirstFrame = false
        var closed = false

        @Throws(IOException::class)
        override fun write(source: Buffer, byteCount: Long) {
            if (closed) throw IOException("closed")

            buffer.write(source, byteCount)

            // Determine if this is a buffered write which we can defer until close() flushes.
            val deferWrite = isFirstFrame &&
                    contentLength != -1L &&
                    buffer.size > contentLength - 8192 /* segment size */

            val emitCount = buffer.completeSegmentByteCount()
            if (emitCount > 0L && !deferWrite) {
                writeMessageFrame(formatOpcode, emitCount, isFirstFrame, isFinal = false)
                isFirstFrame = false
            }
        }

        @Throws(IOException::class)
        override fun flush() {
            if (closed) throw IOException("closed")

            writeMessageFrame(formatOpcode, buffer.size, isFirstFrame, isFinal = false)
            isFirstFrame = false
        }

        override fun timeout(): Timeout = sink.timeout()

        @Throws(IOException::class)
        override fun close() {
            if (closed) throw IOException("closed")

            writeMessageFrame(formatOpcode, buffer.size, isFirstFrame, isFinal = true)
            closed = true
            activeWriter = false
        }
    }

}
