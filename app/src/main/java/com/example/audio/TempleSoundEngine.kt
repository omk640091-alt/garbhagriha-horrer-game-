package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Real-time DSP synthesizer using Android AudioTrack to generate realistic spatial sound effects:
 * - Temple Ghanta (Saptadhatu bronze bell harmonic overtones)
 * - Conch (Shankha) resonance & OM mantra drone
 * - Heartbeat & Brahmarakshasa guttural chase growl modulated by distance
 * - Stone chakra grinding & Vedic seal unlock chime
 */
class TempleSoundEngine {
  private val sampleRate = 22050
  private var isMuted = AtomicBoolean(false)

  fun setMuted(muted: Boolean) {
    isMuted.set(muted)
  }

  fun isAudioMuted(): Boolean = isMuted.get()

  /**
   * Plays a rich metallic Saptadhatu Temple Bell (Ghanta) strike with inharmonic overtones.
   */
  fun playTempleBell(baseFreqHz: Double = 528.0, durationMs: Int = 900) {
    if (isMuted.get()) return
    thread(start = true, isDaemon = true) {
      val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
      val buffer = ShortArray(numSamples)
      val partials = listOf(1.0, 2.0, 2.76, 4.07, 5.4)
      val weights = listOf(0.45, 0.25, 0.15, 0.10, 0.05)

      for (i in 0 until numSamples) {
        val t = i.toDouble() / sampleRate
        val envelope = exp(-4.2 * t)
        var sample = 0.0
        for (p in partials.indices) {
          sample += weights[p] * sin(2.0 * PI * baseFreqHz * partials[p] * t)
        }
        val clamped = (sample * envelope * Short.MAX_VALUE * 0.85).toInt()
          .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
        buffer[i] = clamped.toShort()
      }
      playBuffer(buffer)
    }
  }

  /**
   * Plays a sacred Vedic Mantra / Omkar resonant harmonic chord (108 Hz + 216 Hz + 324 Hz).
   */
  fun playMantraChantPulse(durationMs: Int = 1100) {
    if (isMuted.get()) return
    thread(start = true, isDaemon = true) {
      val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
      val buffer = ShortArray(numSamples)
      val f0 = 136.1 // Sadja / Om frequency
      for (i in 0 until numSamples) {
        val t = i.toDouble() / sampleRate
        val attack = (t / 0.15).coerceAtMost(1.0)
        val decay = exp(-1.8 * t)
        val env = attack * decay
        val wave = 0.50 * sin(2.0 * PI * f0 * t) +
          0.30 * sin(2.0 * PI * (f0 * 1.5) * t) +
          0.20 * sin(2.0 * PI * (f0 * 2.0) * t)
        buffer[i] = (wave * env * Short.MAX_VALUE * 0.75).toInt()
          .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
      }
      playBuffer(buffer)
    }
  }

  /**
   * Plays stone ring rotation grind sound for the Surya Yantra puzzle.
   */
  fun playStoneChakraGrind() {
    if (isMuted.get()) return
    thread(start = true, isDaemon = true) {
      val durationMs = 260
      val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
      val buffer = ShortArray(numSamples)
      var lowPass = 0.0
      for (i in 0 until numSamples) {
        val t = i.toDouble() / sampleRate
        val env = sin(PI * (i.toDouble() / numSamples))
        val noise = (Random.nextDouble() * 2.0 - 1.0)
        lowPass = 0.88 * lowPass + 0.12 * noise
        val rumble = 0.6 * sin(2.0 * PI * 72.0 * t) + 0.4 * lowPass
        buffer[i] = (rumble * env * Short.MAX_VALUE * 0.65).toInt()
          .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
      }
      playBuffer(buffer)
    }
  }

  /**
   * Plays a warm flickering ignition whoosh when lighting a sacred brass Diya lamp.
   */
  fun playDiyaIgnite() {
    if (isMuted.get()) return
    thread(start = true, isDaemon = true) {
      val durationMs = 450
      val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
      val buffer = ShortArray(numSamples)
      for (i in 0 until numSamples) {
        val t = i.toDouble() / sampleRate
        val env = exp(-5.0 * t)
        val chime = 0.6 * sin(2.0 * PI * 660.0 * t) + 0.4 * sin(2.0 * PI * 880.0 * t)
        val crackle = (Random.nextDouble() * 2.0 - 1.0) * 0.25
        buffer[i] = ((chime + crackle) * env * Short.MAX_VALUE * 0.7).toInt()
          .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
      }
      playBuffer(buffer)
    }
  }

