package com.devanshu.miniverse.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.speech.tts.TextToSpeech
import com.devanshu.miniverse.R
import java.util.Locale

/**
 * Sound effects. All clips are short synthesized WAVs in res/raw, played
 * through one SoundPool. Respects the Sound switch in Settings.
 */
object Sfx {
    const val DICE = "dice"
    const val STEP = "step"
    const val SNAKE = "snake"
    const val LADDER = "ladder"
    const val WIN = "win"
    const val LOSE = "lose"
    const val TAP = "tap"
    const val COIN = "coin"
    const val DROP = "drop"
    const val CAPTURE = "capture"
    const val POP = "pop"
    const val ROCK = "rock"
    const val PAPER = "paper"
    const val SCISSORS = "scissors"
    const val BLAST = "blast"
    const val SPARKLE = "sparkle"
    const val KICK = "kick"
    const val WHISTLE = "whistle"
    const val CANNON = "cannon"
    const val DRUMROLL = "drumroll"
    const val CHAMPION = "champion"
    const val TICK = "tick"
    const val CORRECT = "correct"
    const val WRONG = "wrong"
    const val HIT = "hit"
    const val HORN = "horn"

    @Volatile var enabled: Boolean = true
    private var pool: SoundPool? = null
    private val ids = HashMap<String, Int>()

    fun init(context: Context) {
        if (pool != null) return
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val p = SoundPool.Builder().setMaxStreams(8).setAudioAttributes(attrs).build()
        val clips = mapOf(
            DICE to R.raw.dice, STEP to R.raw.step, SNAKE to R.raw.snake, LADDER to R.raw.ladder,
            WIN to R.raw.win, LOSE to R.raw.lose, TAP to R.raw.tap, COIN to R.raw.coin,
            DROP to R.raw.drop, CAPTURE to R.raw.capture, POP to R.raw.pop,
            ROCK to R.raw.rock, PAPER to R.raw.paper, SCISSORS to R.raw.scissors,
            BLAST to R.raw.blast, SPARKLE to R.raw.sparkle, KICK to R.raw.kick,
            WHISTLE to R.raw.whistle, CANNON to R.raw.cannon, DRUMROLL to R.raw.drumroll,
            CHAMPION to R.raw.champion, TICK to R.raw.tick, CORRECT to R.raw.correct,
            WRONG to R.raw.wrong, HIT to R.raw.hit, HORN to R.raw.horn,
        )
        clips.forEach { (name, res) -> ids[name] = p.load(context, res, 1) }
        pool = p
    }

    fun play(name: String, rate: Float = 1f, volume: Float = 1f) {
        if (!enabled) return
        val id = ids[name] ?: return
        pool?.play(id, volume, volume, 1, 0, rate.coerceIn(0.5f, 2f))
    }
}

/** Spoken greetings using the phone's built-in text-to-speech voice. */
object Voice {
    private var tts: TextToSpeech? = null
    private var ready = false
    private var pending: String? = null

    fun init(context: Context) {
        if (tts != null) return
        tts = TextToSpeech(context.applicationContext) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (ready) {
                tts?.language = Locale.getDefault()
                tts?.setPitch(1.25f)       // a little brighter, friendlier
                tts?.setSpeechRate(0.95f)
                pending?.let { say(it) }
                pending = null
            }
        }
    }

    fun say(text: String) {
        if (!Sfx.enabled) return
        if (!ready) { pending = text; return }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "miniverse-voice")
    }
}
