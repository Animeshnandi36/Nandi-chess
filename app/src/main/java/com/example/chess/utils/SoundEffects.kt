package com.example.chess.utils

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack

object SoundEffects {
    private const val SAMPLE_RATE = 22050
    private var isMuted = false
    private var appContext: android.content.Context? = null

    fun init(context: android.content.Context) {
        this.appContext = context.applicationContext
    }

    fun setMuted(muted: Boolean) {
        isMuted = muted
    }

    fun playMove() {
        if (isMuted) return
        playTone(480, 80)
    }

    fun playCapture() {
        if (isMuted) return
        playTones(arrayOf(400 to 60, 600 to 80))
    }

    fun playCheck() {
        if (isMuted) return
        playTones(arrayOf(330 to 120, 330 to 120, 480 to 200))
    }

    fun playCheckmate() {
        if (isMuted) return
        playTones(arrayOf(523 to 150, 659 to 150, 784 to 150, 1046 to 350))
    }

    fun playGameOver() {
        if (isMuted) return
        playTones(arrayOf(380 to 180, 320 to 180, 260 to 350))
    }

    private fun playTone(frequency: Int, durationMs: Int) {
        Thread {
            try {
                val numSamples = durationMs * SAMPLE_RATE / 1000
                val sample = DoubleArray(numSamples)
                val generatedSnd = ByteArray(2 * numSamples)
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / SAMPLE_RATE
                    sample[i] = Math.sin(2.0 * Math.PI * frequency.toDouble() * t)
                }
                var idx = 0
                for (dVal in sample) {
                    val valShort = (dVal * 32767).toInt().toShort()
                    generatedSnd[idx++] = (valShort.toInt() and 0x00ff).toByte()
                    generatedSnd[idx++] = (valShort.toInt() and 0xff00 ushr 8).toByte()
                }
                val audioAttributes = android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
                val audioFormat = android.media.AudioFormat.Builder()
                    .setEncoding(android.media.AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(android.media.AudioFormat.CHANNEL_OUT_MONO)
                    .build()
                val audioTrackBuilder = android.media.AudioTrack.Builder()
                    .setAudioAttributes(audioAttributes)
                    .setAudioFormat(audioFormat)
                    .setBufferSizeInBytes(generatedSnd.size)
                    .setTransferMode(android.media.AudioTrack.MODE_STATIC)

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    appContext?.let { ctx ->
                        val attributionCtx = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                            ctx.createAttributionContext("chess_audio")
                        } else {
                            ctx
                        }
                        audioTrackBuilder.setContext(attributionCtx)
                    }
                }
                val audioTrack = audioTrackBuilder.build()
                audioTrack.write(generatedSnd, 0, generatedSnd.size)
                audioTrack.play()
                Thread.sleep(durationMs.toLong() + 50)
                try {
                    audioTrack.stop()
                } catch (e: Exception) {}
                audioTrack.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun playTones(tones: Array<Pair<Int, Int>>) {
        Thread {
            try {
                val totalDurationMs = tones.sumOf { it.second }
                val numSamples = totalDurationMs * SAMPLE_RATE / 1000
                val sample = DoubleArray(numSamples)
                val generatedSnd = ByteArray(2 * numSamples)

                var sampleOffset = 0
                for ((freq, durMs) in tones) {
                    val tonesSamples = durMs * SAMPLE_RATE / 1000
                    for (i in 0 until tonesSamples) {
                        val t = i.toDouble() / SAMPLE_RATE
                        val idx = sampleOffset + i
                        if (idx < numSamples) {
                            sample[idx] = Math.sin(2.0 * Math.PI * freq.toDouble() * t)
                        }
                    }
                    sampleOffset += tonesSamples
                }

                var idx = 0
                for (dVal in sample) {
                    val valShort = (dVal * 32767).toInt().toShort()
                    generatedSnd[idx++] = (valShort.toInt() and 0x00ff).toByte()
                    generatedSnd[idx++] = (valShort.toInt() and 0xff00 ushr 8).toByte()
                }
                val audioAttributes = android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
                val audioFormat = android.media.AudioFormat.Builder()
                    .setEncoding(android.media.AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(android.media.AudioFormat.CHANNEL_OUT_MONO)
                    .build()
                val audioTrackBuilder = android.media.AudioTrack.Builder()
                    .setAudioAttributes(audioAttributes)
                    .setAudioFormat(audioFormat)
                    .setBufferSizeInBytes(generatedSnd.size)
                    .setTransferMode(android.media.AudioTrack.MODE_STATIC)

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    appContext?.let { ctx ->
                        val attributionCtx = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                            ctx.createAttributionContext("chess_audio")
                        } else {
                            ctx
                        }
                        audioTrackBuilder.setContext(attributionCtx)
                    }
                }
                val audioTrack = audioTrackBuilder.build()
                audioTrack.write(generatedSnd, 0, generatedSnd.size)
                audioTrack.play()
                Thread.sleep(totalDurationMs.toLong() + 50)
                try {
                    audioTrack.stop()
                } catch (e: Exception) {}
                audioTrack.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }
}
