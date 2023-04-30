package dev.justme.busket.me.list.details.whitelisted

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dev.justme.busket.R

private const val ARG_LIST_ID = "listId"

/**
 * A simple [Fragment] subclass.
 * Use the [WhitelistedUsersFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class WhitelistedUsersFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var listId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            listId = it.getString(ARG_LIST_ID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_whitelisted_users, container, false)
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