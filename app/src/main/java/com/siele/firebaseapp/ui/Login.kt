package com.siele.firebaseapp.ui

import android.content.Context.INPUT_METHOD_SERVICE
import android.content.Intent
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
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.*
import com.siele.firebaseapp.BuildConfig
import com.siele.firebaseapp.R
import com.siele.firebaseapp.databinding.FragmentLoginBinding
import com.siele.firebaseapp.utils.Constants.GOOGLE_SIGN_IN_CODE
import com.siele.firebaseapp.viewmodels.SharedViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class Login : Fragment() {

    private lateinit var binding: FragmentLoginBinding
    @Inject
    lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var sharedViewModel: SharedViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentLoginBinding.inflate(inflater, container, false)

        setGoogleSignInClient()
        binding.apply {

            tvCreateAccount.setOnClickListener {
                findNavController().navigate(R.id.action_login_to_register)
            }

            btnLogin.setOnClickListener {
                logIn()
            }

            clLogin.setOnFocusChangeListener { v, hasFocus ->
                if (hasFocus) {
                    hideKeyboard(v)
                }
            }

            btnGoogleLogin.setOnClickListener {
                googleSignIn()
            }

            btnTwitterLogin.setOnClickListener {
                twitterLogin()
            }
        }

        return binding.root
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            GOOGLE_SIGN_IN_CODE -> {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                try {
                    val account = task.getResult(ApiException::class.java)
                    firebaseAuthWithGoogle(account.idToken!!)
                } catch (exception: ApiException) {
                    exception.printStackTrace()
                    updateUI(null)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            reload()
        }
    }

    private fun twitterLogin() {
        sharedViewModel.twitterLoginTask(requireActivity())
        sharedViewModel.firebaseUser.observe(viewLifecycleOwner,{  firebaseUser ->
            updateUI(firebaseUser)
        })
    }

    @Suppress("DEPRECATION")
    private fun googleSignIn() {
        val googleSignInIntent = googleSignInClient.signInIntent
        startActivityForResult(googleSignInIntent, GOOGLE_SIGN_IN_CODE)
    }

    private fun setGoogleSignInClient() {
        val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(BuildConfig.OAUTH_CLIENT_ID)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(requireContext(), googleSignInOptions)
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        showProgressbar()
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener { sigInTask ->
            if (sigInTask.isSuccessful) {
                val user = auth.currentUser
                updateUI(user)
            } else {
                Toast.makeText(context, "Authentication failed!!", Toast.LENGTH_SHORT).show()
                updateUI(null)
            }
            hideProgressbar()
        }
    }

    private fun hideProgressbar() {
        binding.pbLoginLoading.visibility = View.GONE
    }

    private fun showProgressbar() {
        binding.pbLoginLoading.visibility = View.VISIBLE
    }

    private fun logIn() {
        binding.apply {
            if (tiLoginUsernameEmail.text.isNullOrEmpty()) {
                tiLoginUsernameEmail.apply {
                    error = "Email cannot be empty"
                    requestFocus()
                }
                return
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(tiLoginUsernameEmail.text.toString()).matches()) {
                tiLoginUsernameEmail.apply {
                    error = "Please enter a valid Email"
                    requestFocus()
                }
                return
            }
            if (tiLoginPassword.text.isNullOrEmpty()) {
                tiLoginPassword.apply {
                    error = "Password cannot be empty"
                    requestFocus()
                }
                return
            }

            showProgressbar()
            auth.signInWithEmailAndPassword(
                tiLoginUsernameEmail.text.toString(),
                tiLoginPassword.text.toString()
            ).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("Login", "signInWithEmail:success")
                    val user = auth.currentUser
                    updateUI(user)
                } else {
                    Log.w("Login", "signInWithEmail:failure", task.exception)
                    Toast.makeText(
                        context!!, "Authentication failed.",
                        Toast.LENGTH_SHORT
                    ).show()
                    updateUI(null)
                }
                hideProgressbar()

            }
        }
    }

    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            if (user.isEmailVerified) {
                findNavController().navigate(R.id.action_login_to_home2)
            } else {
                Toast.makeText(context, "Please verify your email address", Toast.LENGTH_SHORT)
                    .show()
            }
        } else {
            Toast.makeText(context, "Login Failed, please try again", Toast.LENGTH_SHORT).show()
        }
    }

    private fun reload() {
        auth.currentUser!!.reload().addOnCompleteListener { reloadTask ->
            if (reloadTask.isSuccessful) {
                updateUI(auth.currentUser)
            } else {
                Toast.makeText(context, "Refresh Failed", Toast.LENGTH_SHORT).show()
            }

        }
    }

    private fun hideKeyboard(view: View?) {
        if (view !is TextInputEditText) {
            val inputMethodManager: InputMethodManager? =
                activity!!.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager?
            inputMethodManager?.hideSoftInputFromWindow(activity!!.currentFocus?.windowToken, 0)
        }
    }

}