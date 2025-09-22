package com.finalProyecto.appjonay;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

// 🔴 IMPORTANTE: añade estas imports para los errores específicos
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthException;

public class RegisterActivity extends AppCompatActivity {
    private EditText etNombre, etApellidos, etEmail, etTelefono, etPassword;
    private Button btnRegistrar;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private Button btnVolver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etNombre = findViewById(R.id.etNombre);
        etApellidos = findViewById(R.id.etApellidos);
        etEmail = findViewById(R.id.etEmail);
        etTelefono = findViewById(R.id.etTelefono);
        etPassword = findViewById(R.id.etPassword);
        btnRegistrar = findViewById(R.id.btnRegistrar);
        btnVolver = findViewById(R.id.btnVolver);

        btnRegistrar.setOnClickListener(v -> registrarUsuario());
        btnVolver.setOnClickListener(v -> finish());

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    private void registrarUsuario() {
        String nombre = etNombre.getText().toString().trim();
        String apellidos = etApellidos.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String telefono = etTelefono.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String estado = "pendiente"; // Por defecto

        if (nombre.isEmpty() || apellidos.isEmpty() || email.isEmpty() || telefono.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            user.sendEmailVerification()
                                    .addOnCompleteListener(verifyTask -> {
                                        if (verifyTask.isSuccessful()) {
                                            String uid = user.getUid();
                                            Map<String, Object> usuario = new HashMap<>();
                                            usuario.put("nombre", nombre);
                                            usuario.put("apellidos", apellidos);
                                            usuario.put("email", email);
                                            usuario.put("telefono", telefono);
                                            usuario.put("estado", estado);

                                            db.collection("usuarios").document(uid)
                                                    .set(usuario)
                                                    .addOnSuccessListener(aVoid -> {
                                                        Toast.makeText(this, "Registro correcto. Confirma tu email antes de iniciar sesión.", Toast.LENGTH_LONG).show();
                                                        startActivity(new Intent(this, LoginActivity.class));
                                                        finish();
                                                    })
                                                    .addOnFailureListener(e ->
                                                            Toast.makeText(this, "Error al guardar usuario: " + e.getMessage(), Toast.LENGTH_LONG).show()
                                                    );
                                        } else {
                                            Toast.makeText(this, "Error al enviar email de verificación.", Toast.LENGTH_LONG).show();
                                        }
                                    });
                        }
                    } else {
                        // 🔴 Aquí mejoramos la gestión de errores
                        Exception ex = task.getException();
                        String msg = "Error al registrar.";

                        if (ex instanceof FirebaseAuthWeakPasswordException) {
                            msg = "La contraseña es demasiado débil. Usa al menos 6-8 caracteres con números y letras.";
                        } else if (ex instanceof FirebaseAuthInvalidCredentialsException) {
                            msg = "El email no es válido.";
                        } else if (ex instanceof FirebaseAuthUserCollisionException) {
                            msg = "Ese email ya está registrado.";
                        } else if (ex instanceof FirebaseAuthException) {
                            String code = ((FirebaseAuthException) ex).getErrorCode();
                            msg = "Error de autenticación: " + code;
                        } else if (ex != null) {
                            msg = "Error: " + ex.getMessage();
                        }

                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                        android.util.Log.w("Register", "register error", ex);
                    }
                });
    }
}
