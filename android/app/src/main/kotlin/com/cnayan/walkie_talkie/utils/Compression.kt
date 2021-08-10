package com.cnayan.walkie_talkie.utils

import android.util.Log
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.*

class Compression {
    companion object {
        private val TAG = "Compression"

        @Throws(Exception::class)
        fun compressGZip(bytes: ByteArray): ByteArray? {
            if (bytes.size == 0) {
                return bytes
            }

            Log.d(TAG, "Length : " + bytes.size)

            val outStream = ByteArrayOutputStream()

            val gzip = GZIPOutputStream(outStream)
            gzip.write(bytes)
            gzip.close()

            val out = outStream.toByteArray()
            Log.d(TAG, "Output length : " + out.size)
            return out
        }

        @Throws(Exception::class)
        fun decompressGZip(bytes: ByteArray?): ByteArray? {
            if (bytes == null || bytes.isEmpty()) {
                return bytes
            }

            Log.d(TAG, "Input String length : " + bytes.size)
            val gis = GZIPInputStream(ByteArrayInputStream(bytes))
//            var is_ = InputStreamReader(gis, "UTF-8");
//            val bf = BufferedReader(is)

//            var outStr = ArrayList<Byte>()
//            var line: ByteArray
//            while (bf.read().also({ line = it }) != null) {
//                outStr.add(line)
//            }

            var out = gis.readBytes()
            gis.close()

            Log.d(TAG, "Output length : " + out.size)
            return out
        }

        @Throws(IOException::class)
        fun compress(data: ByteArray): ByteArray {
            val deflater = Deflater()

            deflater.setLevel(Deflater.BEST_SPEED)
            deflater.setInput(data)
            deflater.finish()

            val outputStream = ByteArrayOutputStream(data.size)
            val buffer = ByteArray(2048)
            while (!deflater.finished()) {
                val count: Int = deflater.deflate(buffer) // returns the generated code... index
                outputStream.write(buffer, 0, count)
            }

            outputStream.close()
            val output: ByteArray = outputStream.toByteArray()
            Log.d(TAG, "Original: " + data.size / 1024 + " Kb")
            Log.d(TAG, "Compressed: " + output.size / 1024 + " Kb")
            return output
        }

        @Throws(IOException::class, DataFormatException::class)
        fun decompress(data: ByteArray): ByteArray {
            val inflater = Inflater()
            inflater.setInput(data)
            val outputStream = ByteArrayOutputStream(data.size)
            val buffer = ByteArray(2048)
            while (!inflater.finished()) {
                val count: Int = inflater.inflate(buffer)
                outputStream.write(buffer, 0, count)
            }
            outputStream.close()
            val output: ByteArray = outputStream.toByteArray()
            Log.d(TAG, "Original: " + data.size)
            Log.d(TAG, "Compressed: " + output.size)
            return output
        }
    }
}