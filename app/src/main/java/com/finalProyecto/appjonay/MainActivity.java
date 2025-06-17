package com.finalProyecto.appjonay;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

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

                        // --- Añadir botón cerrar sesión ---
                        Button btnCerrarSesion = findViewById(R.id.btnCerrarSesion);
                        btnCerrarSesion.setOnClickListener(v -> {
                            mAuth.signOut();
                            goToLogin();
                        });
                        // -----------------------------------
                    }
                } else {
                    Log.e("MainActivity", "Error al recargar usuario: ", task.getException());
                    mAuth.signOut();
                    goToLogin();
                }
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
