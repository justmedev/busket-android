package dev.justme.busket.me.list.details

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import dev.justme.busket.MainActivity
import dev.justme.busket.R
import dev.justme.busket.databinding.FragmentDetailedListViewBinding
import dev.justme.busket.feathers.FeathersSocket
import dev.justme.busket.feathers.responses.ShoppingList
import org.json.JSONObject

private const val ARG_LIST_ID = "listId"

class DetailedListView : Fragment() {
    private var _binding: FragmentDetailedListViewBinding? = null
    private val binding get() = _binding!!

    private val handler = Handler(Looper.getMainLooper())

    private var listId: String? = null
    private var list: ShoppingList? = null
    private lateinit var feathers: FeathersSocket

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            listId = it.getString(ARG_LIST_ID)
        }
    }

    private fun loadListFromRemote(cb: () -> Unit) {
        val query = JSONObject()
        query.put("listid", listId)
        feathers.service(FeathersSocket.Service.LIST, FeathersSocket.Method.FIND, query) { data, err ->
            if (data == null || err != null) return@service

            val arr = data.getJSONArray(FeathersSocket.ARRAY_DATA_KEY)
            if (arr.length() <= 0) return@service // TODO TRIGGER NOT FOUND

            list = ShoppingList.fromJSONObject(arr.getJSONObject(0))
            handler.post {
                cb.invoke()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailedListViewBinding.inflate(inflater)
        setupMenu()

        feathers = FeathersSocket.getInstance(requireContext())
        Log.d("Busket DetailedListView", "onCreate.arguments.listId: $listId")

        binding.listContainer.visibility = View.GONE
        binding.listLoader.visibility = View.VISIBLE
        loadListFromRemote {
            (requireActivity() as MainActivity).supportActionBar?.title = list?.name
            binding.listContainer.visibility = View.VISIBLE
            binding.listLoader.visibility = View.GONE
        }

        val adapter = ListDetailsAdapter(
            mutableListOf(
                ListItemDetails(
                    ListDetailsRecyclerEntry(true, "1", "id")
                ) { a, id ->
                    Log.d("DetailsListView", "onCreateView: Clicked $id")
                },
                ListItemDetails(
                    ListDetailsRecyclerEntry(true, "2", "id1")
                ) { a, id ->
                    Log.d("DetailsListView", "onCreateView: Clicked $id")
                },
                ListItemDetails(
                    ListDetailsRecyclerEntry(true, "3", "id2")
                ) { a, id ->
                    Log.d("DetailsListView", "onCreateView: Clicked $id")
                }
            )
        )

        ItemTouchHelper(ItemMoveCallback(adapter)).attachToRecyclerView(binding.todoList)
        binding.todoList.adapter = adapter

        return binding.root;
    }


    private fun setupMenu() {
        (requireActivity() as MenuHost).addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.detailed_list_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                if (menuItem.itemId == R.id.action_manage_whitelisted) {
                    //FIXME: Open Dialog for managing whitelisted users
                }
                return false
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param listId The id of the list to load.
         * @return A new instance of fragment DetailedListView.
         */
        @JvmStatic
        fun newInstance(listId: String) =
            DetailedListView().apply {
                arguments = Bundle().apply {
                    putString(ARG_LIST_ID, listId)
                }
            }
    }
}