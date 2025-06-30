package com.finalProyecto.appjonay;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            goToLogin();
        } else {
            currentUser.reload().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    if (mAuth.getCurrentUser() == null) {
                        goToLogin();
                    } else {
                        setContentView(R.layout.activity_main);

                        // Mostrar mensaje de bienvenida
                        mostrarBienvenida();

                        // Botón cerrar sesión
                        Button btnCerrarSesion = findViewById(R.id.btnCerrarSesion);
                        btnCerrarSesion.setOnClickListener(v -> {
                            mAuth.signOut();
                            goToLogin();
                        });
                    }
                } else {
                    Log.e("MainActivity", "Error al recargar usuario: ", task.getException());
                    mAuth.signOut();
                    goToLogin();
                }
            });
        }
    }

    private void mostrarBienvenida() {
        // 1. Intenta obtener usuario Google
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        TextView tvBienvenida = findViewById(R.id.tvBienvenida);

        if (account != null && account.getDisplayName() != null) {
            // Si inició con Google, mostramos su nombre directamente
            tvBienvenida.setText("¡Bienvenido, " + account.getDisplayName() + "!");
        } else {
            // Si no es Google, cargamos de Firestore y lo ponemos en mayúsculas
            mostrarNombreUsuarioDesdeFirestore(tvBienvenida);
        }
    }

    private void mostrarNombreUsuarioDesdeFirestore(TextView tvBienvenida) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            DocumentReference userRef = db.collection("usuarios").document(uid);

            userRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String nombre = documentSnapshot.getString("nombre");
                    String apellidos = documentSnapshot.getString("apellidos");
                    if (nombre != null && apellidos != null) {
                        tvBienvenida.setText("¡Bienvenido, " + nombre.toUpperCase() + " " + apellidos.toUpperCase() + "!");
                    } else if (nombre != null) {
                        tvBienvenida.setText("¡Bienvenido, " + nombre.toUpperCase() + "!");
                    } else {
                        tvBienvenida.setText("¡Bienvenido!");
                    }
                } else {
                    tvBienvenida.setText("¡Bienvenido!");
                }
            }).addOnFailureListener(e -> {
                tvBienvenida.setText("¡Bienvenido!");
            });
        }
    }

    private void goToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
