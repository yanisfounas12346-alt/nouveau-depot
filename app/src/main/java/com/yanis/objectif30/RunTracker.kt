package com.yanis.objectif30

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.yanis.objectif30.data.RunPreferences
import com.yanis.objectif30.ui.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale
import kotlin.math.max

data class RunLiveState(
    val running: Boolean = false,
    val speedKmh: Double = 0.0,
    val distanceKm: Double = 0.0,
    val elapsedSeconds: Int = 0,
    val steps: Int = 0,
    val targetSpeedKmh: Double = 0.0,
    val targetMinutes: Int = 30,
    val status: String = "Prêt",
    val gpsAccuracyM: Float = 0f
)

object RunSessionState {
    private val _state = MutableStateFlow(RunLiveState())
    val state: StateFlow<RunLiveState> = _state
    fun update(value: RunLiveState) { _state.value = value }
}

class RunTrackerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            Objectif30Theme {
                RunTrackerScreen(onClose = { finish() })
            }
        }
    }
}

@Composable
fun RunTrackerScreen(onClose: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val state by RunSessionState.state.collectAsState()
    val prefs = remember { RunPreferences(context) }
    var targetMinutes by remember { mutableIntStateOf(prefs.targetMinutes()) }
    var targetSpeed by remember { mutableFloatStateOf(prefs.targetSpeedKmh()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val locationGranted =
            grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

        if (locationGranted) {
            val intent = Intent(context, RunTrackingService::class.java).apply {
                action = RunTrackingService.ACTION_START
                putExtra(RunTrackingService.EXTRA_TARGET_MINUTES, targetMinutes)
                putExtra(RunTrackingService.EXTRA_TARGET_SPEED, targetSpeed)
            }
            ContextCompat.startForegroundService(context, intent)
        }
    }

    fun startRun() {
        prefs.saveTargetMinutes(targetMinutes)
        if (targetSpeed > 0f) prefs.saveTargetSpeedKmh(targetSpeed)

        val permissions = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= 29) {
            permissions += Manifest.permission.ACTIVITY_RECOGNITION
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    fun stopRun() {
        val intent = Intent(context, RunTrackingService::class.java).apply {
            action = RunTrackingService.ACTION_STOP
        }
        context.startService(intent)
    }

    val zoneText = if (state.targetSpeedKmh > 0.0) {
        val low = max(0.0, state.targetSpeedKmh - 0.6)
        val high = state.targetSpeedKmh + 0.6
        "%.1f–%.1f km/h".format(low, high)
    } else {
        "Calibration auto"
    }

    val zoneColor = when {
        state.status.startsWith("Zone cible") -> WildGreen
        state.status.startsWith("Au-dessus") -> WildAmber
        state.status.startsWith("Sous") -> WildBlue
        else -> WildCyan
    }

    WildBackdrop {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp, 22.dp, 16.dp, 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        WildEyebrow("Wildsport Run")
                        Text(
                            if (state.running) "Course en direct" else "Prêt à courir",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Black
                        )
                    }
                    WildSecondaryButton("Fermer", onClose)
                }
            }

            item {
                WildGlassCard(highlighted = true) {
                    Column(
                        Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        WildTag(
                            if (state.running) "LIVE GPS" else "PRÊT",
                            if (state.running) WildGreen else WildCyan
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "%.1f".format(state.speedKmh),
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Black,
                            color = zoneColor
                        )
                        Text(
                            "km/h",
                            color = WildMuted,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        WildTag("ZONE • " + zoneText, zoneColor)
                        Text(
                            state.status,
                            fontWeight = FontWeight.Bold,
                            color = zoneColor
                        )
                    }
                }
            }

            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    WildMetricTile(
                        "Temps",
                        "%02d:%02d".format(
                            state.elapsedSeconds / 60,
                            state.elapsedSeconds % 60
                        ),
                        accent = WildCyan,
                        modifier = Modifier.weight(1f)
                    )
                    WildMetricTile(
                        "Distance",
                        "%.2f km".format(state.distanceKm),
                        accent = WildBlue,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    WildMetricTile(
                        "Pas",
                        state.steps.toString(),
                        accent = WildViolet,
                        modifier = Modifier.weight(1f)
                    )
                    WildMetricTile(
                        "Précision GPS",
                        if (state.gpsAccuracyM > 0f) {
                            "±%.0f m".format(state.gpsAccuracyM)
                        } else {
                            "—"
                        },
                        accent = if (state.gpsAccuracyM in 1f..20f) WildGreen else WildAmber,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (!state.running) {
                item {
                    WildGlassCard {
                        WildEyebrow("Réglages")
                        Text(
                            "Séance cible",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            targetMinutes.toString() + " minutes",
                            color = WildCyan,
                            fontWeight = FontWeight.Bold
                        )
                        Slider(
                            value = targetMinutes.toFloat(),
                            onValueChange = { targetMinutes = it.toInt() },
                            valueRange = 20f..45f,
                            steps = 24
                        )

                        if (targetSpeed > 0f) {
                            Text(
                                "Vitesse cible • %.1f km/h".format(targetSpeed),
                                fontWeight = FontWeight.Bold
                            )
                            Slider(
                                value = targetSpeed,
                                onValueChange = { targetSpeed = it },
                                valueRange = 5f..15f,
                                steps = 19
                            )
                            WildSecondaryButton(
                                "Recalibrer mon allure facile",
                                {
                                    prefs.resetCalibration()
                                    targetSpeed = 0f
                                },
                                Modifier.fillMaxWidth()
                            )
                        } else {
                            Text(
                                "La première sortie calibre ton allure facile pendant environ 5 minutes. Ensuite Wildsport te guide vocalement dans ta zone.",
                                color = WildMuted
                            )
                        }
                    }
                }

                item {
                    WildPrimaryButton(
                        "▶  Démarrer la course",
                        { startRun() },
                        Modifier.fillMaxWidth()
                    )
                }
            } else {
                item {
                    Button(
                        onClick = { stopRun() },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("■  Arrêter la séance", fontWeight = FontWeight.Black)
                    }
                }
            }

            item {
                WildGlassCard {
                    WildEyebrow("Coach vocal")
                    Text(
                        "Wildsport te dit quand accélérer, quand ralentir, quand tu es dans la zone et quand terminer.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            item {
                Text(
                    "La zone cible est une allure de course facile personnalisée, pas une limite médicale. Douleur thoracique, malaise, essoufflement inhabituel ou douleur vive : arrête-toi.",
                    style = MaterialTheme.typography.bodySmall,
                    color = WildMuted
                )
            }
        }
    }
}

class RunTrackingService : Service(), LocationListener, SensorEventListener, TextToSpeech.OnInitListener {
    companion object {
        const val ACTION_START = "com.yanis.objectif30.RUN_START"
        const val ACTION_STOP = "com.yanis.objectif30.RUN_STOP"
        const val EXTRA_TARGET_SPEED = "target_speed"
        const val EXTRA_TARGET_MINUTES = "target_minutes"
        private const val CHANNEL_ID = "wildsport_run"
        private const val NOTIFICATION_ID = 3201
    }

    private lateinit var locationManager: LocationManager
    private lateinit var sensorManager: SensorManager
    private lateinit var prefs: RunPreferences
    private var stepSensor: Sensor? = null
    private var textToSpeech: TextToSpeech? = null
    private var ttsReady = false

    private var startRealtime = 0L
    private var lastLocation: Location? = null
    private var distanceMeters = 0.0
    private var stepBaseline: Float? = null
    private var currentSteps = 0
    private var targetSpeedKmh = 0.0
    private var targetMinutes = 30
    private var running = false

    private val speedWindow = ArrayDeque<Double>()
    private val calibrationSpeeds = mutableListOf<Double>()
    private var lastCueState = ""
    private var lastCueRealtime = 0L
    private var min25Announced = false

    private val handler = Handler(Looper.getMainLooper())
    private val ticker = object : Runnable {
        override fun run() {
            if (running) {
                updateElapsedAndCheckStop()
                handler.postDelayed(this, 1000L)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        prefs = RunPreferences(this)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        textToSpeech = TextToSpeech(this, this)
        createChannel()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech?.language = Locale.FRANCE
            ttsReady = true
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopRun(manual = true)
            ACTION_START -> {
                targetMinutes = intent.getIntExtra(EXTRA_TARGET_MINUTES, prefs.targetMinutes())
                    .coerceIn(20, 45)
                targetSpeedKmh = intent.getFloatExtra(
                    EXTRA_TARGET_SPEED,
                    prefs.targetSpeedKmh()
                ).toDouble()
                startRun()
            }
        }
        return START_NOT_STICKY
    }

    private fun startRun() {
        if (running) return
        running = true
        startRealtime = SystemClock.elapsedRealtime()
        distanceMeters = 0.0
        lastLocation = null
        stepBaseline = null
        currentSteps = 0
        speedWindow.clear()
        calibrationSpeeds.clear()
        lastCueState = ""
        lastCueRealtime = 0L
        min25Announced = false

        startForeground(
            NOTIFICATION_ID,
            buildNotification("Course active • acquisition GPS…")
        )

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            RunSessionState.update(
                RunLiveState(
                    running = false,
                    status = "Permission GPS manquante."
                )
            )
            stopSelf()
            return
        }

        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000L,
                1f,
                this
            )
        } catch (_: Exception) {
            RunSessionState.update(
                RunLiveState(
                    running = false,
                    status = "GPS indisponible."
                )
            )
            stopSelf()
            return
        }

        if (
            stepSensor != null &&
            (
                Build.VERSION.SDK_INT < 29 ||
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACTIVITY_RECOGNITION
                    ) == PackageManager.PERMISSION_GRANTED
                )
        ) {
            sensorManager.registerListener(
                this,
                stepSensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }

        val initialStatus = if (targetSpeedKmh > 0.0) {
            "Échauffe-toi tranquillement. Coaching de vitesse actif."
        } else {
            "Calibration de ton allure facile en cours."
        }

        RunSessionState.update(
            RunLiveState(
                running = true,
                targetSpeedKmh = targetSpeedKmh,
                targetMinutes = targetMinutes,
                status = initialStatus
            )
        )

        speak("Course démarrée. Échauffe-toi progressivement.")
        handler.post(ticker)
    }

    override fun onLocationChanged(location: Location) {
        if (!running) return
        if (location.accuracy > 40f) {
            val s = RunSessionState.state.value
            RunSessionState.update(
                s.copy(
                    gpsAccuracyM = location.accuracy,
                    status = "Signal GPS faible, attends quelques secondes."
                )
            )
            return
        }

        lastLocation?.let { previous ->
            val delta = previous.distanceTo(location)
            if (delta in 0.5f..80f) {
                distanceMeters += delta
            }
        }
        lastLocation = location

        val rawKmh = if (location.hasSpeed()) {
            (location.speed * 3.6).toDouble()
        } else {
            RunSessionState.state.value.speedKmh
        }

        if (rawKmh in 0.0..30.0) {
            speedWindow.addLast(rawKmh)
            while (speedWindow.size > 5) speedWindow.removeFirst()
        }

        val smoothKmh = if (speedWindow.isNotEmpty()) speedWindow.average() else 0.0
        val elapsed = elapsedSeconds()

        if (
            targetSpeedKmh <= 0.0 &&
            elapsed in 120..360 &&
            smoothKmh in 5.5..18.0 &&
            location.accuracy <= 25f
        ) {
            calibrationSpeeds += smoothKmh
        }

        if (
            targetSpeedKmh <= 0.0 &&
            elapsed >= 300 &&
            calibrationSpeeds.size >= 15
        ) {
            targetSpeedKmh = calibrationSpeeds.average()
            prefs.saveTargetSpeedKmh(targetSpeedKmh.toFloat())
            speak(
                "Calibration terminée. Ta zone cible est autour de " +
                    "%.1f".format(targetSpeedKmh) + " kilomètres heure."
            )
        }

        val coachingStatus = coachingStatus(smoothKmh, elapsed)
        maybeSpeakSpeedCue(coachingStatus, elapsed)

        val newState = RunLiveState(
            running = true,
            speedKmh = smoothKmh,
            distanceKm = distanceMeters / 1000.0,
            elapsedSeconds = elapsed,
            steps = currentSteps,
            targetSpeedKmh = targetSpeedKmh,
            targetMinutes = targetMinutes,
            status = coachingStatus,
            gpsAccuracyM = location.accuracy
        )
        RunSessionState.update(newState)
        updateNotification(newState)
    }

    private fun coachingStatus(speedKmh: Double, elapsed: Int): String {
        if (elapsed < 120) return "Échauffement progressif"
        if (targetSpeedKmh <= 0.0) return "Calibration de ton allure facile…"

        val low = targetSpeedKmh - 0.6
        val high = targetSpeedKmh + 0.6

        return when {
            speedKmh < low -> "Sous la zone cible • accélère légèrement"
            speedKmh > high -> "Au-dessus de la zone cible • ralentis légèrement"
            else -> "Zone cible atteinte ✓"
        }
    }

    private fun maybeSpeakSpeedCue(status: String, elapsed: Int) {
        if (elapsed < 300 || targetSpeedKmh <= 0.0) return

        val cueState = when {
            status.startsWith("Sous") -> "LOW"
            status.startsWith("Au-dessus") -> "HIGH"
            status.startsWith("Zone cible") -> "OK"
            else -> return
        }

        val now = SystemClock.elapsedRealtime()
        val enoughTime = now - lastCueRealtime >= 45_000L
        if (cueState != lastCueState && enoughTime) {
            when (cueState) {
                "LOW" -> speak("Accélère légèrement.")
                "HIGH" -> speak("Ralentis légèrement.")
                "OK" -> speak("Tu es dans ta zone cible. Garde cette allure.")
            }
            lastCueState = cueState
            lastCueRealtime = now
        }
    }

    private fun updateElapsedAndCheckStop() {
        val elapsed = elapsedSeconds()
        val current = RunSessionState.state.value
        RunSessionState.update(current.copy(elapsedSeconds = elapsed))

        if (!min25Announced && targetMinutes >= 30 && elapsed >= 25 * 60) {
            min25Announced = true
            speak("Vingt-cinq minutes atteintes. Si tu te sens bien, continue jusqu'à trente minutes.")
        }

        if (elapsed >= targetMinutes * 60) {
            speak("Séance terminée. Ralentis maintenant et marche quelques minutes pour récupérer.")
            stopRun(manual = false)
        }
    }

    private fun elapsedSeconds(): Int =
        ((SystemClock.elapsedRealtime() - startRealtime) / 1000L).toInt().coerceAtLeast(0)

    override fun onSensorChanged(event: SensorEvent?) {
        if (!running || event == null || event.sensor.type != Sensor.TYPE_STEP_COUNTER) return
        val total = event.values.firstOrNull() ?: return
        if (stepBaseline == null) stepBaseline = total
        currentSteps = (total - (stepBaseline ?: total)).toInt().coerceAtLeast(0)
        val s = RunSessionState.state.value
        RunSessionState.update(s.copy(steps = currentSteps))
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun stopRun(manual: Boolean) {
        if (!running) {
            stopSelf()
            return
        }

        running = false
        handler.removeCallbacks(ticker)
        try { locationManager.removeUpdates(this) } catch (_: Exception) {}
        try { sensorManager.unregisterListener(this) } catch (_: Exception) {}

        val elapsed = elapsedSeconds()
        val distanceKm = distanceMeters / 1000.0
        val avgKmh = if (elapsed > 0) distanceKm / (elapsed / 3600.0) else 0.0
        prefs.saveLastRun(
            distanceKm.toFloat(),
            avgKmh.toFloat(),
            currentSteps,
            elapsed
        )

        val status = if (manual) {
            "Course arrêtée • récupération"
        } else {
            "Objectif atteint • marche quelques minutes"
        }

        RunSessionState.update(
            RunSessionState.state.value.copy(
                running = false,
                elapsedSeconds = elapsed,
                distanceKm = distanceKm,
                steps = currentSteps,
                status = status
            )
        )

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun speak(text: String) {
        if (!ttsReady) return
        textToSpeech?.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "wildsport_run_cue"
        )
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Wildsport • Course",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Suivi GPS et coaching pendant la course"
                }
            )
        }
    }

    private fun buildNotification(text: String): android.app.Notification {
        val openIntent = Intent(this, RunTrackerActivity::class.java)
        val pending = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return androidx.core.app.NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Wildsport Run")
            .setContentText(text)
            .setContentIntent(pending)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun updateNotification(state: RunLiveState) {
        val manager = getSystemService(NotificationManager::class.java)
        val text =
            "%.1f km/h • %.2f km • %02d:%02d".format(
                state.speedKmh,
                state.distanceKm,
                state.elapsedSeconds / 60,
                state.elapsedSeconds % 60
            )
        manager.notify(NOTIFICATION_ID, buildNotification(text))
    }

    override fun onDestroy() {
        handler.removeCallbacks(ticker)
        try { locationManager.removeUpdates(this) } catch (_: Exception) {}
        try { sensorManager.unregisterListener(this) } catch (_: Exception) {}
        textToSpeech?.shutdown()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
