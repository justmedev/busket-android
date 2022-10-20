package dev.justme.busket

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import dev.justme.busket.databinding.FragmentHomeBinding
import dev.justme.busket.feathers.Feathers

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private var feathers: Feathers? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        feathers = Feathers.getInstance(context as Context)

        if (feathers?.user == null) {
            feathers?.tryAuthenticateWithAccessToken({
                binding.homeProgressbar.visibility = View.GONE
                binding.homeProgressText.visibility = View.GONE
            }, {
                Log.d("Busket", it.toString())
                findNavController().navigate(R.id.action_HomeFragment_to_LoginFragment)
            })

            return
        }

        binding.homeProgressbar.visibility = View.GONE
        binding.homeProgressText.visibility = View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}