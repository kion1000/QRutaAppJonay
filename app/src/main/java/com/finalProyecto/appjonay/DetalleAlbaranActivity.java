package com.finalProyecto.appjonay;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.finalProyecto.appjonay.data.Stop;
import com.finalProyecto.appjonay.data.StopRepository;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONException;
import org.json.JSONObject;

public class DetalleAlbaranActivity extends AppCompatActivity {

    // Payload que se ve en pantalla (para "Añadir a la ruta")
    private JSONObject lastPayload = null;
    private Stop.Source lastSource = Stop.Source.MANUAL;

    private Button btnAddToRoute;
    private Button btnCerrar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_albaran);

        // ---- Referencias UI
        btnAddToRoute = findViewById(R.id.btnAnadirRuta);
        btnCerrar     = findViewById(R.id.btnCerrarDetalle);

        // ---- Configurar botones ANTES de pintar datos
        if (btnCerrar != null) btnCerrar.setOnClickListener(v -> finish());
        if (btnAddToRoute != null) {
            btnAddToRoute.setOnClickListener(v -> onAddToRouteClicked());
            // Arranca deshabilitado; se habilita al pintar si hay dirección válida
            updateAddToRouteButton(null);
        }

        // ---- Obtener el dato de entrada
        String raw = getIntent().getStringExtra(EscanearAlbaranActivity.EXTRA_ALBARAN_RAW);
        if (raw == null) raw = getIntent().getStringExtra("albaranJson");
        if (raw == null) { finish(); return; }

        // ---- Pintar según formato
        if (raw.trim().startsWith("{")) {
            try {
                JSONObject obj = new JSONObject(raw);
                lastPayload = obj;
                lastSource  = Stop.Source.OCR;
                pintarDesdeJSON(obj);
            } catch (JSONException e) {
                cargarDesdeFirestorePorId(raw);
            }
        } else {
            cargarDesdeFirestorePorId(raw);
        }
    }

    // ======================= Acciones =======================

    private void onAddToRouteClicked() {
        if (lastPayload == null) {
            Toast.makeText(this, "No hay datos para añadir", Toast.LENGTH_SHORT).show();
            return;
        }
        String direccion = lastPayload.optString("direccion", "");
        if (direccion == null || direccion.trim().isEmpty()) {
            Toast.makeText(this, "Completa la dirección antes de añadir", Toast.LENGTH_SHORT).show();
            return;
        }

        Stop stop = Stop.fromJson(lastPayload, lastSource);
        StopRepository.get().add(stop);

        Toast.makeText(this, "Albarán añadido a la ruta", Toast.LENGTH_SHORT).show();
        // Si quieres volver atrás automáticamente:
        // finish();
    }

    // ======================= Pintado =======================

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

        // Habilitar el botón según dirección
        updateAddToRouteButton(direccion);

        // Guardar payload visible
        lastPayload = obj;
    }

    private void cargarDesdeFirestorePorId(String posibleId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("albaranes").document(posibleId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        pintarDesdeDoc(doc);
                    } else {
                        db.collection("albaranes")
                                .whereEqualTo("id_albaran", posibleId)
                                .limit(1)
                                .get()
                                .addOnSuccessListener(qs -> {
                                    if (!qs.isEmpty()) {
                                        pintarDesdeDoc(qs.getDocuments().get(0));
                                    } else {
                                        show(R.id.tvNumeroAlbaran, "Número: ", posibleId + " (no encontrado)");
                                        hide(R.id.tvFecha); hide(R.id.tvCliente); hide(R.id.tvDireccion);
                                        hide(R.id.tvCP); hide(R.id.tvLocalidad); hide(R.id.tvTelefono);
                                        hide(R.id.tvEmail); hide(R.id.tvDNI); hide(R.id.tvCIF);
                                        hide(R.id.tvProducto); hide(R.id.tvCantidad);
                                        hide(R.id.tvObservaciones); hide(R.id.tvEstado);
                                        updateAddToRouteButton(null);
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    show(R.id.tvNumeroAlbaran, "Número: ", posibleId + " (error de carga)");
                    updateAddToRouteButton(null);
                });
    }

    private void pintarDesdeDoc(DocumentSnapshot doc) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("id_albaran",   safe(doc.getString("id_albaran")));
            obj.put("fecha",        safe(doc.getString("fecha")));
            obj.put("cliente",      safe(doc.getString("cliente")));
            obj.put("apellidos",    safe(doc.getString("apellidosCliente")));
            obj.put("direccion",    safe(doc.getString("direccion")));
            obj.put("cp",           safe(doc.getString("cp")));
            obj.put("localidad",    safe(doc.getString("localidad")));
            obj.put("telefono",     safe(doc.getString("telefono")));
            obj.put("email",        safe(doc.getString("email")));
            obj.put("dni",          safe(doc.getString("dni")));
            obj.put("cif",          safe(doc.getString("cif")));
            obj.put("producto",     safe(doc.getString("producto")));
            obj.put("cantidad",     safe(doc.getString("cantidad")));
            obj.put("observaciones",safe(doc.getString("observaciones")));
            obj.put("estado",       safe(doc.getString("estado")));
        } catch (JSONException ignored) {}

        lastPayload = obj;
        lastSource  = Stop.Source.QR;

        pintarDesdeJSON(obj);
    }

    // ======================= Helpers UI =======================

    private void updateAddToRouteButton(@Nullable String direccion) {
        boolean enable = direccion != null && direccion.trim().length() >= 5;
        if (btnAddToRoute != null) {
            btnAddToRoute.setEnabled(enable);
            // feedback visual
            btnAddToRoute.setAlpha(enable ? 1f : 0.5f);
        }
    }

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
