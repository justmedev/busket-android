package dev.justme.busket.me.list.details

import android.annotation.SuppressLint
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
import android.view.inputmethod.EditorInfo
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
import java.util.UUID


private const val ARG_LIST_ID = "listId"

class DetailedListView : Fragment() {
    private var _binding: FragmentDetailedListViewBinding? = null
    private val binding get() = _binding!!

    private val handler = Handler(Looper.getMainLooper())

    private var listId: String? = null
    private var list: ShoppingList? = null
    private lateinit var feathers: FeathersSocket
    private lateinit var syncListDetailsManager: SyncListDetailsManager

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

        binding.listContainer.visibility = View.GONE
        binding.listLoader.visibility = View.VISIBLE

        binding.addItemBtn.setOnClickListener { createEntry() }
        binding.detailedListTextInputLayout.editText!!.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                createEntry()
                true
            } else false
        }

        binding.clearBtn.setOnClickListener {
            clearDone()
        }

        val adapter = ListDetailsAdapter(mutableListOf(), ::onItemMoved, ::onItemCheckStateChange, true)
        ItemTouchHelper(ItemMoveCallback(adapter)).attachToRecyclerView(binding.todoList)
        binding.todoList.adapter = adapter

        binding.doneList.adapter = ListDetailsAdapter(mutableListOf(), ::onItemMoved, ::onItemCheckStateChange, false)

        loadListFromRemote {
            if (list == null) throw Exception("list should not be able to be null here!")
            syncListDetailsManager = SyncListDetailsManager(requireContext(), list!!)
            val entries = list!!.entries.toMutableList()
            for (entry in entries) {
                (binding.todoList.adapter as ListDetailsAdapter).entries.add(ListDetailsRecyclerEntry(false, entry.name, entry.id))
            }

            val checkedEntries = list!!.checkedEntries.toMutableList()
            for (entry in checkedEntries) {
                (binding.doneList.adapter as ListDetailsAdapter).entries.add(ListDetailsRecyclerEntry(true, entry.name, entry.id))
            }

            (requireActivity() as MainActivity).supportActionBar?.title = list?.name
            binding.listContainer.visibility = View.VISIBLE
            binding.listLoader.visibility = View.GONE
        }

        return binding.root;
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun clearDone() {
        val clearedEntries = (binding.doneList.adapter as ListDetailsAdapter).entries.toMutableList()
        (binding.doneList.adapter as ListDetailsAdapter).entries.clear()
        (binding.doneList.adapter as ListDetailsAdapter).notifyDataSetChanged()

        for (entry in clearedEntries) {
            val state = ShoppingListEventState(entry.name, null, null)
            syncListDetailsManager.recordEvent(ShoppingListEventType.DELETE_ENTRY, entry.id, state, false)
        }

        syncListDetailsManager.sendEventsToServer()
    }

    private fun createEntry() {
        val name = binding.detailedListTextInputLayout.editText!!.text!!.trim().toString()
        val entry = ListDetailsRecyclerEntry(false, name, UUID.randomUUID().toString())
        (binding.todoList.adapter as ListDetailsAdapter).entries.add(0, entry)
        (binding.todoList.adapter as ListDetailsAdapter).notifyItemInserted(0)

        val state = ShoppingListEventState(entry.name, null, null)
        syncListDetailsManager.recordEvent(ShoppingListEventType.CREATE_ENTRY, entry.id, state)
        binding.detailedListTextInputLayout.editText!!.text = null
    }

    private fun onItemMoved(entry: ListDetailsRecyclerEntry, fromPosition: Int, toPosition: Int) {
        val state = ShoppingListEventState(entry.name, fromPosition, toPosition)
        syncListDetailsManager.recordEvent(ShoppingListEventType.MOVE_ENTRY, entry.id, state)
    }

    private fun onItemCheckStateChange(entry: ListDetailsRecyclerEntry) {
        val eventType = if (entry.checked) ShoppingListEventType.MARK_ENTRY_DONE else ShoppingListEventType.MARK_ENTRY_TODO
        val state = ShoppingListEventState(entry.name, null, null)
        syncListDetailsManager.recordEvent(eventType, entry.id, state)

        if (entry.checked) {
            val index = (binding.todoList.adapter as ListDetailsAdapter).entries.indexOfFirst { it.id == entry.id }
            (binding.todoList.adapter as ListDetailsAdapter).entries.removeAt(index)
            (binding.todoList.adapter as ListDetailsAdapter).notifyItemRemoved(index)

            (binding.doneList.adapter as ListDetailsAdapter).entries.add(0, entry)
            (binding.doneList.adapter as ListDetailsAdapter).notifyItemInserted(0)
        } else {
            val index = (binding.doneList.adapter as ListDetailsAdapter).entries.indexOfFirst { it.id == entry.id }
            (binding.doneList.adapter as ListDetailsAdapter).entries.removeAt(index)
            (binding.doneList.adapter as ListDetailsAdapter).notifyItemRemoved(index)

            (binding.todoList.adapter as ListDetailsAdapter).entries.add(0, entry)
            (binding.todoList.adapter as ListDetailsAdapter).notifyItemInserted(0)
        }
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