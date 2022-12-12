package dev.justme.busket

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import dev.justme.busket.databinding.FragmentHomeBinding
import dev.justme.busket.feathers.FeathersSocket
import dev.justme.busket.feathers.responses.AuthenticationSuccessResponse


class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private var feathers: FeathersSocket? = null
    private val handler = Handler(Looper.getMainLooper())


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        feathers = FeathersSocket.getInstance(context as Context)
        if (feathers?.user == null) {
            binding.homeMainContentContainer.visibility = View.GONE
            binding.homeLoaderContainer.visibility = View.VISIBLE
            feathers?.tryAuthenticateWithAccessToken({ afterLoginSuccess(it) }, {
                Log.d("Busket", it.toString())
                findNavController().navigate(R.id.action_HomeFragment_to_LoginFragment)
            })
        }
    }

    private fun afterLoginSuccess(auth: AuthenticationSuccessResponse) {
        handler.post {
            binding.homeLoaderContainer.visibility = View.GONE
            binding.homeMainContentContainer.visibility = View.VISIBLE
            binding.homeWelcome.text = getString(R.string.welcome, feathers?.user?.fullName)
        }

        val socket = FeathersSocket.getInstance(requireContext())
        // socket.req(requireContext())

        val list = arrayOf(
            ListOverview("Title", "Sub") { Log.d("Busket", "clicked sub") },
            ListOverview("t", "s") { Log.d("Busket", "clicked s") })

        handler.post {
            binding.homeListOverviewRecyclerview.adapter = ListOverviewAdapter(list)
        }

        // TODO: Get lists from backend and populate recyclerview with real data
        /*val shoppingList = ShoppingList(
            UUID.randomUUID().toString(),
            "Test-list-android",
            "android test shopping list",
            feathers?.user?.uuid ?: "UNKNOWN",
            listOf()
        )
        feathers?.service("list")?.create(
            shoppingList,
            ShoppingListResponse::class.java,
            {
                Log.d("Busket sl name", it.name)
            },
            {
                Log.d("Busket", it.networkResponse.data.toString()) })*/
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}