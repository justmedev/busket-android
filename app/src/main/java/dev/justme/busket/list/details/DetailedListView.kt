package dev.justme.busket.list.details

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dev.justme.busket.R

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_LIST_ID = "listId"

class DetailedListView : Fragment() {
    // TODO: Rename and change types of parameters
    private var listId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            listId = it.getString(ARG_LIST_ID)
        }

        Log.d("Busket DetailedListView", "onCreate.arguments.listId: $listId")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_detailed_list_view, container, false)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param listId The id of the list to load.
         * @return A new instance of fragment DetailedListView.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(listId: String) =
            DetailedListView().apply {
                arguments = Bundle().apply {
                    putString(ARG_LIST_ID, listId)
                }
            }
    }
}