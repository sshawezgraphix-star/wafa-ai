package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs

class AudioStreamer(private val context: Context) {

    private val sampleRateRecord = 16000
    private val sampleRatePlay = 24000

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null

    private var recordJob: Job? = null
    private var isRecording = false

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    private val _outputAmplitude = MutableStateFlow(0f)
    val outputAmplitude: StateFlow<Float> = _outputAmplitude.asStateFlow()

    fun initPlayback() {
        if (audioTrack == null) {
            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRatePlay,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = maxOf(minBufferSize, sampleRatePlay * 2)

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRatePlay)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.play()
        }
    }

    fun startRecording(onAudioChunk: (ByteArray) -> Unit) {
        if (isRecording) return
        isRecording = true

        val minBufferSize = AudioRecord.getMinBufferSize(
            sampleRateRecord,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = maxOf(minBufferSize, 2048)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRateRecord,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            audioRecord?.startRecording()

            recordJob = CoroutineScope(Dispatchers.IO).launch {
                val buffer = ByteArray(1024)
                while (isRecording && audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    val readBytes = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (readBytes > 0) {
                        val chunk = buffer.copyOf(readBytes)
                        onAudioChunk(chunk)

                        // Calculate RMS amplitude for VAD animation
                        var sum = 0.0
                        var i = 0
                        while (i < readBytes - 1) {
                            val sample = (buffer[i].toInt() and 0xFF) or (buffer[i + 1].toInt() shl 8)
                            sum += abs(sample.toDouble())
                            i += 2
                        }
                        val avg = (sum / (readBytes / 2.0)) / 32768.0
                        _amplitude.value = avg.toFloat().coerceIn(0f, 1f)
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e("AudioStreamer", "Missing RECORD_AUDIO permission", e)
        } catch (e: Exception) {
            Log.e("AudioStreamer", "Error starting recording", e)
        }
    }

    fun stopRecording() {
        isRecording = false
        recordJob?.cancel()
        recordJob = null
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.e("AudioStreamer", "Error stopping recording", e)
        }
        audioRecord = null
        _amplitude.value = 0f
    }

    fun playAudioPcm24k(pcmData: ByteArray) {
        initPlayback()
        audioTrack?.write(pcmData, 0, pcmData.size)

        // Estimate amplitude for speaking animation
        var sum = 0.0
        var i = 0
        while (i < pcmData.size - 1) {
            val sample = (pcmData[i].toInt() and 0xFF) or (pcmData[i + 1].toInt() shl 8)
            sum += abs(sample.toDouble())
            i += 2
        }
        val avg = (sum / (pcmData.size / 2.0)) / 32768.0
        _outputAmplitude.value = avg.toFloat().coerceIn(0f, 1f)
    }

    fun stopPlayback() {
        try {
            audioTrack?.stop()
            audioTrack?.flush()
        } catch (e: Exception) {
            Log.e("AudioStreamer", "Error stopping playback", e)
        }
        _outputAmplitude.value = 0f
    }

    fun release() {
        stopRecording()
        stopPlayback()
        audioTrack?.release()
        audioTrack = null
    }
}
