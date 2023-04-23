package dev.justme.busket.feathers.responses

import com.google.gson.annotations.SerializedName
import java.util.*

class ShoppingListItems(
    val items: List<ShoppingListItem>
)

class ShoppingListItem(
    val id: String,
    val done: Boolean,
    val name: String,
)

class ShoppingList(
    @SerializedName("listid") val listId: String,
    val name: String,
    val description: String,
    val owner: String,
    val entries: ShoppingListItems,
    val checkedEntries: ShoppingListItems,
)

data class ShoppingListResponse(
    val id: Int,
    val updatedAt: Date,
    val createdAt: Date,

    val backgroundURI: String,
    @SerializedName("listid") val listId: String,
    val name: String,
    val description: String,
    val owner: String,
    val entries: ShoppingListItems,
    val checkedEntries: ShoppingListItems,
)
