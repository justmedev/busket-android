package dev.justme.busket

import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

data class ListOverview(val title: String, val subtitle: String, val onClick: OnClickListener)

class ListOverviewAdapter(val lists: Array<ListOverview>) :
    RecyclerView.Adapter<ListOverviewAdapter.ListOverviewHolder>() {
    class ListOverviewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val listTitle: TextView = itemView.findViewById(R.id.list_overview_item_title)
        private val listSubtitle: TextView = itemView.findViewById(R.id.list_overview_item_subtitle)
        private val listOverviewCard: CardView = itemView.findViewById(R.id.list_overview_card)

        fun bind(title: String, subtitle: String, onClick: OnClickListener) {
            listTitle.text = title
            listSubtitle.text = subtitle
            listOverviewCard.setOnClickListener(onClick)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListOverviewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_overview_item, parent, false)

        return ListOverviewHolder(view)
    }

    override fun getItemCount(): Int {
        return lists.size
    }

    override fun onBindViewHolder(holder: ListOverviewHolder, position: Int) {
        val item = lists[position]
        holder.bind(item.title, item.subtitle, item.onClick)
    }
}