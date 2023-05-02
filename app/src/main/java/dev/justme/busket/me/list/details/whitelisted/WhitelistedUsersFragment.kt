package dev.justme.busket.me.list.details.whitelisted

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import dev.justme.busket.BusketApplication
import dev.justme.busket.MainActivity
import dev.justme.busket.R
import dev.justme.busket.databinding.DialogLoadingBinding
import dev.justme.busket.databinding.DialogWhitelistedUserSettingsBinding
import dev.justme.busket.databinding.DialogWithTextfieldBinding
import dev.justme.busket.databinding.FragmentWhitelistedUsersBinding
import dev.justme.busket.feathers.FeathersService
import dev.justme.busket.feathers.FeathersService.Companion.ARRAY_DATA_KEY
import dev.justme.busket.feathers.FeathersSocket
import org.json.JSONArray
import org.json.JSONObject

private const val ARG_LIST_ID = "listId"
private const val ARG_LIST_NAME = "listName"

data class WhitelistedUser(
    val id: Int,
    val user: String?,
    val listId: String,
    val inviteEmail: String,
    var canEditEntries: Boolean,
    var canDeleteEntries: Boolean,
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

data class WhitelistedUserPermissions(
    var canEditEntries: Boolean,
    var canDeleteEntries: Boolean
)

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
    private var listName: String? = null
    private lateinit var binding: FragmentWhitelistedUsersBinding
    private lateinit var feathers: FeathersSocket
    private val mainThread = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            listId = it.getString(ARG_LIST_ID)
            listName = it.getString(ARG_LIST_NAME)
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

        (activity as MainActivity).supportActionBar?.title = "Whitelist: $listName"

        binding.inviteUserFab.setOnClickListener {
            val dialogView = DialogWithTextfieldBinding.inflate(inflater)
            dialogView.textInput.editText?.hint = getString(R.string.email)

            val dialog = MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.invite_user).setView(dialogView.root)
                .setPositiveButton(R.string.invite, null)
                .setNegativeButton(R.string.cancel) { d, _ -> d.dismiss() }
                .show()

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                dialogView.textInput.error = null
                val email = dialogView.textInput.editText?.text.toString()

                if (!email.contains("@") || !email.contains(".")) {
                    dialogView.textInput.error = "Invalid email!"
                    return@setOnClickListener
                }

                val loadingDialog = MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.loading)
                    .setView(DialogLoadingBinding.inflate(inflater).root)
                    .setCancelable(false)
                    .show()

                val data = JSONObject()
                data.put("inviteEmail", email)
                data.put("listId", listId)

                feathers.service(FeathersService.Service.WHITELISTED_USERS).create(data) { res, err ->
                    if (res == null || err != null) return@create
                    mainThread.post {
                        (binding.recyclerView.adapter as WhitelistedUsersAdapter).users.add(WhitelistedUser.fromJSON(res))
                        (binding.recyclerView.adapter as WhitelistedUsersAdapter).notifyItemInserted((binding.recyclerView.adapter as WhitelistedUsersAdapter).users.size - 1)

                        loadingDialog.dismiss()
                        dialog.dismiss()
                    }
                }
            }

            dialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
            dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        }

        feathers.service(FeathersService.Service.WHITELISTED_USERS).on(FeathersService.SocketEventListener.PATCHED) { data, err ->
            if (err != null || data == null) return@on

            val whitelistedUser = WhitelistedUser.fromJSON(data)

            mainThread.post {
                val index = (binding.recyclerView.adapter as WhitelistedUsersAdapter).users.indexOfFirst { it.id == whitelistedUser.id }

                (binding.recyclerView.adapter as WhitelistedUsersAdapter).users[index] = whitelistedUser
                (binding.recyclerView.adapter as WhitelistedUsersAdapter).notifyItemChanged(index)
            }
        }

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

    private fun onUserClick(user: WhitelistedUser, position: Int) {
        val inflater = requireActivity().layoutInflater
        val dialogView = DialogWhitelistedUserSettingsBinding.inflate(inflater)

        val tmpPermissions = WhitelistedUserPermissions(user.canEditEntries, user.canDeleteEntries)

        fun setDialogLoading(loading: Boolean) {
            val contentVisibility = if (loading) View.GONE else View.VISIBLE
            val loaderVisibility = if (loading) View.VISIBLE else View.GONE

            dialogView.kickUserBtn.visibility = contentVisibility
            dialogView.canEditEntriesContainer.visibility = contentVisibility
            dialogView.canDeleteEntriesContainer.visibility = contentVisibility

            dialogView.loadingPermissions.root.visibility = loaderVisibility
        }


        val permissionDialog = MaterialAlertDialogBuilder(requireContext()).setTitle(getString(R.string.permission_setting_title, user.email)).setView(dialogView.root)
            .setPositiveButton(R.string.save) { d, _ ->
                setDialogLoading(true)

                val patchData = JSONObject()
                patchData.put("canEditEntries", tmpPermissions.canEditEntries)
                patchData.put("canDeleteEntries", tmpPermissions.canDeleteEntries)

                feathers.service(FeathersService.Service.WHITELISTED_USERS).patch(user.id, patchData) { data, err ->
                    if (err != null || data == null) return@patch

                    user.canEditEntries = tmpPermissions.canEditEntries
                    user.canDeleteEntries = tmpPermissions.canDeleteEntries

                    mainThread.post {
                        (binding.recyclerView.adapter as WhitelistedUsersAdapter).notifyItemChanged(position)

                        setDialogLoading(false)
                        d.dismiss()
                    }
                }
            }
            .setNegativeButton(R.string.cancel) { d, _ -> d.dismiss() }
            .show()

        dialogView.canEditEntriesCheckbox.isChecked = tmpPermissions.canEditEntries
        dialogView.canEditEntriesCheckbox.setOnClickListener { tmpPermissions.canEditEntries = !tmpPermissions.canEditEntries }
        dialogView.canEditEntriesContainer.setOnClickListener {
            tmpPermissions.canEditEntries = !tmpPermissions.canEditEntries
            dialogView.canEditEntriesCheckbox.isChecked = tmpPermissions.canEditEntries
        }

        dialogView.canDeleteEntriesCheckbox.isChecked = tmpPermissions.canDeleteEntries
        dialogView.canDeleteEntriesCheckbox.setOnClickListener { tmpPermissions.canDeleteEntries = !tmpPermissions.canDeleteEntries }
        dialogView.canDeleteEntriesContainer.setOnClickListener {
            tmpPermissions.canDeleteEntries = !tmpPermissions.canDeleteEntries
            dialogView.canDeleteEntriesCheckbox.isChecked = tmpPermissions.canDeleteEntries
        }

        dialogView.kickUserBtn.setOnClickListener {
            setDialogLoading(true)
            MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.are_you_sure).setMessage(R.string.permission_setting_kick_user_body)
                .setPositiveButton(R.string.yes) { d, _ ->
                    feathers.service(FeathersService.Service.WHITELISTED_USERS).remove(user.id) { data, err ->
                        if (data == null || err != null) return@remove
                        mainThread.post {
                            (binding.recyclerView.adapter as WhitelistedUsersAdapter).users.removeAt(position)
                            (binding.recyclerView.adapter as WhitelistedUsersAdapter).notifyItemRemoved(position)

                            setDialogLoading(false)
                            d.dismiss()
                            permissionDialog.dismiss()
                        }
                    }
                }
                .setNegativeButton(R.string.cancel) { d, _ ->
                    setDialogLoading(false)
                    d.dismiss()
                }
                .setOnDismissListener {
                    setDialogLoading(false)
                }
                .show()
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param listId Parameter 1.
         * @param listName Parameter 1.
         * @return A new instance of fragment WhitelistedUsersFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(listId: String, listName: String) =
            WhitelistedUsersFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_LIST_ID, listId)
                    putString(ARG_LIST_NAME, listName)
                }
            }
    }
}