  /**
   * Plays a low spatial heartbeat & Brahmarakshasa whisper thump when the creature is close.
   */
  fun playProximityHeartbeat(intensity: Float) {
    if (isMuted.get()) return
    val safeIntensity = intensity.coerceIn(0.1f, 1.0f)
    thread(start = true, isDaemon = true) {
      val durationMs = 340
      val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
      val buffer = ShortArray(numSamples)
      for (i in 0 until numSamples) {
        val t = i.toDouble() / sampleRate
        val pulse1 = exp(-28.0 * (t - 0.05) * (t - 0.05))
        val pulse2 = 0.75 * exp(-32.0 * (t - 0.19) * (t - 0.19))
        val subBass = sin(2.0 * PI * 54.0 * t) + 0.4 * sin(2.0 * PI * 108.0 * t)
        val sample = subBass * (pulse1 + pulse2) * safeIntensity
        buffer[i] = (sample * Short.MAX_VALUE * 0.85).toInt()
          .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
      }
      playBuffer(buffer)
    }
  }

  /**
   * Plays a terrifying dissonant Jumpscare / Creature strike chord if caught by the entity.
   */
  fun playEntityStrikeAlert() {
    if (isMuted.get()) return
    thread(start = true, isDaemon = true) {
      val durationMs = 750
      val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
      val buffer = ShortArray(numSamples)
      // Tritone + minor second dissonance
      val freqs = listOf(110.0, 116.5, 155.6, 233.1, 311.1)
      for (i in 0 until numSamples) {
        val t = i.toDouble() / sampleRate
        val env = exp(-2.8 * t)
        var acc = 0.0
        for (f in freqs) {
          acc += sin(2.0 * PI * f * t)
        }
        acc /= freqs.size
        buffer[i] = (acc * env * Short.MAX_VALUE * 0.9).toInt()
          .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
      }
      playBuffer(buffer)
    }
  }

  /**
   * Synthesizes a dynamic player footstep sound tailored to the detected environment surface material:
   * - STONE: Crisp granite heel tap with cavernous sanctum acoustic tail
   * - DIRT: Soft, low-pass muffled earthen crunch and gravel shift
   * - METAL: Resonant Ashtadhatu/bronze grate clang with metallic overtones
   */
  fun playSurfaceFootstep(surface: SurfaceMaterial, isSprinting: Boolean = false) {
    if (isMuted.get()) return
    thread(start = true, isDaemon = true) {
      val durationMs = when (surface) {
        SurfaceMaterial.STONE -> if (isSprinting) 155 else 185
        SurfaceMaterial.DIRT -> if (isSprinting) 135 else 165
        SurfaceMaterial.METAL -> if (isSprinting) 210 else 250
      }
      val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
      val buffer = ShortArray(numSamples)
      val gainBoost = if (isSprinting) 0.92 else 0.72
      var lowPass = 0.0

      for (i in 0 until numSamples) {
        val t = i.toDouble() / sampleRate
        val rawNoise = Random.nextDouble() * 2.0 - 1.0
        val sample = when (surface) {
          SurfaceMaterial.STONE -> {
            // Sharp granite click + 145Hz stone slab thump + early cave reflection
            val env = exp(-19.0 * t) + 0.22 * exp(-6.5 * t)
            lowPass = 0.72 * lowPass + 0.28 * rawNoise
            val slabBody = 0.58 * sin(2.0 * PI * 142.0 * t) +
              0.24 * sin(2.0 * PI * 310.0 * t) +
              0.28 * lowPass
            slabBody * env
          }
          SurfaceMaterial.DIRT -> {
            // Muffled earthen thud + double-envelope gravel/sand shift
            val env = exp(-14.0 * t) + 0.35 * exp(-22.0 * (t - 0.045).coerceAtLeast(0.0))
            lowPass = 0.90 * lowPass + 0.10 * rawNoise
            val earthBody = 0.62 * sin(2.0 * PI * 76.0 * t) + 0.48 * lowPass
            earthBody * env
          }
          SurfaceMaterial.METAL -> {
            // Resonant bronze/iron ritual plate strike with inharmonic metallic partials
            val env = exp(-11.5 * t)
            val clang = 0.44 * sin(2.0 * PI * 420.0 * t) +
              0.31 * sin(2.0 * PI * 785.0 * t) +
              0.18 * sin(2.0 * PI * 1190.0 * t) +
              0.12 * rawNoise * exp(-30.0 * t)
            clang * env
          }
        }
        buffer[i] = (sample * Short.MAX_VALUE * gainBoost).toInt()
          .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
      }
      playBuffer(buffer)
    }
  }

  private fun playBuffer(buffer: ShortArray) {
    var track: AudioTrack? = null
    try {
      val byteCount = buffer.size * 2
      track = AudioTrack.Builder()
        .setAudioAttributes(
          AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        )
        .setAudioFormat(
          AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()
        )
        .setBufferSizeInBytes(maxOf(byteCount, 4096))
        .setTransferMode(AudioTrack.MODE_STATIC)
        .build()

      track.write(buffer, 0, buffer.size)
      track.play()
      val sleepMs = (buffer.size * 1000L / sampleRate) + 80L
      Thread.sleep(sleepMs)
    } catch (_: Exception) {
      // Ignore AudioTrack hardware exceptions in headless/test environments
    } finally {
      try {
        track?.stop()
        track?.release()
      } catch (_: Exception) {
      }
    }
  }
}
