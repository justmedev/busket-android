package dev.justme.busket.me.list.details.whitelisted

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import dev.justme.busket.BusketApplication
import dev.justme.busket.R
import dev.justme.busket.databinding.FragmentWhitelistedUsersBinding
import dev.justme.busket.feathers.FeathersService
import dev.justme.busket.feathers.FeathersService.Companion.ARRAY_DATA_KEY
import dev.justme.busket.feathers.FeathersSocket
import org.json.JSONArray
import org.json.JSONObject

private const val ARG_LIST_ID = "listId"

data class WhitelistedUser(
    val id: Int,
    val user: String?,
    val listId: String,
    val inviteEmail: String,
    val canEditEntries: Boolean,
    val canDeleteEntries: Boolean,
) {
    val status: WhitelistedUserStatus
        get() = if (user == null) WhitelistedUserStatus.PENDING_INVITATION else WhitelistedUserStatus.JOINED
    val email: String
        // get() = user?.email ?: inviteEmail
        get() = inviteEmail

    companion object {
        private val gson = Gson()

        fun fromJSONArray(jArray: JSONArray): Array<WhitelistedUser> {
            return Array(jArray.length()) { i -> fromJSON(jArray.getJSONObject(i)) }
        }

        fun fromJSON(jData: JSONObject): WhitelistedUser {
            return gson.fromJson(jData.toString(), WhitelistedUser::class.java)
        }
    }
}

enum class WhitelistedUserStatus(val localized: String) {
    PENDING_INVITATION(BusketApplication.appResources?.getString(R.string.whitelist_pending_invitation) ?: "error"),
    JOINED(BusketApplication.appResources?.getString(R.string.whitelist_joined) ?: "error"),
}

/**
 * A simple [Fragment] subclass.
 * Use the [WhitelistedUsersFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class WhitelistedUsersFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var listId: String? = null
    private lateinit var binding: FragmentWhitelistedUsersBinding
    private lateinit var feathers: FeathersSocket
    private val mainThread = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            listId = it.getString(ARG_LIST_ID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentWhitelistedUsersBinding.inflate(inflater, container, false)
        feathers = FeathersSocket.getInstance(requireContext())

        binding.recyclerView.adapter = WhitelistedUsersAdapter(mutableListOf(), ::onUserClick)
        populate()

        return binding.root
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun populate() {
        val query = JSONObject()
        query.put("listId", listId)
        feathers.service(FeathersService.Service.WHITELISTED_USERS).find(query) { data, error ->
            if (error != null || data == null) return@find

            mainThread.post {
                (binding.recyclerView.adapter as WhitelistedUsersAdapter).users = mutableListOf(*WhitelistedUser.fromJSONArray(data.getJSONArray(ARRAY_DATA_KEY)))
                (binding.recyclerView.adapter as WhitelistedUsersAdapter).notifyDataSetChanged()
            }
        }
    }

    private fun onUserClick(user: WhitelistedUser) {

    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param listId Parameter 1.
         * @return A new instance of fragment WhitelistedUsersFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(listId: String) =
            WhitelistedUsersFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_LIST_ID, listId)
                }
            }
    }
}