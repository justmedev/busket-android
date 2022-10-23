package dev.justme.busket.feathers.responses

import com.google.gson.annotations.SerializedName
import java.util.*

class ShoppingList(
    @SerializedName("listid") val listId: String,
    val name: String,
    val description: String,
    val owner: String,
    val entries: List<String>
) {
}

data class ShoppingListResponse(
    val id: Int,
    val updatedAt: Date,
    val createdAt: Date,

    val backgroundURI: String,
    @SerializedName("listid") val listId: String,
    val name: String,
    val description: String,
    val owner: String,
    val entries: List<String>,
)
