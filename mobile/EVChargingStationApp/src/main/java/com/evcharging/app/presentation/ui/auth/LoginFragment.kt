package com.evcharging.app.presentation.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.evcharging.app.R
import com.evcharging.app.databinding.FragmentLoginBinding
import com.evcharging.app.domain.model.LoginRequest
import dagger.hilt.android.AndroidEntryPoint

/**
 * Login fragment for user authentication
 */
@AndroidEntryPoint
class LoginFragment : Fragment() {
    
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: AuthViewModel by activityViewModels()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        setupObservers()
    }
    
    private fun setupUI() {
        binding.apply {
            // Enable login button only when both fields are filled
            etEmail.addTextChangedListener { validateInputs() }
            etPassword.addTextChangedListener { validateInputs() }
            
            btnLogin.setOnClickListener {
                performLogin()
            }
            
            tvRegisterLink.setOnClickListener {
                (requireActivity() as AuthActivity).switchToRegister()
            }
            
            // Setup role selection
            setupRoleSelection()
        }
    }
    
    private fun setupRoleSelection() {
        binding.apply {
            radioGroupRole.setOnCheckedChangeListener { _, checkedId ->
                when (checkedId) {
                    R.id.rb_ev_owner -> {
                        // EV Owner selected - show standard login
                        tvRoleDescription.text = "Login as an EV Owner to book charging stations"
                    }
                    R.id.rb_operator -> {
                        // Operator selected - show operator login
                        tvRoleDescription.text = "Login as a Station Operator to manage bookings"
                    }
                }
            }
            
            // Default to EV Owner
            rbEvOwner.isChecked = true
        }
    }
    
    private fun setupObservers() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.apply {
                btnLogin.isEnabled = !isLoading
                progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                
                if (isLoading) {
                    btnLogin.text = "Logging in..."
                } else {
                    btnLogin.text = "Login"
                }
            }
        }
        
        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }
    
    private fun validateInputs() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        
        binding.btnLogin.isEnabled = email.isNotEmpty() && password.isNotEmpty()
        
        // Email validation
        if (email.isNotEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Please enter a valid email address"
        } else {
            binding.tilEmail.error = null
        }
        
        // Password validation
        if (password.isNotEmpty() && password.length < 6) {
            binding.tilPassword.error = "Password must be at least 6 characters"
        } else {
            binding.tilPassword.error = null
        }
    }
    
    private fun performLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        
        // Validate inputs
        if (email.isEmpty()) {
            binding.tilEmail.error = "Email is required"
            return
        }
        
        if (password.isEmpty()) {
            binding.tilPassword.error = "Password is required"
            return
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Please enter a valid email address"
            return
        }
        
        // Clear errors
        binding.tilEmail.error = null
        binding.tilPassword.error = null
        
        // Create login request
        val loginRequest = LoginRequest(email, password)
        
        // Determine user role based on selection
        val isOperator = binding.rbOperator.isChecked
        
        // Perform login
        viewModel.login(loginRequest, isOperator)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}