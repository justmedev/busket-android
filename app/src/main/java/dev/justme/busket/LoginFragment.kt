package dev.justme.busket

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import dev.justme.busket.databinding.FragmentLoginBinding
import dev.justme.busket.feathers.Feathers

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
    ): View? {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.loginEmailInput.doAfterTextChanged {
            if (it != null) validateInput(it, null);
        }

        binding.loginRegisterButton.setOnClickListener {
            findNavController().navigate(R.id.action_LoginFragment_to_RegisterFragment)
        }

        binding.loginLoginButton.setOnClickListener {
            if (validateInput(binding.loginEmailInput.text, binding.loginPasswordInput.text)) {
                if (context == null) return@setOnClickListener

                binding.loginRegisterButton.isEnabled = false
                binding.loginLoginButton.isEnabled = false

                val feathers = Feathers(context as Context)
                feathers.authenticate(
                    binding.loginEmailInput.text.toString(),
                    binding.loginPasswordInput.text.toString(),
                    {
                        findNavController().navigate(R.id.action_LoginFragment_to_HomeFragment)
                    },
                    {
                        binding.loginRegisterButton.isEnabled = true
                        binding.loginLoginButton.isEnabled = true

                        if (it.networkResponse.statusCode == 401) {
                            Snackbar.make(view, "Wrong email or password!", Snackbar.LENGTH_LONG).show()
                            return@authenticate
                        }

                        Snackbar.make(view, "An error occurred!", Snackbar.LENGTH_LONG).show()
                        Log.e("Busket VolleyError", it.toString())
                    }
                )
            }
        }
    }

    private fun validateInput(email: Editable?, password: Editable?): Boolean {
        var passed = true;

        if (email != null && !email.contains("@")) {
            passed = false;
            binding.loginEmailInput.error = "Input has to be an email!";
        } else binding.loginEmailInput.error = null;

        if (password != null && password.length < 3) {
            passed = false;
            binding.loginPasswordInput.error = "Password has to be at least 3 chars long!";
        }

        return passed;
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}