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
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import androidx.core.os.bundleOf
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_SHORT
import com.google.android.material.snackbar.Snackbar
import dev.justme.busket.MainActivity
import dev.justme.busket.R
import dev.justme.busket.databinding.DialogWithTextfieldBinding
import dev.justme.busket.databinding.FragmentDetailedListViewBinding
import dev.justme.busket.feathers.FeathersService
import dev.justme.busket.feathers.FeathersService.Companion.ARRAY_DATA_KEY
import dev.justme.busket.feathers.FeathersSocket
import dev.justme.busket.feathers.responses.ShoppingList
import dev.justme.busket.me.list.details.whitelisted.WhitelistedUser
import dev.justme.busket.me.list.details.whitelisted.WhitelistedUserPermissions
import org.json.JSONObject
import java.util.UUID


private const val ARG_LIST_ID = "listId"

abstract class ListDetailsMenuProvider : MenuProvider {
    open lateinit var menu: Menu
}

class ShoppingListAdapter {
    lateinit var todo: ListDetailsAdapter
    lateinit var done: ListDetailsAdapter
}

class DetailedListView : Fragment() {
    private var _binding: FragmentDetailedListViewBinding? = null
    private val binding get() = _binding!!

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var menuProvider: ListDetailsMenuProvider

    private var listId: String? = null
    private var list: ShoppingList? = null
    private val shoppingListAdapter = ShoppingListAdapter()

    private lateinit var feathers: FeathersSocket
    private lateinit var syncListDetailsManager: SyncListDetailsManager
    private lateinit var itemTouchHelper: ItemTouchHelper

