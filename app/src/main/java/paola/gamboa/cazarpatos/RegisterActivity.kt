package paola.gamboa.cazarpatos

import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class RegisterActivity : AppCompatActivity() {
    lateinit var editTextEmail: EditText
    lateinit var editTextPassword: EditText
    lateinit var editTextConfirmPassword: EditText
    lateinit var buttonSignUp: Button
    lateinit var buttonBackToLogin: Button
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        editTextEmail = findViewById(R.id.editTextEmail)
        editTextPassword = findViewById(R.id.editTextPassword)
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword)
        buttonSignUp = findViewById(R.id.buttonSignUp)
        buttonBackToLogin = findViewById(R.id.buttonBackToLogin)

        auth = Firebase.auth

        buttonSignUp.setOnClickListener {
            if (validateForm()) {
                registrarUsuario(
                    editTextEmail.text.toString(),
                    editTextPassword.text.toString()
                )
            }
        }

        buttonBackToLogin.setOnClickListener {
            finish()
        }
    }

    private fun validateForm(): Boolean {
        val email = editTextEmail.text.toString().trim()
        val password = editTextPassword.text.toString()
        val confirmPassword = editTextConfirmPassword.text.toString()

        if (email.isEmpty()) {
            editTextEmail.error = getString(R.string.error_email_required)
            editTextEmail.requestFocus()
            return false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editTextEmail.error = getString(R.string.error_email_invalid)
            editTextEmail.requestFocus()
            return false
        }

        if (password.isEmpty()) {
            editTextPassword.error = getString(R.string.error_password_required)
            editTextPassword.requestFocus()
            return false
        }

        if (password.length < 8) {
            editTextPassword.error = getString(R.string.error_password_length)
            editTextPassword.requestFocus()
            return false
        }

        if (confirmPassword.isEmpty()) {
            editTextConfirmPassword.error = getString(R.string.error_confirm_password_required)
            editTextConfirmPassword.requestFocus()
            return false
        }

        if (password != confirmPassword) {
            editTextConfirmPassword.error = getString(R.string.error_passwords_not_matching)
            editTextConfirmPassword.requestFocus()
            return false
        }

        return true
    }

    private fun registrarUsuario(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(
                        this,
                        getString(R.string.success_user_registered),
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                } else {
                    Toast.makeText(
                        this,
                        getString(R.string.error_register_prefix) + task.exception?.message,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }
}