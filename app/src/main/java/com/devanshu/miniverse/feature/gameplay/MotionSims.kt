package com.devanshu.miniverse.feature.gameplay

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.devanshu.miniverse.audio.Sfx
import com.devanshu.miniverse.ui.theme.MvDark
import com.devanshu.miniverse.ui.theme.MvPink
import com.devanshu.miniverse.ui.theme.MvSky
import com.devanshu.miniverse.ui.theme.MvYellow
import com.devanshu.miniverse.ui.theme.StarField
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

// ====================== Live camera background (AR feel) ======================

/**
 * Shows the back camera behind the game. Asks for the camera permission the
 * first time; if it is refused or the phone has no camera, a starfield is
 * shown instead so the game still works.
 */
@Composable
internal fun CameraBackground(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = it
    }
    LaunchedEffect(Unit) { if (!granted) launcher.launch(Manifest.permission.CAMERA) }

    if (granted) {
        AndroidView(
            modifier = modifier,
            factory = { ctx ->
                val view = PreviewView(ctx)
                val future = ProcessCameraProvider.getInstance(ctx)
                future.addListener({
                    try {
                        val provider = future.get()
                        val preview = Preview.Builder().build()
                        preview.setSurfaceProvider(view.surfaceProvider)
                        provider.unbindAll()
                        provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview)
                    } catch (e: Exception) {
                        // No usable camera: the view simply stays dark.
                    }
                }, ContextCompat.getMainExecutor(ctx))
                view
            },
        )
        DisposableEffect(Unit) {
            onDispose {
                try {
                    ProcessCameraProvider.getInstance(context).get().unbindAll()
                } catch (e: Exception) {
                    // nothing to release
                }
            }
        }
    } else {
        Box(modifier.background(MvDark)) { StarField() }
    }
}

// ====================== AR Balloon Hunt ======================

private class Burst(val x: Float, val y: Float, val kind: Int, val start: Float)

private class ArBalloon(
    val id: Int, val x: Float, var y: Float, val speed: Float,
    val kind: Int,          // 0 balloon, 1 bomb, 2 golden star
    val wobble: Float,
)

