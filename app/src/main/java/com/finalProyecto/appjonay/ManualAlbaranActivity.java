package com.finalProyecto.appjonay;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;
import java.util.regex.Pattern;

public class ManualAlbaranActivity extends AppCompatActivity {

    private EditText etCliente, etDireccion, etCP, etLocalidad, etTelefono, etEmail, etObs;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual_albaran);

        etCliente   = findViewById(R.id.etCliente);
        etDireccion = findViewById(R.id.etDireccion);
        etCP        = findViewById(R.id.etCP);
        etLocalidad = findViewById(R.id.etLocalidad);
        etTelefono  = findViewById(R.id.etTelefono);
        etEmail     = findViewById(R.id.etEmail);
        etObs       = findViewById(R.id.etObservaciones);

        Button btnGuardar = findViewById(R.id.btnGuardarManual);
        Button btnCancelar= findViewById(R.id.btnCancelarManual);

        btnGuardar.setOnClickListener(v -> onGuardar());
        btnCancelar.setOnClickListener(v -> finish());
    }

    private void onGuardar() {
        String cliente   = safe(etCliente);
        String direccion = safe(etDireccion);
        String cp        = safe(etCP);
        String localidad = safe(etLocalidad);
        String telefono  = safe(etTelefono);
        String email     = safe(etEmail);
        String obs       = safe(etObs);

        // Validaciones mínimas
        if (TextUtils.isEmpty(direccion)) {
            etDireccion.setError("La dirección es obligatoria");
            etDireccion.requestFocus();
            return;
        }
        if (!cp.matches("(?:0[1-9]|[1-4][0-9]|5[0-2])\\d{3}")) {
            etCP.setError("CP inválido (01000–52999)");
            etCP.requestFocus();
            return;
        }
        if (!telefono.isEmpty()) {
            String t = telefono.replaceAll("[\\s.-]", "");
            if (!t.matches("(?:\\+?34)?\\d{9}")) {
                etTelefono.setError("Teléfono no válido");
                etTelefono.requestFocus();
                return;
            }
        }
        if (!email.isEmpty()) {
            Pattern p = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",
                    Pattern.CASE_INSENSITIVE);
            if (!p.matcher(email).find()) {
                etEmail.setError("Email no válido");
                etEmail.requestFocus();
                return;
            }
        }

        try {
            JSONObject o = new JSONObject();
            o.put("cliente", cliente);
            o.put("direccion", direccion);
            o.put("cp", cp);
            o.put("localidad", localidad);
            o.put("telefono", telefono.replaceAll("[\\s.-]", ""));
            o.put("email", email);
            o.put("observaciones", obs);
            o.put("origen", "MANUAL");

            Intent i = new Intent(this, DetalleAlbaranActivity.class);
            i.putExtra(EscanearAlbaranActivity.EXTRA_ALBARAN_RAW, o.toString());
            startActivity(i);
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "Error creando JSON: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String safe(EditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }
}
