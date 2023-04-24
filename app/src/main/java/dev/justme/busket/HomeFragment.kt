package dev.justme.busket

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.navigation.fragment.findNavController
import dev.justme.busket.databinding.FragmentHomeBinding
import dev.justme.busket.feathers.FeathersSocket
import dev.justme.busket.feathers.responses.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.*


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

        binding.homeListOverviewRecyclerview.adapter = EmptyAdapter();

        feathers = FeathersSocket.getInstance(context as Context)
        if (feathers?.user == null) {
            binding.homeMainContentContainer.visibility = View.GONE
            binding.homeLoaderContainer.visibility = View.VISIBLE
            feathers?.tryAuthenticateWithAccessToken({ afterLoginSuccess(it) }, {
                Log.d("Busket", it.toString())
                findNavController().navigate(R.id.action_HomeFragment_to_LoginFragment)
            })
        }

        binding.createShoppingListFab.setOnClickListener(::createShoppingList)
    }

    private fun createShoppingList(view: View) {
        val alertDialog: AlertDialog? = activity?.let {
            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater;
            val dialogView = inflater.inflate(R.layout.dialog_create_list, null)

            builder.setView(dialogView)
                .setPositiveButton(R.string.create) { _, _ ->
                    val createListName = dialogView.findViewById(R.id.dialog_create_list_name) as EditText
                    val createListDescription = dialogView.findViewById(R.id.dialog_create_list_description) as EditText

                    if (createListName.text.trim().length < 3) {
                        createListName.error = "List Name has to be longer than 3 characters!"
                        return@setPositiveButton
                    }

                    val shoppingList = ShoppingList(
                        UUID.randomUUID().toString(),
                        createListName.text.trim().toString(),
                        createListDescription.text.trim().toString(),
                        feathers?.user?.uuid ?: "UNKNOWN",
                        emptyList(),
                        emptyList(),
                    )

                    val jData = JSONObject()
                    jData.put("listid", shoppingList.listId)
                    jData.put("name", shoppingList.name)
                    jData.put("description", shoppingList.description)
                    jData.put("owner", shoppingList.owner)
                    jData.put("entries", shoppingList.entries)
                    jData.put("checkedEntries", shoppingList.checkedEntries)

                    feathers?.service("list", FeathersSocket.Method.CREATE, jData) { data, error ->
                        if (error != null || data == null) return@service
                        shoppingLists.add(ListOverview(shoppingList) {
                            Log.d("Busket ShoppingList", "pressed")
                        })
                        handler.post {
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

    private fun afterLoginSuccess(auth: AuthenticationSuccessResponse) {
        handler.post {
            binding.homeLoaderContainer.visibility = View.GONE
            binding.homeMainContentContainer.visibility = View.VISIBLE
            binding.homeWelcome.text = getString(R.string.welcome, feathers?.user?.fullName)
        }

        feathers?.service(FeathersSocket.Service.LIBRARY, FeathersSocket.Method.FIND, null) { data, err ->
            if (err != null || data == null) {
                // Handle error
                return@service
            }

            val array = data.getJSONArray("arrayData")

            Log.d("Busket HomeFragment", data.toString())

            for (i in 0 until array.length()) {
                val libraryEntry = array.getJSONObject(i)
                val list = libraryEntry.getJSONObject("list")

                val res = feathers?.gson?.fromJson(libraryEntry.getJSONObject("list").toString(), ShoppingListResponse::class.java) ?: return@service
                val shoppingList = ShoppingList(libraryEntry.getString("listId"), res.name, res.description, res.owner, res.entries, res.checkedEntries)
                shoppingLists.add(ListOverview(shoppingList) { Log.d("Busket", "clicked sub") })

                handler.post {
                    binding.homeListOverviewRecyclerview.adapter = ListOverviewAdapter(shoppingLists.toTypedArray())
                    binding.homeListOverviewRecyclerview.adapter?.notifyItemInserted(i)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}