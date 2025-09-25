package com.evcharging.app.presentation.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.evcharging.app.databinding.FragmentRegisterBinding
import com.evcharging.app.domain.model.RegisterRequest
import com.evcharging.app.domain.model.UserRole
import dagger.hilt.android.AndroidEntryPoint

/**
 * Registration fragment for new user accounts
 * Uses NIC as primary key as per requirement
 */
@AndroidEntryPoint
class RegisterFragment : Fragment() {
    
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: AuthViewModel by activityViewModels()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        setupObservers()
    }
    
    private fun setupUI() {
        binding.apply {
            // Add text change listeners for validation
            etNic.addTextChangedListener { validateInputs() }
            etFirstName.addTextChangedListener { validateInputs() }
            etLastName.addTextChangedListener { validateInputs() }
            etEmail.addTextChangedListener { validateInputs() }
            etPhone.addTextChangedListener { validateInputs() }
            etPassword.addTextChangedListener { validateInputs() }
            etConfirmPassword.addTextChangedListener { validateInputs() }
            
            btnRegister.setOnClickListener {
                performRegistration()
            }
            
            tvLoginLink.setOnClickListener {
                (requireActivity() as AuthActivity).switchToLogin()
            }
        }
    }
    
    private fun setupObservers() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.apply {
                btnRegister.isEnabled = !isLoading
                progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                
                if (isLoading) {
                    btnRegister.text = "Creating Account..."
                } else {
                    btnRegister.text = "Create Account"
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
        binding.apply {
            val nic = etNic.text.toString().trim()
            val firstName = etFirstName.text.toString().trim()
            val lastName = etLastName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val password = etPassword.text.toString()
            val confirmPassword = etConfirmPassword.text.toString()
            
            // NIC validation (Sri Lankan NIC format)
            if (nic.isNotEmpty() && !isValidNIC(nic)) {
                tilNic.error = "Please enter a valid NIC number"
            } else {
                tilNic.error = null
            }
            
            // Email validation
            if (email.isNotEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                tilEmail.error = "Please enter a valid email address"
            } else {
                tilEmail.error = null
            }
            
            // Phone validation
            if (phone.isNotEmpty() && !isValidPhoneNumber(phone)) {
                tilPhone.error = "Please enter a valid phone number"
            } else {
                tilPhone.error = null
            }
            
            // Password validation
            if (password.isNotEmpty() && password.length < 8) {
                tilPassword.error = "Password must be at least 8 characters"
            } else if (password.isNotEmpty() && !isValidPassword(password)) {
                tilPassword.error = "Password must contain letters, numbers and special characters"
            } else {
                tilPassword.error = null
            }
            
            // Confirm password validation
            if (confirmPassword.isNotEmpty() && password != confirmPassword) {
                tilConfirmPassword.error = "Passwords do not match"
            } else {
                tilConfirmPassword.error = null
            }
            
            // Enable register button if all fields are valid
            btnRegister.isEnabled = nic.isNotEmpty() && firstName.isNotEmpty() && 
                    lastName.isNotEmpty() && email.isNotEmpty() && phone.isNotEmpty() &&
                    password.isNotEmpty() && confirmPassword.isNotEmpty() &&
                    isValidNIC(nic) && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() &&
                    isValidPhoneNumber(phone) && isValidPassword(password) && 
                    password == confirmPassword
        }
    }
    
    private fun isValidNIC(nic: String): Boolean {
        // Sri Lankan NIC validation
        // Old format: 9 digits + V (e.g., 123456789V)
        // New format: 12 digits (e.g., 199812345678)
        val oldNicPattern = "^[0-9]{9}[vVxX]$".toRegex()
        val newNicPattern = "^[0-9]{12}$".toRegex()
        
        return oldNicPattern.matches(nic) || newNicPattern.matches(nic)
    }
    
    private fun isValidPhoneNumber(phone: String): Boolean {
        // Sri Lankan phone number validation
        // Mobile: +94 7X XXXX XXX or 07X XXXX XXX
        val phonePattern = "^(\\+94|0)?7[0-9]{8}$".toRegex()
        return phonePattern.matches(phone.replace(" ", "").replace("-", ""))
    }
    
    private fun isValidPassword(password: String): Boolean {
        // Password must contain at least one letter, one number, and one special character
        val hasLetter = password.any { it.isLetter() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecial = password.any { !it.isLetterOrDigit() }
        
        return hasLetter && hasDigit && hasSpecial
    }
    
    private fun performRegistration() {
        binding.apply {
            val nic = etNic.text.toString().trim()
            val firstName = etFirstName.text.toString().trim()
            val lastName = etLastName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val password = etPassword.text.toString()
            
            // Final validation
            if (!validateAllFields()) {
                return
            }
            
            // Create registration request
            val registerRequest = RegisterRequest(
                nic = nic,
                firstName = firstName,
                lastName = lastName,
                email = email,
                phoneNumber = phone,
                password = password,
                role = UserRole.EVOwner // Default role for mobile registration
            )
            
            // Perform registration
            viewModel.register(registerRequest)
        }
    }
    
    private fun validateAllFields(): Boolean {
        binding.apply {
            val nic = etNic.text.toString().trim()
            val firstName = etFirstName.text.toString().trim()
            val lastName = etLastName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val password = etPassword.text.toString()
            val confirmPassword = etConfirmPassword.text.toString()
            
            var isValid = true
            
            if (nic.isEmpty()) {
                tilNic.error = "NIC is required"
                isValid = false
            } else if (!isValidNIC(nic)) {
                tilNic.error = "Please enter a valid NIC number"
                isValid = false
            }
            
            if (firstName.isEmpty()) {
                tilFirstName.error = "First name is required"
                isValid = false
            }
            
            if (lastName.isEmpty()) {
                tilLastName.error = "Last name is required"
                isValid = false
            }
            
            if (email.isEmpty()) {
                tilEmail.error = "Email is required"
                isValid = false
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                tilEmail.error = "Please enter a valid email address"
                isValid = false
            }
            
            if (phone.isEmpty()) {
                tilPhone.error = "Phone number is required"
                isValid = false
            } else if (!isValidPhoneNumber(phone)) {
                tilPhone.error = "Please enter a valid phone number"
                isValid = false
            }
            
            if (password.isEmpty()) {
                tilPassword.error = "Password is required"
                isValid = false
            } else if (password.length < 8) {
                tilPassword.error = "Password must be at least 8 characters"
                isValid = false
            } else if (!isValidPassword(password)) {
                tilPassword.error = "Password must contain letters, numbers and special characters"
                isValid = false
            }
            
            if (confirmPassword.isEmpty()) {
                tilConfirmPassword.error = "Please confirm your password"
                isValid = false
            } else if (password != confirmPassword) {
                tilConfirmPassword.error = "Passwords do not match"
                isValid = false
            }
            
            return isValid
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}