package dev.justme.busket.me.list.details

class EntryNotFound(entryId: String): Exception("Entry with id $entryId was not found!")

enum class ListType {
    TODO,
    DONE
}

data class FoundEntry(
    val index: Int,
    val list: ListType?,
)