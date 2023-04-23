package dev.justme.busket

import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import dev.justme.busket.feathers.responses.ShoppingList

data class ListOverview(val shoppingList: ShoppingList, val onClick: OnClickListener)

class ListOverviewAdapter(val lists: Array<ListOverview>) :
    RecyclerView.Adapter<ListOverviewAdapter.ListOverviewHolder>() {
    class ListOverviewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val listTitle: TextView = itemView.findViewById(R.id.list_overview_item_title)
        private val listSubtitle: TextView = itemView.findViewById(R.id.list_overview_item_subtitle)
        private val listOverviewCard: CardView = itemView.findViewById(R.id.list_overview_card)

        fun bind(shoppingList: ShoppingList, onClick: OnClickListener) {
            listTitle.text = shoppingList.name
            listSubtitle.text = shoppingList.description
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
        holder.bind(item.shoppingList, item.onClick)
    }
}