    private var whitelistedUser: WhitelistedUser? = null
    private val permissions: WhitelistedUserPermissions
        get() = WhitelistedUserPermissions(whitelistedUser?.canEditEntries ?: true, whitelistedUser?.canDeleteEntries ?: true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            listId = it.getString(ARG_LIST_ID)
        }
    }

    private fun loadListFromRemote(cb: () -> Unit) {
        val query = JSONObject()
        query.put("listid", listId)
        feathers.service(FeathersService.Service.LIST).find(query) { data, err ->
            if (data == null || err != null) return@find

            val arr = data.getJSONArray(FeathersService.ARRAY_DATA_KEY)
            if (arr.length() <= 0) return@find // TODO TRIGGER NOT FOUND

            list = ShoppingList.fromJSONObject(arr.getJSONObject(0))
            handler.post(cb)
        }
    }

    private fun loadWhitelistedUserFromRemote(cb: () -> Unit) {
        val query = JSONObject()
        query.put("listId", listId)

        feathers.service(FeathersService.Service.WHITELISTED_USERS).find(query) { data, err ->
            if (data == null || err != null) return@find

            val whitelistedUsers = WhitelistedUser.fromJSONArray(data.getJSONArray(ARRAY_DATA_KEY))
            if (whitelistedUsers.isEmpty()) {
                handler.post {
                    setLoadingState(false)
                    cb.invoke()
                }
                return@find
            }

            whitelistedUser = whitelistedUsers.find { it.user == feathers.user?.uuid }
            handler.post {
                setLoadingState(false)
                cb.invoke()
            }
        }
    }

    private fun setLoadingState(loading: Boolean) {
        binding.listContainer.visibility = if (loading) View.GONE else View.VISIBLE
        binding.listLoader.visibility = if (loading) View.VISIBLE else View.GONE
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailedListViewBinding.inflate(inflater)
        setupMenu()

        feathers = FeathersSocket.getInstance(requireContext())

        setLoadingState(true)

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

        shoppingListAdapter.todo = ListDetailsAdapter(mutableListOf(), ::onItemMoved, ::onItemCheckStateChange, ::onItemLongPress, true, permissions, object : StartDragListener {
            override fun requestDrag(viewHolder: RecyclerView.ViewHolder) {
                itemTouchHelper.startDrag(viewHolder)
            }
        })
        itemTouchHelper = ItemTouchHelper(ItemMoveCallback(shoppingListAdapter.todo))
        itemTouchHelper.attachToRecyclerView(binding.todoList)
        binding.todoList.adapter = shoppingListAdapter.todo

        shoppingListAdapter.done = ListDetailsAdapter(mutableListOf(), ::onItemMoved, ::onItemCheckStateChange, ::onItemLongPress, false, permissions)
        binding.doneList.adapter = shoppingListAdapter.done

        loadListFromRemote {
            if (list == null) throw Exception("list should not be able to be null here!")
            loadWhitelistedUserFromRemote(::updatePermissions)

            syncListDetailsManager = SyncListDetailsManager(requireContext(), list!!)
            syncListDetailsManager.registerEventListener(ShoppingListEventListeners(
                {
                    createEntry(it.eventData.state.name, it.eventData.entryId, false)
                },
                {
                    val entry = findEntryGlobalById(it.eventData.entryId)
                    if (entry.list == ListType.TODO) {
                        shoppingListAdapter.todo.onRowMoved(it.eventData.state.oldIndex ?: -1, it.eventData.state.newIndex ?: -1, false)
                    } else {
                        shoppingListAdapter.done.onRowMoved(it.eventData.state.oldIndex ?: -1, it.eventData.state.newIndex ?: -1, false)
                    }
                },
                {
                    deleteEntry(it.eventData.entryId)
                },
                {
                    renameEntry(it.eventData.entryId, it.eventData.state.name, false)
                },
                {
                    onItemCheckStateChange(ListDetailsRecyclerEntry(false, it.eventData.state.name, it.eventData.entryId), false)
                },
                {
                    onItemCheckStateChange(ListDetailsRecyclerEntry(true, it.eventData.state.name, it.eventData.entryId), false)
                },
            ))


            val entries = list!!.entries.toMutableList()
            for (entry in entries) {
                shoppingListAdapter.todo.entries.add(ListDetailsRecyclerEntry(false, entry.name, entry.id))
            }

            val checkedEntries = list!!.checkedEntries.toMutableList()
            for (entry in checkedEntries) {
                shoppingListAdapter.done.entries.add(ListDetailsRecyclerEntry(true, entry.name, entry.id))
            }

            (requireActivity() as MainActivity).supportActionBar?.title = list?.name
            menuProvider.menu.findItem(R.id.action_manage_whitelisted).isVisible = list?.owner == feathers.user?.uuid
        }

        feathers.service(FeathersService.Service.WHITELISTED_USERS).on(FeathersService.SocketEventListener.PATCHED) { data, err ->
            if (err != null || data == null) return@on

            whitelistedUser = WhitelistedUser.fromJSON(data)
            handler.post(::updatePermissions)
        }

        feathers.service(FeathersService.Service.WHITELISTED_USERS).on(FeathersService.SocketEventListener.REMOVED) { data, err ->
            if (err != null || data == null) return@on

            list = null
            findNavController().popBackStack(R.id.HomeFragment, false)
        }

        return binding.root;
    }

    override fun onPause() {
        super.onPause()
        syncListDetailsManager.destroy()
    }

    private fun updatePermissions() {
        Log.d(javaClass.simpleName, "updatePermissions: canEditEntries ${permissions.canEditEntries}; canDeleteEntries ${permissions.canDeleteEntries}")
        shoppingListAdapter.todo.permissions = permissions
        shoppingListAdapter.done.permissions = permissions
        shoppingListAdapter.todo.notifyItemRangeChanged(0, shoppingListAdapter.todo.entries.size)
        shoppingListAdapter.done.notifyItemRangeChanged(0, shoppingListAdapter.done.entries.size)

        val editVisibility = if (permissions.canEditEntries) View.VISIBLE else View.GONE

        binding.clearBtn.visibility = if (permissions.canDeleteEntries) View.VISIBLE else View.GONE
        binding.addItemBtn.visibility = editVisibility
        binding.detailedListTextInputLayout.visibility = editVisibility
    }

    // region handle list events
    private fun findEntryGlobalById(entryId: String): FoundEntry {
        val todoListIndex = shoppingListAdapter.todo.entries.indexOfFirst { it.id == entryId }
        if (todoListIndex != -1) return FoundEntry(todoListIndex, ListType.TODO)


        val doneListIndex = shoppingListAdapter.done.entries.indexOfFirst { it.id == entryId }
        if (doneListIndex != -1) return FoundEntry(doneListIndex, ListType.DONE)

        throw EntryNotFound(entryId)
    }

    private fun onItemLongPress(entry: ListDetailsRecyclerEntry) {
        val inflater = requireActivity().layoutInflater
        val dialogView = DialogWithTextfieldBinding.inflate(inflater)

        val window = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.rename_entry)
            .setView(dialogView.root)
            .setPositiveButton(R.string.ok) { d, _ ->
                val newName = dialogView.textInput.editText?.text.toString()
                if (newName.isEmpty()) {
                    Snackbar.make(binding.root, R.string.name_too_short, LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                renameEntry(entry.id, newName, true)
                d.dismiss()
            }
            .setNegativeButton(R.string.cancel) { d, _ ->
                d.dismiss()
            }.show().window

        window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
        window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
    }

    private fun renameEntry(entryId: String, newName: String, recordEvent: Boolean) {
        val found = findEntryGlobalById(entryId)

        if (found.list == ListType.TODO) {
            shoppingListAdapter.todo.entries[found.index].name = newName
            shoppingListAdapter.todo.notifyItemChanged(found.index)
        } else {
            shoppingListAdapter.done.entries[found.index].name = newName
            shoppingListAdapter.done.notifyItemChanged(found.index)
        }

        if (recordEvent) {
            val state = ShoppingListEventState(newName, null, null)
            syncListDetailsManager.recordEvent(ShoppingListEventType.CHANGED_ENTRY_NAME, entryId, state)
        }
    }

    private fun deleteEntry(entryId: String) {
        val found = findEntryGlobalById(entryId)

        if (found.list == ListType.TODO) {
            shoppingListAdapter.todo.entries.removeAt(found.index)
            shoppingListAdapter.todo.notifyItemRemoved(found.index)

            return
        }

        shoppingListAdapter.done.entries.removeAt(found.index)
        shoppingListAdapter.done.notifyItemRemoved(found.index)
    }

    private fun clearDone() {
        clearDone(true)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun clearDone(recordEvent: Boolean) {
        val clearedEntries = shoppingListAdapter.done.entries.toMutableList()
        shoppingListAdapter.done.entries.clear()
        shoppingListAdapter.done.notifyDataSetChanged()

        if (!recordEvent) return
        for (entry in clearedEntries) {
            val state = ShoppingListEventState(entry.name, null, null)
            syncListDetailsManager.recordEvent(ShoppingListEventType.DELETE_ENTRY, entry.id, state, false)
        }

        syncListDetailsManager.sendEventsToServer()
    }

    private fun createEntry() {
        val name = binding.detailedListTextInputLayout.editText!!.text!!.trim().toString()
        createEntry(name, UUID.randomUUID().toString(), true)
        binding.detailedListTextInputLayout.editText!!.text = null
    }

    private fun createEntry(name: String, id: String, recordEvent: Boolean) {
        val entry = ListDetailsRecyclerEntry(false, name, id)
        shoppingListAdapter.todo.entries.add(0, entry)
        shoppingListAdapter.todo.notifyItemInserted(0)

        if (recordEvent) {
            val state = ShoppingListEventState(entry.name, null, null)
            syncListDetailsManager.recordEvent(ShoppingListEventType.CREATE_ENTRY, entry.id, state)
        }
    }

    private fun onItemMoved(entry: ListDetailsRecyclerEntry, fromPosition: Int, toPosition: Int) {
        val state = ShoppingListEventState(entry.name, fromPosition, toPosition)
        syncListDetailsManager.recordEvent(ShoppingListEventType.MOVE_ENTRY, entry.id, state)
    }

    private fun onItemCheckStateChange(entry: ListDetailsRecyclerEntry) {
        if (!permissions.canEditEntries) return
        onItemCheckStateChange(entry, true)
    }

    private fun onItemCheckStateChange(entry: ListDetailsRecyclerEntry, recordEvent: Boolean) {
        if (recordEvent) {
            val eventType = if (entry.checked) ShoppingListEventType.MARK_ENTRY_DONE else ShoppingListEventType.MARK_ENTRY_TODO
            val state = ShoppingListEventState(entry.name, null, null)
            syncListDetailsManager.recordEvent(eventType, entry.id, state)
        }

        if (entry.checked) {
            val index = shoppingListAdapter.todo.entries.indexOfFirst { it.id == entry.id }
            shoppingListAdapter.todo.entries.removeAt(index)
            shoppingListAdapter.todo.notifyItemRemoved(index)

            shoppingListAdapter.done.entries.add(0, entry)
            shoppingListAdapter.done.notifyItemInserted(0)
        } else {
            val index = shoppingListAdapter.done.entries.indexOfFirst { it.id == entry.id }
            shoppingListAdapter.done.entries.removeAt(index)
            shoppingListAdapter.done.notifyItemRemoved(index)

            shoppingListAdapter.todo.entries.add(0, entry)
            shoppingListAdapter.todo.notifyItemInserted(0)
        }
    }
    // endregion

    private fun setupMenu() {
        menuProvider = object : ListDetailsMenuProvider() {
            override lateinit var menu: Menu

            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.detailed_list_menu, menu)
                this.menu = menu
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                if (menuItem.itemId == R.id.action_manage_whitelisted) {
                    if (list == null) return false
                    findNavController().navigate(
                        R.id.action_DetailedListView_to_WhitelistedUsersFragment,
                        bundleOf("listId" to list?.listId, "listName" to list?.name)
                    )
                    return true
                }
                return false
            }
        }

        (requireActivity() as MenuHost).addMenuProvider(menuProvider, viewLifecycleOwner, Lifecycle.State.RESUMED)
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