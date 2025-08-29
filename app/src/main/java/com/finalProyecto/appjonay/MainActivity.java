package com.finalProyecto.appjonay;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;             // ✅ lo usamos para el client
import com.google.android.gms.auth.api.signin.GoogleSignInClient;      // ✅ NUEVO
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;     // ✅ NUEVO
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

                        // Mostrar mensaje de bienvenida (⚠️ solo desde Firebase/Firestore)
                        mostrarBienvenida();

                        // Botón cerrar sesión: Firebase + Google
                        Button btnCerrarSesion = findViewById(R.id.btnCerrarSesion);
                        btnCerrarSesion.setOnClickListener(v -> {
                            // 1) Firebase fuera
                            mAuth.signOut();
                            // 2) Google fuera (si no hubo Google, no pasa nada)
                            try {
                                getGoogleClient().signOut().addOnCompleteListener(t -> goToLogin());
                            } catch (Exception e) {
                                // En dispositivos sin GMS no pasa nada
                                goToLogin();
                            }
                        });

                        // Botón ESCANEAR ALBARÁN
                        Button btnEscanear = findViewById(R.id.btnEscanear);
                        btnEscanear.setOnClickListener(v -> {
                            Intent intent = new Intent(MainActivity.this, EscanearAlbaranActivity.class);
                            startActivity(intent);
                        });
                    }
                } else {
                    Log.e("MainActivity", "Error al recargar usuario: ", task.getException());
                    mAuth.signOut();
                    // Intenta también cerrar sesión de Google por si estaba activo
                    try {
                        getGoogleClient().signOut().addOnCompleteListener(t -> goToLogin());
                    } catch (Exception e) {
                        goToLogin();
                    }
                }
            });
        }
    }

    // ✅ Bienvenida SIEMPRE basada en FirebaseAuth + Firestore
    private void mostrarBienvenida() {
        TextView tvBienvenida = findViewById(R.id.tvBienvenida);

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            tvBienvenida.setText("¡Bienvenido!");
            return;
        }

        // Si el proveedor fue Google, FirebaseUser.getDisplayName() suele venir relleno
        String displayName = user.getDisplayName();
        if (displayName != null && !displayName.isEmpty()) {
            tvBienvenida.setText("¡Bienvenido, " + displayName + "!");
            return;
        }

        // Si no hay displayName (email/contraseña): Firestore o alias de email
        mostrarNombreUsuarioDesdeFirestore(tvBienvenida);
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
                    } else if (nombre != null && !nombre.isEmpty()) {
                        tvBienvenida.setText("¡Bienvenido, " + nombre.toUpperCase() + "!");
                    } else {
                        // Fallback: alias desde email
                        tvBienvenida.setText("¡Bienvenido, " + aliasDesdeEmail(currentUser) + "!");
                    }
                } else {
                    // Fallback: alias desde email
                    tvBienvenida.setText("¡Bienvenido, " + aliasDesdeEmail(currentUser) + "!");
                }
            }).addOnFailureListener(e -> {
                tvBienvenida.setText("¡Bienvenido, " + aliasDesdeEmail(currentUser) + "!");
            });
        }
    }

    private String aliasDesdeEmail(FirebaseUser user) {
        String email = user.getEmail();
        if (email == null) return "";
        int at = email.indexOf('@');
        return at > 0 ? email.substring(0, at) : email;
    }

    // ✅ Cliente de Google para cerrar sesión si procede
    private GoogleSignInClient getGoogleClient() {
        GoogleSignInOptions gso = new GoogleSignInOptions
                .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail() // no hace falta requestIdToken para signOut
                .build();
        return GoogleSignIn.getClient(this, gso);
    }

    private void goToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
