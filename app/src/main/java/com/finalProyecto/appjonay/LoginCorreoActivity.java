package com.finalProyecto.appjonay;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginCorreoActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private EditText emailEditText, passwordEditText;
    private Button loginButton, resendVerificationButton;
    private Button btnVolver;
    private Button btnRecuperarPassword;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_correo); // Asegúrate que el layout se llama así

        mAuth = FirebaseAuth.getInstance();

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        resendVerificationButton = findViewById(R.id.resendVerificationButton);
        btnVolver = findViewById(R.id.btnVolver);
        btnRecuperarPassword = findViewById(R.id.btnRecuperarPassword);

        loginButton.setOnClickListener(v -> loginUser());
        resendVerificationButton.setOnClickListener(v -> resendVerificationEmail());
        btnVolver.setOnClickListener(v -> finish());
        btnRecuperarPassword.setOnClickListener(v -> recuperarPassword());

    }

    private void loginUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Introduce email y contraseña", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            if (user.isEmailVerified()) {
                                startActivity(new Intent(LoginCorreoActivity.this, MainActivity.class));
                                finish();
                            } else {
                                Toast.makeText(this, "Debes verificar tu email antes de entrar. Revisa tu bandeja de entrada.", Toast.LENGTH_LONG).show();
                                mAuth.signOut();
                            }
                        }
                    } else {
                        Toast.makeText(this, "Error al iniciar sesión: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void resendVerificationEmail() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Introduce email y contraseña", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        mAuth.getCurrentUser().sendEmailVerification()
                                .addOnCompleteListener(verificationTask -> {
                                    if (verificationTask.isSuccessful()) {
                                        Toast.makeText(this, "Correo de verificación enviado. Revisa tu bandeja.", Toast.LENGTH_LONG).show();
                                    } else {
                                        Toast.makeText(this, "Error al enviar el correo: " + verificationTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                    mAuth.signOut();
                                });
                    } else {
                        Toast.makeText(this, "Email o contraseña incorrectos.", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void recuperarPassword() {
        String email = emailEditText.getText().toString().trim();
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Introduce tu email para recuperar la contraseña.", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Correo de recuperación enviado. Revisa tu bandeja.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "No se pudo enviar el correo: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

}
