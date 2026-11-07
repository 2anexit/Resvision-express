package fr.example.rvisionscours

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.garmin.android.connectiq.ConnectIQ
import com.garmin.android.connectiq.IQApp
import com.garmin.android.connectiq.IQDevice
import com.garmin.android.connectiq.exception.InvalidStateException
import com.garmin.android.connectiq.exception.ServiceUnavailableException

class GarminWatchManager(
    context: Context,
    private val listener: Listener
) {

    interface Listener {
        fun onGarminStatusChanged(status: GarminStatus)
        fun onGarminAnswerReceived(questionIndex: Int, answerIndex: Int)
    }

    data class GarminStatus(
        val ready: Boolean,
        val messageType: GarminStatusMessage,
        val deviceName: String? = null
    )

    enum class GarminStatusMessage {
        OFF,
        INITIALIZING,
        READY,
        NO_DEVICE,
        SDK_UNAVAILABLE,
        APP_MISSING
    }

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val connectIQ = ConnectIQ.getInstance(appContext, ConnectIQ.IQConnectType.WIRELESS)
    private val watchApp = IQApp(GARMIN_APP_ID)

    private var isSdkReady = false
    private var currentDevice: IQDevice? = null
    private var currentStatus = GarminStatus(false, GarminStatusMessage.INITIALIZING)
    private var appEventsRegistered = false

    private val sdkListener = object : ConnectIQ.ConnectIQListener {
        override fun onInitializeError(errorStatus: ConnectIQ.IQSdkErrorStatus) {
            isSdkReady = false
            publishStatus(GarminStatus(false, GarminStatusMessage.SDK_UNAVAILABLE))
        }

        override fun onSdkReady() {
            isSdkReady = true
            refreshStatus()
        }

        override fun onSdkShutDown() {
            isSdkReady = false
            currentDevice = null
            publishStatus(GarminStatus(false, GarminStatusMessage.SDK_UNAVAILABLE))
        }
    }

    fun initialize() {
        publishStatus(GarminStatus(false, GarminStatusMessage.INITIALIZING))
        connectIQ.initialize(appContext, true, sdkListener)
    }

    fun shutdown() {
        try {
            connectIQ.unregisterAllForEvents()
            connectIQ.shutdown(appContext)
        } catch (_: InvalidStateException) {
        }
    }

    fun refreshStatus() {
        if (!isSdkReady) {
            publishStatus(GarminStatus(false, GarminStatusMessage.INITIALIZING))
            return
        }

        currentDevice = findBestDevice()
        val device = currentDevice
        if (device == null) {
            publishStatus(GarminStatus(false, GarminStatusMessage.NO_DEVICE))
            return
        }

        registerForDeviceEvents(device)
        checkWatchApp(device) { installed ->
            if (installed) {
                publishStatus(
                    GarminStatus(
                        ready = true,
                        messageType = GarminStatusMessage.READY,
                        deviceName = device.friendlyNameSafe()
                    )
                )
            } else {
                publishStatus(GarminStatus(false, GarminStatusMessage.APP_MISSING, device.friendlyNameSafe()))
            }
        }
    }

    fun prepareWatchSession(onReady: (Boolean) -> Unit) {
        if (!isSdkReady) {
            publishStatus(GarminStatus(false, GarminStatusMessage.INITIALIZING))
            onMain { onReady(false) }
            return
        }

        val device = findBestDevice()
        if (device == null) {
            publishStatus(GarminStatus(false, GarminStatusMessage.NO_DEVICE))
            onMain { onReady(false) }
            return
        }

        currentDevice = device
        registerForDeviceEvents(device)
        registerForAppEvents(device)

        checkWatchApp(device) { installed ->
            if (!installed) {
                publishStatus(GarminStatus(false, GarminStatusMessage.APP_MISSING, device.friendlyNameSafe()))
                onMain { onReady(false) }
                return@checkWatchApp
            }

            publishStatus(GarminStatus(false, GarminStatusMessage.INITIALIZING, device.friendlyNameSafe()))
            try {
                connectIQ.openApplication(device, watchApp) { _, _, status ->
                    publishStatus(
                        GarminStatus(
                            ready = true,
                            messageType = GarminStatusMessage.READY,
                            deviceName = device.friendlyNameSafe()
                        )
                    )
                    onMain { onReady(true) }
                }
            } catch (_: InvalidStateException) {
                publishStatus(GarminStatus(false, GarminStatusMessage.SDK_UNAVAILABLE, device.friendlyNameSafe()))
                onMain { onReady(false) }
            } catch (_: ServiceUnavailableException) {
                publishStatus(GarminStatus(false, GarminStatusMessage.SDK_UNAVAILABLE, device.friendlyNameSafe()))
                onMain { onReady(false) }
            }
        }
    }

    fun syncIdleState() {
        sendPayload(
            mapOf(
                "type" to "idle"
            )
        )
    }

    fun syncQuestionState(
        category: String,
        questionIndex: Int,
        totalQuestions: Int,
        question: String,
        options: List<String>,
        remainingMillis: Long,
        feedback: String,
        selectedIndex: Int?,
        correctIndex: Int?,
        answerLocked: Boolean
    ) {
        sendPayload(
            mapOf(
                "type" to "question",
                "category" to category,
                "questionIndex" to questionIndex,
                "totalQuestions" to totalQuestions,
                "question" to question,
                "option1" to options.getOrElse(0) { "" },
                "option2" to options.getOrElse(1) { "" },
                "option3" to options.getOrElse(2) { "" },
                "option4" to options.getOrElse(3) { "" },
                "remainingMillis" to remainingMillis,
                "feedback" to feedback,
                "selectedIndex" to (selectedIndex ?: -1),
                "correctIndex" to (correctIndex ?: -1),
                "answerLocked" to answerLocked
            )
        )
    }

    fun syncResultState(
        category: String,
        score: Int,
        totalQuestions: Int,
        timedOut: Boolean
    ) {
        sendPayload(
            mapOf(
                "type" to "result",
                "category" to category,
                "score" to score,
                "totalQuestions" to totalQuestions,
                "timedOut" to timedOut
            )
        )
    }

    fun getCurrentStatus(): GarminStatus = currentStatus

    private fun sendPayload(payload: Map<String, Any>) {
        val device = currentDevice ?: return
        if (!currentStatus.ready) {
            return
        }

        try {
            registerForAppEvents(device)
            connectIQ.sendMessage(device, watchApp, payload) { _, _, _ -> }
        } catch (_: InvalidStateException) {
            publishStatus(GarminStatus(false, GarminStatusMessage.SDK_UNAVAILABLE, device.friendlyNameSafe()))
        } catch (_: ServiceUnavailableException) {
            publishStatus(GarminStatus(false, GarminStatusMessage.SDK_UNAVAILABLE, device.friendlyNameSafe()))
        }
    }

    private fun findBestDevice(): IQDevice? {
        return try {
            val connected = connectIQ.connectedDevices?.firstOrNull()
            if (connected != null) {
                connected
            } else {
                connectIQ.knownDevices?.firstOrNull()
            }
        } catch (_: InvalidStateException) {
            null
        } catch (_: ServiceUnavailableException) {
            publishStatus(GarminStatus(false, GarminStatusMessage.SDK_UNAVAILABLE))
            null
        }
    }

    private fun registerForDeviceEvents(device: IQDevice) {
        try {
            connectIQ.registerForDeviceEvents(device) { _, status ->
                if (status.name == "CONNECTED") {
                    publishStatus(GarminStatus(true, GarminStatusMessage.READY, device.friendlyNameSafe()))
                } else {
                    publishStatus(GarminStatus(false, GarminStatusMessage.NO_DEVICE, device.friendlyNameSafe()))
                }
            }
        } catch (_: InvalidStateException) {
            publishStatus(GarminStatus(false, GarminStatusMessage.SDK_UNAVAILABLE))
        }
    }

    private fun registerForAppEvents(device: IQDevice) {
        if (appEventsRegistered) {
            return
        }

        try {
            connectIQ.registerForAppEvents(device, watchApp) { _, _, message, _ ->
                val payload = extractPayload(message)
                if (payload["type"]?.toString() != "answer") {
                    return@registerForAppEvents
                }

                val questionIndex = payload["questionIndex"]?.toString()?.toIntOrNull() ?: return@registerForAppEvents
                val answerIndex = payload["answerIndex"]?.toString()?.toIntOrNull() ?: return@registerForAppEvents
                onMain {
                    listener.onGarminAnswerReceived(questionIndex, answerIndex)
                }
            }
            appEventsRegistered = true
        } catch (_: InvalidStateException) {
            publishStatus(GarminStatus(false, GarminStatusMessage.SDK_UNAVAILABLE))
        } catch (_: ServiceUnavailableException) {
            publishStatus(GarminStatus(false, GarminStatusMessage.SDK_UNAVAILABLE))
        }
    }

    private fun extractPayload(message: List<Any>?): Map<*, *> {
        if (message.isNullOrEmpty()) {
            return emptyMap<String, Any>()
        }

        val firstEntry = message.firstOrNull()
        return when (firstEntry) {
            is Map<*, *> -> firstEntry
            else -> emptyMap<String, Any>()
        }
    }

    private fun checkWatchApp(device: IQDevice, onResult: (Boolean) -> Unit) {
        try {
            connectIQ.getApplicationInfo(
                GARMIN_APP_ID,
                device,
                object : ConnectIQ.IQApplicationInfoListener {
                    override fun onApplicationInfoReceived(app: IQApp) {
                        onMain { onResult(true) }
                    }

                    override fun onApplicationNotInstalled(applicationId: String) {
                        onMain { onResult(false) }
                    }
                }
            )
        } catch (_: InvalidStateException) {
            publishStatus(GarminStatus(false, GarminStatusMessage.SDK_UNAVAILABLE, device.friendlyNameSafe()))
            onMain { onResult(false) }
        } catch (_: ServiceUnavailableException) {
            publishStatus(GarminStatus(false, GarminStatusMessage.SDK_UNAVAILABLE, device.friendlyNameSafe()))
            onMain { onResult(false) }
        }
    }

    private fun publishStatus(status: GarminStatus) {
        currentStatus = status
        onMain {
            listener.onGarminStatusChanged(status)
        }
    }

    private fun onMain(action: () -> Unit) {
        mainHandler.post(action)
    }

    private fun IQDevice.friendlyNameSafe(): String {
        return getFriendlyName() ?: "Garmin"
    }

    companion object {
        const val GARMIN_APP_ID = "0c7844da83ae4977aa6000fffdccceb3"
    }
}
