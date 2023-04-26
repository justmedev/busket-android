package dev.justme.busket.me.list.details

import android.content.Context
import android.util.Log
import dev.justme.busket.feathers.FeathersSocket
import dev.justme.busket.feathers.responses.ShoppingList
import org.json.JSONArray
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Arrays
import java.util.UUID

/*
export interface EventData {
  event: EventType,
  entryId: string,
  sender?: string,
  isoDate: string,
  state: {
    name: string,
    /**
     * @deprecated done is deprecated since 23.11.2022. Just emit with {@link EventType} set to {@link EventType.MARK_ENTRY_DONE} or {@link EventType.MARK_ENTRY_TODO}
     */
    done?: boolean,
    oldIndex?: number,
    newIndex?: number,
  },
}

export interface LogEvent {
  listid: string,
  eventData: EventData
}
 */

enum class ShoppingListEventType(name: String) {
    MOVE_ENTRY("MOVE_ENTRY"),
    DELETE_ENTRY("DELETE_ENTRY"),
    CREATE_ENTRY("CREATE_ENTRY"),
    CHANGED_ENTRY_NAME("CHANGED_ENTRY_NAME"),
    MARK_ENTRY_DONE("MARK_ENTRY_DONE"),
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

class SyncListDetailsManager(val context: Context, val list: ShoppingList) {
    private val feathers = FeathersSocket.getInstance(context)
    private val eventQueue: MutableList<ShoppingListEvent> = mutableListOf()
    private val sessionUUID = UUID.randomUUID().toString()

    fun recordEvent(event: ShoppingListEventType, entryId: String, eventState: ShoppingListEventState) {
        if (list.listId == null) return

        val offsetDate = OffsetDateTime.of(LocalDateTime.now(), ZoneOffset.UTC)
        val isoDate = offsetDate.format(DateTimeFormatter.ISO_DATE_TIME)

        val eventData = ShoppingListEventData(event, entryId, sessionUUID, isoDate, eventState)
        eventQueue.add(ShoppingListEvent(list.listId, eventData))

        sendEventsToServer()
    }

    private fun sendEventsToServer() {
        val curHandling = eventQueue.toMutableList()
        eventQueue.clear()
        feathers.service(FeathersSocket.Service.EVENT, FeathersSocket.Method.CREATE, JSONArray(feathers.gson.toJson(curHandling))) { data, err ->
            if (data == null || err != null) {
                eventQueue.addAll(curHandling)
                return@service
            }
        }
    }
}