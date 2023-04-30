package dev.justme.busket.me.list.details

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import dev.justme.busket.R
import java.util.Collections


typealias ListClickListener = (entry: ListDetailsRecyclerEntry) -> Unit
typealias ItemMovedListener = (entry: ListDetailsRecyclerEntry, fromPosition: Int, toPosition: Int) -> Unit

data class ListDetailsRecyclerEntry(var checked: Boolean, var name: String, val id: String)


class ListDetailsAdapter(var entries: MutableList<ListDetailsRecyclerEntry>, val onItemMoved: ItemMovedListener, val onItemClicked: ListClickListener, val showItemHandle: Boolean, val startDragListener: StartDragListener?) :
    RecyclerView.Adapter<ListDetailsAdapter.ListDetailsHolder>(), ItemMoveCallback.ItemTouchHelperContract {

    constructor(entries: MutableList<ListDetailsRecyclerEntry>, onItemMoved: ItemMovedListener, onItemClicked: ListClickListener, showItemHandle: Boolean) : this(entries, onItemMoved, onItemClicked, showItemHandle, null)

    class ListDetailsHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val checkBox: CheckBox = itemView.findViewById(R.id.listDetailsItemCheck)
        private val handle: ImageView = itemView.findViewById(R.id.listDetailsItemHandle)
        val card: MaterialCardView = itemView.findViewById(R.id.listDetailsCard)

        @SuppressLint("ClickableViewAccessibility")
        fun bind(entry: ListDetailsRecyclerEntry, onClick: ListClickListener, showHandle: Boolean, startDragListener: StartDragListener?, holder: ListDetailsHolder) {
            if (!showHandle) handle.visibility = View.GONE;
            else {
                handle.setOnTouchListener { _, e ->
                    if (e.action == MotionEvent.ACTION_DOWN) {
                        startDragListener?.requestDrag(holder)
                    }
                    false
                }
            }

            checkBox.text = entry.name
            checkBox.isChecked = entry.checked
            checkBox.setOnClickListener {
                entry.checked = !entry.checked
                onClick.invoke(entry)
            }
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
        holder.bind(item, onItemClicked, showItemHandle, startDragListener, holder)
    }

    override fun onRowMoved(fromPosition: Int, toPosition: Int) {
        onRowMoved(fromPosition, toPosition, true)
    }
    fun onRowMoved(fromPosition: Int, toPosition: Int, invokeEventListener: Boolean) {
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
        if (invokeEventListener) onItemMoved.invoke(entries[toPosition], fromPosition, toPosition)
    }

    override fun onRowSelected(viewHolder: ListDetailsHolder?) {
        viewHolder?.card?.alpha = 0.7f
    }

    override fun onRowClear(viewHolder: ListDetailsHolder?) {
        viewHolder?.card?.alpha = 1f
    }
}