package dev.justme.busket.me.settings

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.justme.busket.R
import dev.justme.busket.databinding.FragmentSettingsBinding
import dev.justme.busket.feathers.FeathersSocket
import org.json.JSONObject

private const val LIST_ID_ARG = "listId"

class SettingsFragment : Fragment() {
    private var listId: String? = null
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            listId = it.getString(LIST_ID_ARG)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater)

        val feathers = FeathersSocket.getInstance(requireContext())
        binding.logoutBtn.setOnClickListener {
            feathers.logout()
        }

        binding.deleteAccountBtn.setOnClickListener {
            feathers.service(FeathersSocket.Service.USERS, FeathersSocket.Method.REMOVE, JSONObject()) { data, err ->
                if (err != null) {
                    MaterialAlertDialogBuilder(requireContext()).setCancelable(false).setTitle(R.string.unexpected_error).setMessage(R.string.error_deleting_user).setPositiveButton(R.string.ok) { dialog, _ ->
                        dialog.dismiss()
                    }
                }
            }
        }

        return binding.root
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param listId The id of the list to load.
         * @return A new instance of fragment SettingsFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(listId: String) =
            SettingsFragment().apply {
                arguments = Bundle().apply {
                    putString(LIST_ID_ARG, listId)
                }
            }
    }
}