package com.example.pezonn.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pezonn.MetronomeViewModel
import com.example.pezonn.ui.theme.MetroBackground
import com.example.pezonn.ui.theme.MetroCyan
import com.example.pezonn.ui.theme.MetroGold
import com.example.pezonn.ui.theme.MetroInactive
import com.example.pezonn.ui.theme.MetroRed
import com.example.pezonn.ui.theme.MetroSurface
import com.example.pezonn.ui.theme.MetroSurfaceDark
import com.example.pezonn.ui.theme.MetroTextPrimary
import com.example.pezonn.ui.theme.MetroTextSecondary
import kotlinx.coroutines.launch

private val EaseOutCubic = CubicBezierEasing(0.33f, 1f, 0.68f, 1f)

@Composable
fun MetronomeScreen(viewModel: MetronomeViewModel = viewModel()) {
    val bpm by viewModel.bpm.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentBeat by viewModel.currentBeat.collectAsState()
    val beatsPerMeasure by viewModel.beatsPerMeasure.collectAsState()
    val debugLog by viewModel.debugLog.collectAsState()
    val isVoiceActive by viewModel.isVoiceActive.collectAsState()

    val context = LocalContext.current
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
        if (granted) viewModel.toggleVoiceListening()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(MetroSurfaceDark, MetroBackground),
                    radius = 1200f
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))

            Text(
                text = "PEZON",
                style = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 8.sp,
                    color = MetroCyan.copy(alpha = 0.6f)
                )
            )

            Spacer(Modifier.height(12.dp))

            DebugLog(entries = debugLog)

            Spacer(Modifier.weight(0.3f))

            BeatIndicators(
                currentBeat = currentBeat,
                beatsPerMeasure = beatsPerMeasure,
                isPlaying = isPlaying
            )

            Spacer(Modifier.height(32.dp))

            BpmDisplay(
                bpm = bpm,
                currentBeat = currentBeat,
                isPlaying = isPlaying,
                onBpmChange = viewModel::setBpm
            )

            Spacer(Modifier.height(40.dp))

            BpmSlider(bpm = bpm, onBpmChange = viewModel::setBpm)

            Spacer(Modifier.height(20.dp))

            BpmControls(bpm = bpm, onBpmChange = viewModel::setBpm)

            Spacer(Modifier.weight(0.5f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TapTempoButton(onTap = viewModel::tapTempo)
                PlayStopButton(isPlaying = isPlaying, onClick = viewModel::togglePlayback)
                MicButton(
                    isActive = isVoiceActive,
                    onClick = {
                        if (hasAudioPermission) {
                            viewModel.toggleVoiceListening()
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                )
            }

            Spacer(Modifier.height(48.dp))
        }
    }
}

@Composable
private fun DebugLog(entries: List<String>) {
    if (entries.isEmpty()) return

    val scrollState = rememberScrollState()

    LaunchedEffect(entries.size) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 100.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MetroSurface.copy(alpha = 0.6f))
            .padding(8.dp)
            .verticalScroll(scrollState)
    ) {
        entries.forEach { entry ->
            Text(
                text = entry,
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = when {
                        entry.startsWith("[CMD]") -> MetroCyan
                        entry.startsWith("[ERR]") -> MetroRed
                        entry.startsWith("[RES]") -> MetroGold
                        else -> MetroTextSecondary
                    }
                )
            )
        }
    }
}

@Composable
private fun BeatIndicators(
    currentBeat: Int,
    beatsPerMeasure: Int,
    isPlaying: Boolean
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until beatsPerMeasure) {
            BeatDot(
                isActive = isPlaying && i == currentBeat,
                isAccent = i == 0
            )
        }
    }
}

@Composable
private fun BeatDot(isActive: Boolean, isAccent: Boolean) {
    val targetColor = when {
        isActive && isAccent -> MetroGold
        isActive -> MetroCyan
        else -> MetroInactive
    }
    val color by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(80),
        label = "beatDotColor"
    )
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.5f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "beatDotScale"
    )

    Canvas(modifier = Modifier.size(24.dp)) {
        val baseRadius = 5.dp.toPx()
        val radius = baseRadius * scale

        if (isActive) {
            drawCircle(
                color = color.copy(alpha = 0.25f),
                radius = radius + 6.dp.toPx()
            )
            drawCircle(
                color = color.copy(alpha = 0.12f),
                radius = radius + 12.dp.toPx()
            )
        }
        drawCircle(color = color, radius = radius)
    }
}

