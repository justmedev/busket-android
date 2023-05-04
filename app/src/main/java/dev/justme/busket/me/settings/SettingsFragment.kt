package dev.justme.busket.me.settings

import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.justme.busket.R
import dev.justme.busket.databinding.FragmentSettingsBinding
import dev.justme.busket.feathers.FeathersService
import dev.justme.busket.feathers.FeathersSocket
import org.json.JSONObject

private const val LIST_ID_ARG = "listId"

class SettingsFragment : Fragment() {
    private var listId: String? = null
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val mainThread = Handler(Looper.getMainLooper())

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
            findNavController().navigate(R.id.action_Settings_to_LoginFragment)
        }

        binding.deleteAccountBtn.setOnClickListener {
            areYouSure(R.string.delete_account_notice) { d, _ ->
                feathers.service(FeathersService.Service.USERS).remove(feathers.user?.id ?: -1) { _, err ->
                    if (err != null) {
                        mainThread.post {
                            MaterialAlertDialogBuilder(requireContext()).setCancelable(false).setTitle(R.string.unexpected_error).setMessage(R.string.error_deleting_user).setPositiveButton(R.string.ok) { dialog, _ ->
                                dialog.dismiss()
                            }.show()
                        }
                        return@remove
                    }

                    mainThread.post {
                        d.dismiss()
                        findNavController().navigate(R.id.action_Settings_to_LoginFragment)
                    }
                }
            }
        }

        return binding.root
    }

    private fun areYouSure(message: Int, yesCallback: DialogInterface.OnClickListener) {
        MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.are_you_sure).setMessage(message)
            .setPositiveButton(R.string.yes, yesCallback)
            .setNegativeButton(R.string.cancel) { d, _ -> d.dismiss() }
            .show()
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