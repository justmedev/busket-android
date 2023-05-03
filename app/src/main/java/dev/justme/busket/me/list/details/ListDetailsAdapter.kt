package dev.justme.busket.me.list.details

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.justme.busket.databinding.ListDetailsItemBinding
import dev.justme.busket.me.list.details.whitelisted.WhitelistedUserPermissions
import java.util.Collections


typealias ListClickListener = (entry: ListDetailsRecyclerEntry) -> Unit
typealias ItemMovedListener = (entry: ListDetailsRecyclerEntry, fromPosition: Int, toPosition: Int) -> Unit

data class ListDetailsRecyclerEntry(var checked: Boolean, var name: String, val id: String)


class ListDetailsAdapter(var entries: MutableList<ListDetailsRecyclerEntry>, val onItemMove: ItemMovedListener, val onItemCheck: ListClickListener, val onItemLongPress: ListClickListener, val showItemHandle: Boolean, var permissions: WhitelistedUserPermissions, val startDragListener: StartDragListener?) :
    RecyclerView.Adapter<ListDetailsAdapter.ListDetailsHolder>(), ItemMoveCallback.ItemTouchHelperContract {

    constructor(entries: MutableList<ListDetailsRecyclerEntry>, onItemMoved: ItemMovedListener, onItemCheck: ListClickListener, onItemLongPress: ListClickListener, showItemHandle: Boolean, permissions: WhitelistedUserPermissions)
            : this(entries, onItemMoved, onItemCheck, onItemLongPress, showItemHandle, permissions, null)

    lateinit var binding: ListDetailsItemBinding

    class ListDetailsHolder(binding: ListDetailsItemBinding) : RecyclerView.ViewHolder(binding.root) {
        val checkBox = binding.listDetailsItemCheck
        val handle = binding.listDetailsItemHandle
        val card = binding.listDetailsCard

        @SuppressLint("ClickableViewAccessibility")
        fun bind(entry: ListDetailsRecyclerEntry, onCheckClick: ListClickListener, onLongClick: ListClickListener, showHandle: Boolean, permissions: WhitelistedUserPermissions, startDragListener: StartDragListener?) {
            fun longPressListener(v: View): Boolean {
                onLongClick.invoke(entry)
                return true
            }

            if (!showHandle) handle.visibility = View.GONE;
            else {
                handle.setOnTouchListener { _, e ->
                    if (e.action == MotionEvent.ACTION_DOWN) startDragListener?.requestDrag(this)
                    false
                }
            }

            card.setOnLongClickListener(::longPressListener)
            checkBox.setOnLongClickListener(::longPressListener)

            checkBox.text = entry.name
            checkBox.isChecked = entry.checked
            checkBox.setOnClickListener {
                entry.checked = !entry.checked
                onCheckClick.invoke(entry)
            }

            checkBox.isEnabled = permissions.canEditEntries
            handle.visibility = if (showHandle && permissions.canEditEntries) View.VISIBLE else View.INVISIBLE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListDetailsHolder {
        val binding = ListDetailsItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ListDetailsHolder(binding)
    }

    override fun getItemCount(): Int {
        return entries.size
    }

    override fun onBindViewHolder(holder: ListDetailsHolder, position: Int) {
        val item = entries[position]
        Log.d(javaClass.simpleName, "onBindViewHolder: canEditEntries ${permissions.canEditEntries}; canDeleteEntries ${permissions.canDeleteEntries}")
        holder.bind(item, onItemCheck, onItemLongPress, showItemHandle, permissions, startDragListener)
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
        if (invokeEventListener) onItemMove.invoke(entries[toPosition], fromPosition, toPosition)
    }

    override fun onRowSelected(viewHolder: ListDetailsHolder?) {
        viewHolder?.card?.alpha = 0.7f
    }

    override fun onRowClear(viewHolder: ListDetailsHolder?) {
        viewHolder?.card?.alpha = 1f
    }
}