@Composable
private fun BpmDisplay(
    bpm: Int,
    currentBeat: Int,
    isPlaying: Boolean,
    onBpmChange: (Int) -> Unit
) {
    val pulseAnim = remember { Animatable(1f) }
    val glowAlpha = remember { Animatable(0.15f) }

    var beatTrigger by remember { mutableIntStateOf(0) }
    LaunchedEffect(currentBeat) {
        if (currentBeat >= 0) {
            beatTrigger++
        }
    }

    LaunchedEffect(beatTrigger) {
        if (beatTrigger > 0) {
            pulseAnim.snapTo(1.05f)
            glowAlpha.snapTo(0.5f)
            launch {
                pulseAnim.animateTo(1f, tween(350, easing = EaseOutCubic))
            }
            glowAlpha.animateTo(0.12f, tween(350, easing = EaseOutCubic))
        }
    }

    val ringColor = if (isPlaying) MetroCyan else MetroInactive

    Box(contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(240.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.minDimension / 2 - 8.dp.toPx()

            if (isPlaying) {
                drawCircle(
                    color = MetroCyan.copy(alpha = glowAlpha.value * 0.3f),
                    radius = radius + 14.dp.toPx(),
                    center = center
                )
                drawCircle(
                    color = MetroCyan.copy(alpha = glowAlpha.value * 0.12f),
                    radius = radius + 28.dp.toPx(),
                    center = center
                )
            }

            drawCircle(
                color = ringColor.copy(alpha = if (isPlaying) 0.8f else 0.25f),
                radius = radius * pulseAnim.value,
                center = center,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )

            drawCircle(
                color = ringColor.copy(alpha = 0.06f),
                radius = radius - 24.dp.toPx(),
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BpmTextField(bpm = bpm, onBpmChange = onBpmChange)
            Spacer(Modifier.height(2.dp))
            Text(
                text = "BPM",
                style = TextStyle(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 4.sp,
                    color = MetroTextSecondary
                )
            )
        }
    }
}

@Composable
private fun BpmTextField(bpm: Int, onBpmChange: (Int) -> Unit) {
    val focusManager = LocalFocusManager.current
    var text by remember(bpm) { mutableStateOf(bpm.toString()) }

    BasicTextField(
        value = text,
        onValueChange = { newText ->
            text = newText.filter { it.isDigit() }.take(3)
        },
        textStyle = TextStyle(
            color = MetroTextPrimary,
            fontSize = 80.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        ),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                val newBpm = text.toIntOrNull()
                if (newBpm != null) onBpmChange(newBpm)
                else text = bpm.toString()
                focusManager.clearFocus()
            }
        ),
        cursorBrush = SolidColor(MetroCyan),
        singleLine = true,
        modifier = Modifier.widthIn(min = 100.dp, max = 200.dp)
    )
}

@Composable
private fun BpmSlider(bpm: Int, onBpmChange: (Int) -> Unit) {
    Slider(
        value = bpm.toFloat(),
        onValueChange = { onBpmChange(it.toInt()) },
        valueRange = 20f..300f,
        modifier = Modifier.fillMaxWidth(),
        colors = SliderDefaults.colors(
            thumbColor = MetroCyan,
            activeTrackColor = MetroCyan,
            inactiveTrackColor = MetroInactive
        )
    )
}

@Composable
private fun BpmControls(bpm: Int, onBpmChange: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ControlButton(text = "-5") { onBpmChange(bpm - 5) }
        Spacer(Modifier.width(12.dp))
        ControlButton(text = "-1") { onBpmChange(bpm - 1) }
        Spacer(Modifier.width(40.dp))
        ControlButton(text = "+1") { onBpmChange(bpm + 1) }
        Spacer(Modifier.width(12.dp))
        ControlButton(text = "+5") { onBpmChange(bpm + 5) }
    }
}

@Composable
private fun ControlButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MetroSurface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = TextStyle(
                color = MetroTextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}

@Composable
private fun TapTempoButton(onTap: () -> Unit) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(MetroSurface)
            .clickable(onClick = onTap),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "TAP",
            style = TextStyle(
                color = MetroTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        )
    }
}

@Composable
private fun MicButton(isActive: Boolean, onClick: () -> Unit) {
    val bgColor by animateColorAsState(
        targetValue = if (isActive) MetroCyan else MetroSurface,
        animationSpec = tween(200),
        label = "micBg"
    )
    val textColor = if (isActive) MetroBackground else MetroTextSecondary

    Box(
        modifier = Modifier
            .size(64.dp)
            .then(
                if (isActive) Modifier.drawBehind {
                    drawCircle(
                        color = MetroCyan.copy(alpha = 0.2f),
                        radius = size.minDimension / 2 + 6.dp.toPx()
                    )
                } else Modifier
            )
            .clip(CircleShape)
            .background(bgColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "MIC",
            style = TextStyle(
                color = textColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        )
    }
}

@Composable
private fun PlayStopButton(isPlaying: Boolean, onClick: () -> Unit) {
    val buttonColor by animateColorAsState(
        targetValue = if (isPlaying) MetroRed else MetroCyan,
        animationSpec = tween(200),
        label = "playButtonColor"
    )

    Box(
        modifier = Modifier
            .size(80.dp)
            .drawBehind {
                drawCircle(
                    color = buttonColor.copy(alpha = 0.25f),
                    radius = size.minDimension / 2 + 10.dp.toPx()
                )
                drawCircle(
                    color = buttonColor.copy(alpha = 0.1f),
                    radius = size.minDimension / 2 + 22.dp.toPx()
                )
            }
            .clip(CircleShape)
            .background(buttonColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(28.dp)) {
            if (isPlaying) {
                val p = size.width * 0.18f
                val s = size.width - p * 2
                drawRect(
                    color = Color.White,
                    topLeft = Offset(p, p),
                    size = Size(s, s)
                )
            } else {
                val path = Path().apply {
                    moveTo(size.width * 0.22f, 0f)
                    lineTo(size.width, size.height / 2)
                    lineTo(size.width * 0.22f, size.height)
                    close()
                }
                drawPath(path, color = Color.White)
            }
        }
    }
}
