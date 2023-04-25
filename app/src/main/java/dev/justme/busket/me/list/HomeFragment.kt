package dev.justme.busket.me.list

import android.content.Context
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
import android.widget.EditText
import androidx.core.os.bundleOf
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.justme.busket.MainActivity
import dev.justme.busket.R
import dev.justme.busket.databinding.FragmentHomeBinding
import dev.justme.busket.feathers.FeathersSocket
import dev.justme.busket.feathers.responses.ShoppingList


class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private var feathers: FeathersSocket? = null
    private val handler = Handler(Looper.getMainLooper())
    private val shoppingLists = emptyList<ListOverview>().toMutableList()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMenu()

        (requireActivity() as MainActivity).supportActionBar?.setDisplayHomeAsUpEnabled(false)

        feathers = FeathersSocket.getInstance(context as Context)
        if (feathers?.user == null) findNavController().navigate(R.id.action_HomeFragment_to_LoginFragment)
        (requireActivity() as MainActivity).supportActionBar?.title = getString(R.string.welcome, feathers?.user?.fullName)

        binding.homeListOverviewRecyclerview.adapter = EmptyAdapter()
        populateRecyclerView()

        binding.createShoppingListFab.setOnClickListener(::createShoppingList)
    }

    private fun setupMenu() {
        (requireActivity() as MenuHost).addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_open_settings, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                if (menuItem.itemId == R.id.action_settings) findNavController().navigate(R.id.action_HomeFragment_to_Settings)
                return true
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun createShoppingList(view: View) {
        val alertDialog: androidx.appcompat.app.AlertDialog? = activity?.let {
            val builder = MaterialAlertDialogBuilder(it)
            val inflater = requireActivity().layoutInflater
            val dialogView = inflater.inflate(R.layout.dialog_create_list, null)

            builder.setView(dialogView)
                .setPositiveButton(R.string.create) { _, _ ->
                    val createListName = dialogView.findViewById(R.id.dialog_create_list_name) as EditText
                    val createListDescription = dialogView.findViewById(R.id.dialog_create_list_description) as EditText

                    if (createListName.text.trim().length < 3) {
                        createListName.error = "List Name has to be longer than 3 characters!"
                        return@setPositiveButton
                    }

                    val listJSON = ShoppingList(
                        null,
                        createListName.text.trim().toString(),
                        createListDescription.text.trim().toString(),
                        null,
                        emptyList(),
                        emptyList(),
                    ).toJSONObject()

                    feathers?.service(FeathersSocket.Service.LIST, FeathersSocket.Method.CREATE, listJSON) { data, error ->
                        if (error != null || data == null) return@service
                        shoppingLists.add(ListOverview(ShoppingList.fromJSONObject(data), ::openList))
                        handler.post {
                            (binding.homeListOverviewRecyclerview.adapter as ListOverviewAdapter).lists = shoppingLists.toTypedArray()
                            binding.homeListOverviewRecyclerview.adapter?.notifyItemInserted(shoppingLists.lastIndex)
                        }
                    }
                }
                .setNegativeButton(R.string.cancel) { dialog, _ ->
                    dialog.cancel()
                }
            builder.create()
        }
        alertDialog?.show()
    }

    private fun openList(v: View?, list: ShoppingList) {
        findNavController().navigate(R.id.action_HomeFragment_to_DetailedListView, bundleOf("listId" to list.listId))
    }

    private fun populateRecyclerView() {
        binding.homeListOverviewRecyclerview.visibility = View.GONE
        binding.homeListOverviewLoader.visibility = View.VISIBLE
        shoppingLists.clear()

        feathers?.service(FeathersSocket.Service.LIBRARY, FeathersSocket.Method.FIND, null) { data, err ->
            if (err != null || data == null) {
                // Handle error
                return@service
            }

            val array = data.getJSONArray("arrayData")

            Log.d("Busket HomeFragment", data.toString())

            for (i in 0 until array.length()) {
                val libraryEntry = array.getJSONObject(i)

                val shoppingList = ShoppingList.fromJSONObject(libraryEntry.getJSONObject("list"))
                shoppingLists.add(ListOverview(shoppingList, ::openList))

                handler.post {
                    binding.homeListOverviewRecyclerview.adapter = ListOverviewAdapter(shoppingLists.toTypedArray())
                    binding.homeListOverviewRecyclerview.adapter?.notifyItemInserted(i)

                    binding.homeListOverviewRecyclerview.visibility = View.VISIBLE
                    binding.homeListOverviewLoader.visibility = View.GONE
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}