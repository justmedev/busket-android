package dev.justme.busket

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import dev.justme.busket.databinding.FragmentLoginBinding
import dev.justme.busket.feathers.FeathersSocket


/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()

        binding.loginContainer.visibility = View.VISIBLE
        binding.loginLoaderContainer.visibility = View.GONE

        (activity as AppCompatActivity?)!!.supportActionBar!!.setDisplayHomeAsUpEnabled(false)
    }

    override fun onStop() {
        super.onStop()
        (activity as AppCompatActivity?)!!.supportActionBar!!.setDisplayHomeAsUpEnabled(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val handler = Handler(Looper.getMainLooper())

        FeathersSocket.getInstance(requireContext()).tryAuthenticateWithAccessToken({
            handler.post {
                findNavController().navigate(R.id.action_LoginFragment_to_HomeFragment)
            }
        })

        binding.loginEmailInput.doAfterTextChanged {
            if (it != null) validateInput(it, null)
        }

        binding.loginRegisterButton.setOnClickListener {
            findNavController().navigate(R.id.action_LoginFragment_to_RegisterFragment)
        }

        binding.loginLoginButton.setOnClickListener {
            if (validateInput(binding.loginEmailInput.text, binding.loginPasswordInput.text)) {
                if (context == null) return@setOnClickListener

                binding.loginContainer.visibility = View.GONE
                binding.loginLoaderContainer.visibility = View.VISIBLE

                FeathersSocket.getInstance(requireContext()).authenticate(
                    binding.loginEmailInput.text.toString(),
                    binding.loginPasswordInput.text.toString(),
                    {
                        Log.d("LoginFragment", "Success [%d]: %s".format(it.user.id, it.user.fullName))
                        handler.post {
                            findNavController().navigate(R.id.action_LoginFragment_to_HomeFragment)
                        }
                    },
                    {
                        Log.d("LoginFragment", "Error: SocketError [%d]: %s".format(it.code, it.message))
                        handler.post {
                            binding.loginRegisterButton.isEnabled = true
                            binding.loginLoginButton.isEnabled = true
                        }

                        if (it.code == 401) {
                            Snackbar.make(view, "Wrong email or password!", Snackbar.LENGTH_LONG)
                                .show()
                            return@authenticate
                        }

                        Snackbar.make(view, "An error occurred!", Snackbar.LENGTH_LONG).show()
                        Log.e("Busket", it.toString())
                    }
                )
            }
        }
    }

    private fun validateInput(email: Editable?, password: Editable?): Boolean {
        var passed = true

        if (email != null && !email.contains("@")) {
            passed = false
            binding.loginEmailInput.error = "Input has to be an email!"
        } else binding.loginEmailInput.error = null

        if (password != null && password.length < 3) {
            passed = false
            binding.loginPasswordInput.error = "Password has to be at least 3 chars long!"
        }

        return passed
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}