@Composable
internal fun ArBalloonSim(onEnd: (Int) -> Unit) {
    var balloons by remember { mutableStateOf(listOf<ArBalloon>()) }
    var score by remember { mutableIntStateOf(0) }
    var timeLeft by remember { mutableIntStateOf(30) }
    var nextId by remember { mutableIntStateOf(0) }
    var clock by remember { mutableStateOf(0f) }
    var box by remember { mutableStateOf(IntSize(1, 1)) }
    var bursts by remember { mutableStateOf(listOf<Burst>()) }

    LaunchedEffect(Unit) {
        var last = 0L
        var elapsed = 0L
        var spawnAcc = 0L
        while (timeLeft > 0) {
            withFrameNanos { now ->
                if (last != 0L) {
                    val dt = now - last
                    elapsed += dt; spawnAcc += dt
                    clock = elapsed / 1_000_000_000f
                    if (bursts.isNotEmpty()) bursts = bursts.filter { clock - it.start < 0.6f }
                    timeLeft = (30 - elapsed / 1_000_000_000L).toInt().coerceAtLeast(0)
                    if (spawnAcc > 600_000_000L) {
                        spawnAcc = 0L
                        val roll = Random.nextInt(10)
                        balloons = balloons + ArBalloon(
                            nextId++, 0.1f + Random.nextFloat() * 0.75f, 1.05f,
                            0.003f + Random.nextFloat() * 0.004f,
                            kind = if (roll == 0) 2 else if (roll <= 2) 1 else 0,
                            wobble = Random.nextFloat() * 6f,
                        )
                    }
                    balloons = balloons.map { it.apply { y -= speed } }.filter { it.y > -0.12f }
                }
                last = now
            }
        }
        onEnd(score.coerceAtLeast(0))
    }

    Box(Modifier.fillMaxSize().clip(RoundedCornerShape(18.dp)).onSizeChanged { box = it }) {
        CameraBackground(Modifier.fillMaxSize())
        Surface(
            color = MvDark.copy(alpha = 0.7f), shape = RoundedCornerShape(14.dp),
            modifier = Modifier.padding(10.dp),
        ) {
            Text(
                "⏱ $timeLeft s   ·   Score $score", color = Color.White, fontWeight = FontWeight.Black,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
        balloons.forEach { b ->
            Text(
                when (b.kind) { 1 -> "💣"; 2 -> "🌟"; else -> "🎈" },
                fontSize = 46.sp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .graphicsLayer {
                        translationX = b.x * box.width + sin(clock * 3f + b.wobble) * 24f
                        translationY = b.y * box.height
                        rotationZ = sin(clock * 2f + b.wobble) * 12f
                    }
                    .clickable(
                        interactionSource = remember(b.id) { MutableInteractionSource() },
                        indication = null,
                    ) {
                        balloons = balloons.filter { it.id != b.id }
                        Sfx.play(when (b.kind) { 1 -> Sfx.BLAST; 2 -> Sfx.SPARKLE; else -> Sfx.POP })
                        bursts = bursts + Burst(
                            b.x * box.width + 60f, b.y * box.height + 70f, b.kind, clock,
                        )
                        score += when (b.kind) { 1 -> -2; 2 -> 3; else -> 1 }
                    },
            )
        }
        // sparkles for pops, a fiery blast and red flash for bombs
        Canvas(Modifier.fillMaxSize()) {
            bursts.forEach { burst ->
                val age = ((clock - burst.start) / 0.6f).coerceIn(0f, 1f)
                val c = Offset(burst.x, burst.y)
                if (burst.kind == 1) {
                    drawRect(Color(0xFFFF2D2D).copy(alpha = (1f - age) * 0.35f))
                    drawCircle(Color(0xFFFF5A1F).copy(alpha = 1f - age), radius = 40f + age * 170f, center = c)
                    drawCircle(Color(0xFFFFE066).copy(alpha = 1f - age), radius = 20f + age * 90f, center = c)
                } else {
                    val col = if (burst.kind == 2) Color(0xFFFFE066) else Color.White
                    for (i in 0 until 10) {
                        val ang = i * 0.628f
                        val d = 20f + age * (if (burst.kind == 2) 150f else 95f)
                        val p = Offset(c.x + kotlin.math.cos(ang) * d, c.y + sin(ang) * d)
                        drawCircle(col.copy(alpha = 1f - age), radius = (1f - age) * 9f + 2f, center = p)
                        drawLine(col.copy(alpha = (1f - age) * 0.8f), c, p, strokeWidth = 3f)
                    }
                }
            }
        }
        Text(
            "🎈 +1   🌟 +3   💣 -2", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp)
                .background(MvDark.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

// ====================== Tilt & Catch (accelerometer) ======================

@Composable
internal fun TiltSim(onEnd: (Int) -> Unit) {
    val context = LocalContext.current
    var tilt by remember { mutableStateOf(Offset.Zero) }   // from the sensor
    var ball by remember { mutableStateOf(Offset(0.5f, 0.5f)) }
    var vel by remember { mutableStateOf(Offset.Zero) }
    var star by remember { mutableStateOf(Offset(0.25f, 0.3f)) }
    var score by remember { mutableIntStateOf(0) }
    var timeLeft by remember { mutableIntStateOf(30) }
    var hasSensor by remember { mutableStateOf(true) }
    var box by remember { mutableStateOf(IntSize(1, 1)) }

    DisposableEffect(Unit) {
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                // x: tilt left/right, y: tilt toward/away from you
                tilt = Offset(-event.values[0], event.values[1])
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        if (sensor != null) {
            manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        } else hasSensor = false
        onDispose { manager.unregisterListener(listener) }
    }

    LaunchedEffect(Unit) {
        var last = 0L
        var elapsed = 0L
        while (timeLeft > 0) {
            withFrameNanos { now ->
                if (last != 0L) {
                    elapsed += now - last
                    timeLeft = (30 - elapsed / 1_000_000_000L).toInt().coerceAtLeast(0)
                    var v = Offset(vel.x + tilt.x * 0.00035f, vel.y + tilt.y * 0.00035f) * 0.97f
                    var b = ball + v
                    if (b.x < 0.05f) { b = b.copy(x = 0.05f); v = v.copy(x = -v.x * 0.5f) }
                    if (b.x > 0.95f) { b = b.copy(x = 0.95f); v = v.copy(x = -v.x * 0.5f) }
                    if (b.y < 0.05f) { b = b.copy(y = 0.05f); v = v.copy(y = -v.y * 0.5f) }
                    if (b.y > 0.95f) { b = b.copy(y = 0.95f); v = v.copy(y = -v.y * 0.5f) }
                    val d = b - star
                    if (sqrt(d.x * d.x + d.y * d.y) < 0.09f) {
                        score += 1
                        star = Offset(0.1f + Random.nextFloat() * 0.8f, 0.1f + Random.nextFloat() * 0.8f)
                    }
                    ball = b; vel = v
                }
                last = now
            }
        }
        onEnd(score)
    }

    Box(Modifier.fillMaxSize().clip(RoundedCornerShape(18.dp)).background(MvDark).onSizeChanged { box = it }) {
        StarField()
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            // glow ring + ball
            drawCircle(MvSky.copy(alpha = 0.25f), radius = w * 0.085f, center = Offset(ball.x * w, ball.y * h))
            drawCircle(MvSky, radius = w * 0.05f, center = Offset(ball.x * w, ball.y * h))
            drawCircle(Color.White.copy(alpha = 0.7f), radius = w * 0.015f,
                center = Offset(ball.x * w - w * 0.015f, ball.y * h - w * 0.015f))
            // star target halo
            drawCircle(MvYellow.copy(alpha = 0.25f), radius = w * 0.08f, center = Offset(star.x * w, star.y * h))
        }
        Text(
            "⭐", fontSize = 34.sp,
            modifier = Modifier.align(Alignment.TopStart).graphicsLayer {
                translationX = star.x * box.width - 17.dp.toPx()
                translationY = star.y * box.height - 20.dp.toPx()
            },
        )
        Text(
            "⏱ $timeLeft s   ·   Stars $score", color = Color.White, fontWeight = FontWeight.Black,
            modifier = Modifier.padding(10.dp),
        )
        Text(
            if (hasSensor) "Tilt your phone to roll the ball!" else "No tilt sensor on this phone",
            color = MvPink, fontSize = 12.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp),
        )
    }
}
