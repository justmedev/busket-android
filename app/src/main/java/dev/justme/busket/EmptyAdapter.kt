package dev.justme.busket

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class EmptyAdapter :
    RecyclerView.Adapter<EmptyAdapter.EmptyHolder>() {
    class EmptyHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmptyHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_overview_item, parent, false)

        return EmptyHolder(view)
    }

    override fun getItemCount(): Int {
        return 0
    }

    override fun onBindViewHolder(holder: EmptyHolder, position: Int) {}
}