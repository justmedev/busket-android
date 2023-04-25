package dev.justme.busket.me.list.details

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import dev.justme.busket.R

typealias ListClickListener = (v: View?, entryId: String) -> Unit

data class ListDetailsRecyclerEntry(val checked: Boolean, val name: String, val id: String)

data class ListItemDetails(val entry: ListDetailsRecyclerEntry, val onClick: ListClickListener)

class ListDetailsAdapter(var entries: Array<ListItemDetails>) :
    RecyclerView.Adapter<ListDetailsAdapter.ListDetailsHolder>() {
    class ListDetailsHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val checkBox: CheckBox = itemView.findViewById(R.id.listDetailsItemCheck)

        fun bind(entry: ListDetailsRecyclerEntry, onClick: ListClickListener) {
            checkBox.text = entry.name
            checkBox.isChecked = entry.checked
            checkBox.setOnClickListener { onClick.invoke(it, entry.id) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListDetailsHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_details_item, parent, false)

        return ListDetailsHolder(view)
    }

    override fun getItemCount(): Int {
        return entries.size
    }

    override fun onBindViewHolder(holder: ListDetailsHolder, position: Int) {
        val item = entries[position]
        holder.bind(item.entry, item.onClick)
    }
}