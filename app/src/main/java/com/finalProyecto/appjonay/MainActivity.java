package com.finalProyecto.appjonay;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
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

                        // Bienvenida
                        mostrarBienvenida();

                        // --- Cerrar sesión (Firebase + Google) ---
                        Button btnCerrarSesion = findViewById(R.id.btnCerrarSesion);
                        if (btnCerrarSesion != null) {
                            btnCerrarSesion.setOnClickListener(v -> {
                                mAuth.signOut();
                                try {
                                    getGoogleClient().signOut().addOnCompleteListener(t -> goToLogin());
                                } catch (Exception e) {
                                    goToLogin();
                                }
                            });
                        }

                        // --- Escanear por QR ---
                        Button btnEscanear = findViewById(R.id.btnEscanear);
                        if (btnEscanear != null) {
                            btnEscanear.setOnClickListener(v ->
                                    startActivity(new Intent(MainActivity.this, EscanearAlbaranActivity.class))
                            );
                        }

                        // --- Escanear por OCR (ML Kit) ---
                        Button btnEscanearOcr = findViewById(R.id.btnEscanearOcr);
                        if (btnEscanearOcr != null) {
                            btnEscanearOcr.setOnClickListener(v ->
                                    startActivity(new Intent(MainActivity.this, OcrAlbaranActivity.class))
                            );
                        }

                        // --- ✅ NUEVO: Entrada manual ---
                        Button btnEntradaManual = findViewById(R.id.btnEntradaManual);
                        if (btnEntradaManual != null) {
                            btnEntradaManual.setOnClickListener(v ->
                                    startActivity(new Intent(MainActivity.this, ManualAlbaranActivity.class))
                            );
                        }
                    }
                } else {
                    Log.e("MainActivity", "Error al recargar usuario: ", task.getException());
                    mAuth.signOut();
                    try {
                        getGoogleClient().signOut().addOnCompleteListener(t -> goToLogin());
                    } catch (Exception e) {
                        goToLogin();
                    }
                }
            });
        }
    }

    // Bienvenida basada en FirebaseAuth + Firestore
    private void mostrarBienvenida() {
        TextView tvBienvenida = findViewById(R.id.tvBienvenida);

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            if (tvBienvenida != null) tvBienvenida.setText("¡Bienvenido!");
            return;
        }

        String displayName = user.getDisplayName();
        if (displayName != null && !displayName.isEmpty()) {
            if (tvBienvenida != null) tvBienvenida.setText("¡Bienvenido, " + displayName + "!");
            return;
        }

        mostrarNombreUsuarioDesdeFirestore(tvBienvenida);
    }

    private void mostrarNombreUsuarioDesdeFirestore(TextView tvBienvenida) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            DocumentReference userRef = db.collection("usuarios").document(uid);

            userRef.get().addOnSuccessListener(documentSnapshot -> {
                if (tvBienvenida == null) return;

                if (documentSnapshot.exists()) {
                    String nombre = documentSnapshot.getString("nombre");
                    String apellidos = documentSnapshot.getString("apellidos");
                    if (nombre != null && apellidos != null) {
                        tvBienvenida.setText("¡Bienvenido, " + nombre.toUpperCase() + " " + apellidos.toUpperCase() + "!");
                    } else if (nombre != null && !nombre.isEmpty()) {
                        tvBienvenida.setText("¡Bienvenido, " + nombre.toUpperCase() + "!");
                    } else {
                        tvBienvenida.setText("¡Bienvenido, " + aliasDesdeEmail(currentUser) + "!");
                    }
                } else {
                    tvBienvenida.setText("¡Bienvenido, " + aliasDesdeEmail(currentUser) + "!");
                }
            }).addOnFailureListener(e -> {
                if (tvBienvenida != null) {
                    tvBienvenida.setText("¡Bienvenido, " + aliasDesdeEmail(currentUser) + "!");
                }
            });
        }
    }

    private String aliasDesdeEmail(FirebaseUser user) {
        String email = user.getEmail();
        if (email == null) return "";
        int at = email.indexOf('@');
        return at > 0 ? email.substring(0, at) : email;
    }

    private GoogleSignInClient getGoogleClient() {
        GoogleSignInOptions gso = new GoogleSignInOptions
                .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
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
