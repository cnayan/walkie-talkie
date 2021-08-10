package com.cnayan.walkie_talkie

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch

class SoundRecorder {
    companion object {
        private val TAG = "SoundRecorder"
        val AUDIO_SOURCE =
            MediaRecorder.AudioSource.MIC // for raw audio, use MediaRecorder.AudioSource.UNPROCESSED, see note in MediaRecorder section

        val SAMPLE_RATE = 16000
        val CHANNEL_CONFIG: Int = AudioFormat.CHANNEL_IN_MONO
        val AUDIO_FORMAT: Int = AudioFormat.ENCODING_PCM_16BIT
        val BUFFER_SIZE_RECORDING: Int =
            AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
    }

    private var recording: Boolean = false
    var audioRecord: AudioRecord? = null
    var listener: ((ByteArray) -> Unit)? = null

    fun startRecording() {
        audioRecord = AudioRecord(AUDIO_SOURCE,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            BUFFER_SIZE_RECORDING)

        if (audioRecord == null || audioRecord!!.state != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "error initializing")
            return
        }

        audioRecord!!.startRecording()
        recording = true
        Log.d(TAG, "Recording started!")
        //Toast.makeText(this, "Recording started!", Toast.LENGTH_SHORT).show()

        _emitAudioData()
    }

    fun stopRecording() {
        if (recording) {
            audioRecord!!.stop()
            audioRecord!!.release()
            audioRecord = null;
            recording = false
            Log.d(TAG, "Recording stopped!")
        }
    }

    private fun _emitAudioData() { // to be called in a Runnable for a Thread created after call to startRecording()
        if (recording) {
            CoroutineScope(IO).launch {
                while (recording && listener != null) {
                    val data =
                        ByteArray(BUFFER_SIZE_RECORDING / 2) // assign size so that bytes are read in in chunks inferior to AudioRecord internal buffer size
                    val read: Int = audioRecord!!.read(data, 0, data.size)
                    if (read > 0) {
                        listener!!.invoke(data)
                    }
                }
            }
        }
    }
}