package com.siele.firebaseapp.ui

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.siele.firebaseapp.R
import com.siele.firebaseapp.databinding.FragmentRegisterBinding

class Register : Fragment() {
    private lateinit var binding: FragmentRegisterBinding
    private lateinit var auth: FirebaseAuth
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentRegisterBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()

        binding.apply {
            tvLogin.setOnClickListener {
                findNavController().navigate(R.id.action_register_to_login)
            }
            btnSignup.setOnClickListener {
                signUpUserWithEmail()
            }
            clRegister.setOnFocusChangeListener { v, hasFocus ->
                if (hasFocus) {
                    hideKeyboard(v)
                }
            }
        }
        return binding.root
    }

    private fun signUpUserWithEmail() {
        binding.apply {
            if (!Patterns.EMAIL_ADDRESS.matcher(tiRegisterUsername.text.toString()).matches()) {
                if (tiConfirmPassword.text!!.isEmpty()) {
                    tiConfirmPassword.apply {
                        error = "Email address cannot be empty"
                        requestFocus()
                    }
                } else {
                    tiRegisterUsername.apply {
                        error = "Enter valid Email"
                        requestFocus()
                    }
                }
                return
            }
            if (tiRegisterPassword.text!!.isEmpty()) {
                tiRegisterPassword.apply {
                    error = "Password cannot be empty"
                    requestFocus()
                }
                return
            }
            if (tiConfirmPassword.text!!.isEmpty()) {
                tiConfirmPassword.apply {
                    error = "Password cannot be empty"
                    requestFocus()
                }
                return
            }
            if (tiRegisterPassword.text.toString() != tiConfirmPassword.text.toString()) {
                tiConfirmPassword.apply {
                    error = "Password do not match"
                    requestFocus()
                }
                return
            }
            pbLoading.visibility = View.VISIBLE
            auth.createUserWithEmailAndPassword(
                tiRegisterUsername.text.toString(),
                tiRegisterPassword.text.toString()
            ).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser

                    user?.sendEmailVerification()?.addOnCompleteListener { verificationTask ->
                        if (verificationTask.isSuccessful) {
                            Toast.makeText(
                                context,
                                "Verification email sent to ${user.email} ",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    updateUI(user)
                } else {
                    Log.d("Register", "createUserWithEmail:failure", task.exception)
                    Toast.makeText(
                        context!!, "Authentication failed.",
                        Toast.LENGTH_SHORT
                    ).show()
                    updateUI(null)
                }

            }
            pbLoading.visibility = View.GONE
        }
    }

    private fun updateUI(user: FirebaseUser?) {
        Toast.makeText(
            context!!, "${user.toString()} TODO(\"Not yet implemented\")",
            Toast.LENGTH_SHORT
        ).show()
        findNavController().navigate(R.id.action_register_to_login)
    }

    private fun hideKeyboard(view: View?) {
        if (view !is TextInputEditText) {
            val inputMethodManager: InputMethodManager? =
                activity!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
            inputMethodManager?.hideSoftInputFromWindow(activity!!.currentFocus?.windowToken, 0)
        }
    }

}