package dev.justme.busket.me.list.details

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import dev.justme.busket.R
import java.util.Collections


typealias ListClickListener = (v: View?, entryId: String) -> Unit

data class ListDetailsRecyclerEntry(val checked: Boolean, val name: String, val id: String)

data class ListItemDetails(val entry: ListDetailsRecyclerEntry, val onClick: ListClickListener)

class ListDetailsAdapter(var entries: MutableList<ListItemDetails>) :
    RecyclerView.Adapter<ListDetailsAdapter.ListDetailsHolder>(), ItemMoveCallback.ItemTouchHelperContract {
    class ListDetailsHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val checkBox: CheckBox = itemView.findViewById(R.id.listDetailsItemCheck)
        val card: MaterialCardView = itemView.findViewById(R.id.listDetailsCard)

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

    override fun onRowMoved(fromPosition: Int, toPosition: Int) {
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                Collections.swap(entries, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(entries, i, i - 1)
            }
        }
        notifyItemMoved(fromPosition, toPosition)
    }

    override fun onRowSelected(viewHolder: ListDetailsHolder?) {
        viewHolder?.card?.alpha = 0.7f
    }

    override fun onRowClear(viewHolder: ListDetailsHolder?) {
        viewHolder?.card?.alpha = 1f
    }
}