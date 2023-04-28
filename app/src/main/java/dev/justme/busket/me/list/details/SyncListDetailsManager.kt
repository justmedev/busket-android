package dev.justme.busket.me.list.details

import android.content.Context
import android.os.Looper
import android.util.Log
import com.google.gson.annotations.SerializedName
import dev.justme.busket.feathers.FeathersService
import dev.justme.busket.feathers.FeathersSocket
import dev.justme.busket.feathers.responses.ShoppingList
import org.json.JSONArray
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID

enum class ShoppingListEventType(name: String) {
    @SerializedName("MOVE_ENTRY")
    MOVE_ENTRY("MOVE_ENTRY"),

    @SerializedName("DELETE_ENTRY")
    DELETE_ENTRY("DELETE_ENTRY"),

    @SerializedName("CREATE_ENTRY")
    CREATE_ENTRY("CREATE_ENTRY"),

    @SerializedName("CHANGED_ENTRY_NAME")
    CHANGED_ENTRY_NAME("CHANGED_ENTRY_NAME"),

    @SerializedName("MARK_ENTRY_DONE")
    MARK_ENTRY_DONE("MARK_ENTRY_DONE"),

    @SerializedName("MARK_ENTRY_TODO")
    MARK_ENTRY_TODO("MARK_ENTRY_TODO"),
}

data class ShoppingListEventState(
    val name: String,
    val oldIndex: Int?,
    val newIndex: Int?,
)

data class ShoppingListEventData(
    val event: ShoppingListEventType,
    val entryId: String,
    val sender: String?,
    val isoDate: String,
    val state: ShoppingListEventState
)

data class ShoppingListEvent(
    val listid: String,
    val eventData: ShoppingListEventData
)


typealias ShoppingListEventListener = (event: ShoppingListEvent) -> Unit;

data class ShoppingListEventListeners(
    val created: ShoppingListEventListener,
    val moved: ShoppingListEventListener,
    val deleted: ShoppingListEventListener,
    val renamed: ShoppingListEventListener,
    val markedAsTodo: ShoppingListEventListener,
    val markedAsDone: ShoppingListEventListener,
)

class SyncListDetailsManager(val context: Context, val list: ShoppingList) {
    private val feathers = FeathersSocket.getInstance(context)
    private val sessionUUID = UUID.randomUUID().toString()
    private val eventQueue: MutableList<ShoppingListEvent> = mutableListOf()
    private var eventListenerAttached = false
    private var eventListeners: ShoppingListEventListeners? = null
    private val handler = android.os.Handler(Looper.getMainLooper())

    fun registerEventListener(listeners: ShoppingListEventListeners) {
        if (eventListenerAttached) {
            Log.w("Busket ${javaClass.simpleName}", "EventListener for Sync is already attached! Cannot attach twice")
            return
        }
        eventListenerAttached = true
        eventListeners = listeners

        feathers.service(FeathersService.Service.EVENT).on(FeathersService.SocketEventListener.CREATED) { data, err ->
            if (err != null) return@on // TODO: Handle error
            val event: ShoppingListEvent = feathers.gson.fromJson(data.toString(), ShoppingListEvent::class.java)

            Log.d("SyncListDetailsManager Event", "${event.eventData.event} executed by ${event.eventData.sender} at ${event.eventData.isoDate} on entry ${event.eventData.entryId}")
            if (event.listid != list.listId) {
                Log.d("SyncListDetailsManager Event", "${event.eventData.event} is being ignored because it wasn't for the currently open list!")
                return@on// Event was for other list (not the one we are on)}
            }

            if (event.eventData.sender == sessionUUID) {
                Log.d("SyncListDetailsManager Event", "${event.eventData.event} executed by us on entry ${event.eventData.entryId} is ignored by syncManager because its already handled")
                return@on
            }

            handler.post {
                when (event.eventData.event) {
                    ShoppingListEventType.CREATE_ENTRY -> eventListeners?.created?.invoke(event)
                    ShoppingListEventType.MOVE_ENTRY -> eventListeners?.moved?.invoke(event)
                    ShoppingListEventType.DELETE_ENTRY -> eventListeners?.deleted?.invoke(event)
                    ShoppingListEventType.CHANGED_ENTRY_NAME -> eventListeners?.renamed?.invoke(event)
                    ShoppingListEventType.MARK_ENTRY_DONE -> eventListeners?.markedAsDone?.invoke(event)
                    ShoppingListEventType.MARK_ENTRY_TODO -> eventListeners?.markedAsTodo?.invoke(event)
                }
            }
        }
    }

    fun recordEvent(event: ShoppingListEventType, entryId: String, eventState: ShoppingListEventState) {
        recordEvent(event, entryId, eventState, true)
    }

    fun recordEvent(event: ShoppingListEventType, entryId: String, eventState: ShoppingListEventState, sendToServer: Boolean) {
        if (list.listId == null) return

        val offsetDate = OffsetDateTime.of(LocalDateTime.now(), ZoneOffset.UTC)
        val isoDate = offsetDate.format(DateTimeFormatter.ISO_DATE_TIME)

        val eventData = ShoppingListEventData(event, entryId, sessionUUID, isoDate, eventState)
        eventQueue.add(ShoppingListEvent(list.listId, eventData))

        if (sendToServer) sendEventsToServer()
    }

    fun sendEventsToServer() {
        val curHandling = eventQueue.toMutableList()
        eventQueue.clear()
        feathers.service(FeathersService.Service.EVENT).create(JSONArray(feathers.gson.toJson(curHandling))) { data, err ->
            if (data == null || err != null) {
                eventQueue.addAll(curHandling)
                return@create
            }
        }
    }
}