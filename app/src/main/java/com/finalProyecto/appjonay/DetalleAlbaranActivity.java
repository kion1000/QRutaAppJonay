package com.finalProyecto.appjonay;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONException;
import org.json.JSONObject;

public class DetalleAlbaranActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_albaran);

        // 1) Obtener el dato que venga (nuevo flujo o el antiguo)
        String raw = getIntent().getStringExtra(EscanearAlbaranActivity.EXTRA_ALBARAN_RAW);
        if (raw == null) {
            raw = getIntent().getStringExtra("albaranJson"); // compatibilidad con tu flujo anterior
        }
        if (raw == null) {
            finish();
            return;
        }

        // 2) Si parece JSON -> parsear y pintar; si no -> tratar como ID y buscar en Firestore
        if (raw.trim().startsWith("{")) {
            try {
                JSONObject obj = new JSONObject(raw);
                pintarDesdeJSON(obj);
            } catch (JSONException e) {
                // JSON inválido: plan B, tratar como ID
                cargarDesdeFirestorePorId(raw);
            }
        } else {
            // ID simple
            cargarDesdeFirestorePorId(raw);
        }

        // 3) Botón cerrar (si existe en tu XML)
        Button btnCerrar = findViewById(R.id.btnCerrarDetalle);
        if (btnCerrar != null) {
            btnCerrar.setOnClickListener(v -> finish());
        }
    }

    // ---------- PINTAR DESDE JSON ----------
    private void pintarDesdeJSON(JSONObject obj) {
        // Usamos claves flexibles: si no existe una, prueba otras variantes
        String numero   = optAny(obj, "numeroAlbaran", "id_albaran", "id_albarán", "albaranId", "id");
        String fecha    = optAny(obj, "fecha");
        String nombre   = optAny(obj, "nombreCliente", "cliente", "nombre");
        String apellidos= optAny(obj, "apellidosCliente", "apellidos");
        String direccion= optAny(obj, "direccion", "dirección");
        String telefono = optAny(obj, "telefono", "teléfono", "phone");
        String email    = optAny(obj, "email", "correo");
        String dni      = optAny(obj, "dni");
        String cif      = optAny(obj, "cif");
        String producto = optAny(obj, "producto");
        String cantidad = optAny(obj, "cantidad", "qty");
        String observ   = optAny(obj, "observaciones", "notas");
        String estado   = optAny(obj, "estado");

        setLabeledText(R.id.tvNumeroAlbaran,  "Número: ", numero);
        setLabeledText(R.id.tvFecha,          "Fecha: ", fecha);
        setLabeledText(R.id.tvCliente,        "Cliente: ", (nombre + (apellidos.isEmpty() ? "" : " " + apellidos)).trim());
        setLabeledText(R.id.tvDireccion,      "Dirección: ", direccion);
        setLabeledText(R.id.tvTelefono,       "Teléfono: ", telefono);
        setLabeledText(R.id.tvEmail,          "Email: ", email);
        setLabeledText(R.id.tvDNI,            "DNI: ", dni);
        setLabeledText(R.id.tvCIF,            "CIF: ", cif);
        setLabeledText(R.id.tvProducto,       "Producto: ", producto);
        setLabeledText(R.id.tvCantidad,       "Cantidad: ", cantidad);
        setLabeledText(R.id.tvObservaciones,  "Observaciones: ", observ);
        setLabeledText(R.id.tvEstado,         "Estado: ", estado);
    }

    // ---------- CARGAR DESDE FIRESTORE ----------
    private void cargarDesdeFirestorePorId(String posibleId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 1) Probar como documentId directo
        db.collection("albaranes").document(posibleId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        pintarDesdeDoc(doc);
                    } else {
                        // 2) Buscar por campo id_albaran == posibleId
                        db.collection("albaranes")
                                .whereEqualTo("id_albaran", posibleId)
                                .limit(1)
                                .get()
                                .addOnSuccessListener(qs -> {
                                    if (!qs.isEmpty()) {
                                        pintarDesdeDoc(qs.getDocuments().get(0));
                                    } else {
                                        // No encontrado: pinta algo informativo si quieres
                                        setLabeledText(R.id.tvNumeroAlbaran, "Número: ", posibleId + " (no encontrado)");
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    setLabeledText(R.id.tvNumeroAlbaran, "Número: ", posibleId + " (error de carga)");
                });
    }

    private void pintarDesdeDoc(DocumentSnapshot doc) {
        // Ajusta estos nombres a tu esquema real de Firestore
        String numero   = safe(doc.getString("id_albaran"));
        String fecha    = safe(doc.getString("fecha"));
        String nombre   = safe(doc.getString("cliente")); // o "nombreCliente"
        String apellidos= safe(doc.getString("apellidosCliente"));
        String direccion= safe(doc.getString("direccion"));
        String telefono = safe(doc.getString("telefono"));
        String email    = safe(doc.getString("email"));
        String dni      = safe(doc.getString("dni"));
        String cif      = safe(doc.getString("cif"));
        String producto = safe(doc.getString("producto"));
        String cantidad = safe(doc.getString("cantidad"));
        String observ   = safe(doc.getString("observaciones"));
        String estado   = safe(doc.getString("estado"));

        setLabeledText(R.id.tvNumeroAlbaran,  "Número: ", numero);
        setLabeledText(R.id.tvFecha,          "Fecha: ", fecha);
        setLabeledText(R.id.tvCliente,        "Cliente: ", (nombre + (apellidos.isEmpty() ? "" : " " + apellidos)).trim());
        setLabeledText(R.id.tvDireccion,      "Dirección: ", direccion);
        setLabeledText(R.id.tvTelefono,       "Teléfono: ", telefono);
        setLabeledText(R.id.tvEmail,          "Email: ", email);
        setLabeledText(R.id.tvDNI,            "DNI: ", dni);
        setLabeledText(R.id.tvCIF,            "CIF: ", cif);
        setLabeledText(R.id.tvProducto,       "Producto: ", producto);
        setLabeledText(R.id.tvCantidad,       "Cantidad: ", cantidad);
        setLabeledText(R.id.tvObservaciones,  "Observaciones: ", observ);
        setLabeledText(R.id.tvEstado,         "Estado: ", estado);
    }

    // ---------- HELPERS ----------
    private void setLabeledText(int viewId, String label, String value) {
        TextView tv = findViewById(viewId);
        if (tv != null) tv.setText(label + safe(value));
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private String optAny(JSONObject obj, String... keys) {
        for (String k : keys) {
            if (obj.has(k)) {
                String v = obj.optString(k, "");
                if (v != null && !v.isEmpty()) return v;
            }
        }
        return "";
    }
}
