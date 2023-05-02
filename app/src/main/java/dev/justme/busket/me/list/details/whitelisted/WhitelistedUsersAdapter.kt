package dev.justme.busket.me.list.details.whitelisted

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.justme.busket.databinding.WhitelistedUsersItemBinding

typealias UserClickListener = (user: WhitelistedUser) -> Unit

class WhitelistedUsersAdapter(var users: MutableList<WhitelistedUser>, val onUserClick: UserClickListener) :
    RecyclerView.Adapter<WhitelistedUsersAdapter.WhitelistedUsersHolder>() {

    lateinit var binding: WhitelistedUsersItemBinding

    class WhitelistedUsersHolder(binding: WhitelistedUsersItemBinding) : RecyclerView.ViewHolder(binding.root) {
        val email = binding.whitelistedUsersEmail
        val status = binding.whitelistedUsersStatus
        val editBtn = binding.whitelistedUsersEditButton

        fun bind(user: WhitelistedUser, onClick: UserClickListener) {
            email.text = user.email
            status.text = user.status.localized

            editBtn.setOnClickListener {
                onClick.invoke(user)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WhitelistedUsersHolder {
        val binding = WhitelistedUsersItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WhitelistedUsersHolder(binding)
    }

    override fun getItemCount(): Int {
        return users.size
    }

    override fun onBindViewHolder(holder: WhitelistedUsersHolder, position: Int) {
        val item = users[position]
        holder.bind(item, onUserClick)
    }
}