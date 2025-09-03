package com.finalProyecto.appjonay;

import android.os.Bundle;
import android.view.View;
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

        // 1) Obtener el dato que venga (nuevo flujo u otros)
        String raw = getIntent().getStringExtra(EscanearAlbaranActivity.EXTRA_ALBARAN_RAW);
        if (raw == null) raw = getIntent().getStringExtra("albaranJson"); // compatibilidad

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
                cargarDesdeFirestorePorId(raw);
            }
        } else {
            cargarDesdeFirestorePorId(raw);
        }

        // 3) Botón cerrar
        Button btnCerrar = findViewById(R.id.btnCerrarDetalle);
        if (btnCerrar != null) btnCerrar.setOnClickListener(v -> finish());
    }

    // ---------- PINTAR DESDE JSON (oculta vistas vacías) ----------
    private void pintarDesdeJSON(JSONObject obj) {
        String numero     = optAny(obj, "numeroAlbaran", "id_albaran", "id_albarán", "albaranId", "id");
        String fecha      = optAny(obj, "fecha");
        String nombre     = optAny(obj, "nombreCliente", "cliente", "nombre");
        String apellidos  = optAny(obj, "apellidosCliente", "apellidos");
        String direccion  = optAny(obj, "direccion", "dirección");
        String cp         = optAny(obj, "cp", "codigo_postal", "código_postal");
        String localidad  = optAny(obj, "localidad", "poblacion", "población", "ciudad", "provincia");
        String telefono   = optAny(obj, "telefono", "teléfono", "phone");
        String email      = optAny(obj, "email", "correo");
        String dni        = optAny(obj, "dni");
        String cif        = optAny(obj, "cif");
        String producto   = optAny(obj, "producto");
        String cantidad   = optAny(obj, "cantidad", "qty");
        String observ     = optAny(obj, "observaciones", "notas");
        String estado     = optAny(obj, "estado");

        String cliente = buildNombreCompleto(nombre, apellidos);

        show(R.id.tvNumeroAlbaran, "Número: ",   numero);
        show(R.id.tvFecha,         "Fecha: ",    fecha);
        show(R.id.tvCliente,       "Cliente: ",  cliente);
        show(R.id.tvDireccion,     "Dirección: ",direccion);
        show(R.id.tvCP,            "CP: ",       cp);          // <- requiere TextView con id tvCP
        show(R.id.tvLocalidad,     "Localidad: ",localidad);   // <- requiere TextView con id tvLocalidad
        show(R.id.tvTelefono,      "Teléfono: ", telefono);
        show(R.id.tvEmail,         "Email: ",    email);
        show(R.id.tvDNI,           "DNI: ",      dni);
        show(R.id.tvCIF,           "CIF: ",      cif);
        show(R.id.tvProducto,      "Producto: ", producto);
        show(R.id.tvCantidad,      "Cantidad: ", cantidad);
        show(R.id.tvObservaciones, "Observaciones: ", observ);
        show(R.id.tvEstado,        "Estado: ",   estado);
    }

    // ---------- CARGAR DESDE FIRESTORE (oculta vistas vacías) ----------
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
                                        // No encontrado: muestra sólo el número con nota y oculta el resto
                                        show(R.id.tvNumeroAlbaran, "Número: ", posibleId + " (no encontrado)");
                                        hide(R.id.tvFecha);
                                        hide(R.id.tvCliente);
                                        hide(R.id.tvDireccion);
                                        hide(R.id.tvCP);
                                        hide(R.id.tvLocalidad);
                                        hide(R.id.tvTelefono);
                                        hide(R.id.tvEmail);
                                        hide(R.id.tvDNI);
                                        hide(R.id.tvCIF);
                                        hide(R.id.tvProducto);
                                        hide(R.id.tvCantidad);
                                        hide(R.id.tvObservaciones);
                                        hide(R.id.tvEstado);
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    show(R.id.tvNumeroAlbaran, "Número: ", posibleId + " (error de carga)");
                });
    }

    private void pintarDesdeDoc(DocumentSnapshot doc) {
        // Ajusta a tu esquema de Firestore si usas otros nombres
        String numero     = safe(doc.getString("id_albaran"));
        String fecha      = safe(doc.getString("fecha"));
        String nombre     = safe(doc.getString("cliente"));
        String apellidos  = safe(doc.getString("apellidosCliente"));
        String direccion  = safe(doc.getString("direccion"));
        String cp         = safe(doc.getString("cp"));
        String localidad  = safe(doc.getString("localidad"));
        String telefono   = safe(doc.getString("telefono"));
        String email      = safe(doc.getString("email"));
        String dni        = safe(doc.getString("dni"));
        String cif        = safe(doc.getString("cif"));
        String producto   = safe(doc.getString("producto"));
        String cantidad   = safe(doc.getString("cantidad"));
        String observ     = safe(doc.getString("observaciones"));
        String estado     = safe(doc.getString("estado"));

        String cliente = buildNombreCompleto(nombre, apellidos);

        show(R.id.tvNumeroAlbaran, "Número: ",   numero);
        show(R.id.tvFecha,         "Fecha: ",    fecha);
        show(R.id.tvCliente,       "Cliente: ",  cliente);
        show(R.id.tvDireccion,     "Dirección: ",direccion);
        show(R.id.tvCP,            "CP: ",       cp);
        show(R.id.tvLocalidad,     "Localidad: ",localidad);
        show(R.id.tvTelefono,      "Teléfono: ", telefono);
        show(R.id.tvEmail,         "Email: ",    email);
        show(R.id.tvDNI,           "DNI: ",      dni);
        show(R.id.tvCIF,           "CIF: ",      cif);
        show(R.id.tvProducto,      "Producto: ", producto);
        show(R.id.tvCantidad,      "Cantidad: ", cantidad);
        show(R.id.tvObservaciones, "Observaciones: ", observ);
        show(R.id.tvEstado,        "Estado: ",   estado);
    }

    // ---------- HELPERS ----------
    private void show(int viewId, String label, String value) {
        TextView tv = findViewById(viewId);
        if (tv == null) return;
        String v = safe(value);
        if (v.isEmpty()) {
            tv.setVisibility(View.GONE);
        } else {
            tv.setVisibility(View.VISIBLE);
            tv.setText(label + v);
        }
    }

    private void hide(int viewId) {
        TextView tv = findViewById(viewId);
        if (tv != null) tv.setVisibility(View.GONE);
    }

    private String buildNombreCompleto(String nombre, String apellidos) {
        String n = safe(nombre);
        String a = safe(apellidos);
        if (n.isEmpty() && a.isEmpty()) return "";
        if (a.isEmpty()) return n;
        if (n.isEmpty()) return a;
        return (n + " " + a).trim();
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private String optAny(JSONObject obj, String... keys) {
        for (String k : keys) {
            if (obj.has(k)) {
                String v = obj.optString(k, "");
                if (v != null && !v.trim().isEmpty()) return v.trim();
            }
        }
        return "";
    }
}
