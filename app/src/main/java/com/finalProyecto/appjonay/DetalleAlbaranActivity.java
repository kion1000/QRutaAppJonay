package com.finalProyecto.appjonay;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.finalProyecto.appjonay.data.Stop;
import com.finalProyecto.appjonay.data.StopRepository;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.finalProyecto.appjonay.data.CloudRouteStore;


public class DetalleAlbaranActivity extends AppCompatActivity {

    private JSONObject lastPayload = null;          // siempre normalizado
    private Stop.Source lastSource = Stop.Source.MANUAL;

    private Button btnAddToRoute;
    private Button btnCerrar;

    private View panelLectura, panelEdicion;
    private Button btnEditar, btnGuardarEdicion, btnCancelarEdicion;

    private TextInputLayout tilCliente, tilDireccion, tilCP, tilLocalidad, tilTelefono, tilEmail, tilNotas;
    private TextInputEditText etCliente, etDireccion, etCP, etLocalidad, etTelefono, etEmail, etNotas;

    private TextView tvAvisoGuardar;

    private boolean editing = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_albaran);

        // Botones acción
        btnAddToRoute = findViewById(R.id.btnAnadirRuta);
        btnCerrar     = findViewById(R.id.btnCerrarDetalle);

        // Paneles
        panelLectura  = findViewById(R.id.panelLectura);
        panelEdicion  = findViewById(R.id.panelEdicion);

        // Botones edición
        btnEditar          = findViewById(R.id.btnEditar);
        btnGuardarEdicion  = findViewById(R.id.btnGuardarEdicion);
        btnCancelarEdicion = findViewById(R.id.btnCancelarEdicion);

        tvAvisoGuardar = findViewById(R.id.tvAvisoGuardar);

        // Campos edición
        tilCliente   = findViewById(R.id.tilCliente);
        tilDireccion = findViewById(R.id.tilDireccion);
        tilCP        = findViewById(R.id.tilCP);
        tilLocalidad = findViewById(R.id.tilLocalidad);
        tilTelefono  = findViewById(R.id.tilTelefono);
        tilEmail     = findViewById(R.id.tilEmail);
        tilNotas     = findViewById(R.id.tilNotas);

        etCliente   = findViewById(R.id.etCliente);
        etDireccion = findViewById(R.id.etDireccion);
        etCP        = findViewById(R.id.etCP);
        etLocalidad = findViewById(R.id.etLocalidad);
        etTelefono  = findViewById(R.id.etTelefono);
        etEmail     = findViewById(R.id.etEmail);
        etNotas     = findViewById(R.id.etNotas);

        // 1) Obtener el dato que venga (nuevo flujo u otros)
        String raw = getIntent().getStringExtra(EscanearAlbaranActivity.EXTRA_ALBARAN_RAW);
        if (raw == null) raw = getIntent().getStringExtra("albaranJson"); // compat

        if (raw == null) {
            lastPayload = null;
            updateSaveUI();
            finish();
            return;
        }

        // 2) JSON -> normalizar y pintar; si no -> tratar como ID y buscar en Firestore
        if (raw.trim().startsWith("{")) {
            try {
                JSONObject obj = new JSONObject(raw);
                lastPayload = normalizeKeys(obj);      // 🔴 CLAVE: normalizar SIEMPRE
                lastSource  = Stop.Source.OCR;
                pintarDesdeJSON(lastPayload);
                if (btnEditar != null) btnEditar.setEnabled(true);
            } catch (JSONException e) {
                cargarDesdeFirestorePorId(raw);
            }
        } else {
            cargarDesdeFirestorePorId(raw);
        }

        // 3) Botones
        if (btnCerrar != null) btnCerrar.setOnClickListener(v -> finish());

        if (btnAddToRoute != null) {
            btnAddToRoute.setOnClickListener(v -> onAddToRouteClicked());
        }

        if (btnEditar != null) {
            btnEditar.setOnClickListener(v -> {
                if (lastPayload == null) {
                    Toast.makeText(this, "No hay datos para editar", Toast.LENGTH_SHORT).show();
                    return;
                }
                enterEditMode();
            });
        }

        if (btnGuardarEdicion != null) {
            btnGuardarEdicion.setOnClickListener(v -> onSaveEdits());
        }

        if (btnCancelarEdicion != null) {
            btnCancelarEdicion.setOnClickListener(v -> exitEditMode());
        }

        // Fijar estado final del botón tras procesar raw y listeners
        updateSaveUI();
    }

    // ---------- Acción: Añadir a la ruta ----------
    private void onAddToRouteClicked() {
        if (btnAddToRoute != null) btnAddToRoute.setEnabled(false); // anti doble tap

        if (lastPayload == null) {
            Toast.makeText(this, "No hay datos para añadir", Toast.LENGTH_SHORT).show();
            updateSaveUI();
            return;
        }

        // Leer canónicas (gracias a normalizeKeys); fallback a variantes
        String direccion = safe(lastPayload.optString("direccion", getPayloadAny("direccion","dirección","Direccion","Dirección","address")));
        String cp        = safe(lastPayload.optString("cp",        getPayloadAny("cp","CP","codigo_postal","código_postal","codigoPostal","postal_code","postalCode","zip")));
        String loc       = safe(lastPayload.optString("localidad", getPayloadAny("localidad","Localidad","poblacion","población","ciudad","municipio","city","pueblo")));

        String falta = "";
        if (TextUtils.isEmpty(direccion)) falta = appendFalta(falta, "Dirección");
        if (TextUtils.isEmpty(cp) && TextUtils.isEmpty(loc)) falta = appendFalta(falta, "CP o Localidad");

        if (!falta.isEmpty()) {
            Toast.makeText(this, "Completa: " + falta, Toast.LENGTH_SHORT).show();
            updateSaveUI();
            return;
        }

        Stop stop = Stop.fromJson(lastPayload, lastSource);

        // Dedupe local antes de bloquear definitivamente
        if (StopRepository.get().existsDuplicate(stop)) {
            Toast.makeText(this, "Este albarán ya está en la ruta", Toast.LENGTH_SHORT).show();
            updateSaveUI();
            return;
        }

        // Añadir local evitando duplicados
        boolean added = StopRepository.get().addUnique(stop);
        if (!added) {
            Toast.makeText(this, "Este albarán ya estaba en la ruta", Toast.LENGTH_SHORT).show();
            updateSaveUI();
            return;
        }

        Toast.makeText(this, "Añadido en el móvil. Sincronizando…", Toast.LENGTH_SHORT).show();

        // === Subida a Firestore ===
        String uid = CloudRouteStore.currentUid();
        if (uid == null) {
            // Sin sesión: solo local
            Toast.makeText(this, "Sin sesión: guardado localmente", Toast.LENGTH_SHORT).show();
            updateSaveUI();
            return;
        }

        setSavingState(true);
        String dayKey = CloudRouteStore.todayKey();
        CloudRouteStore.saveStop(uid, dayKey, stop, (ok, e) -> {
            setSavingState(false);
            if (ok) {
                Toast.makeText(this, "Sincronizado con la nube ✔", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Guardado local. Se subirá cuando haya conexión", Toast.LENGTH_LONG).show();
            }
            updateSaveUI();
        });
    }


    // ---------- PINTAR DESDE JSON ----------
    private void pintarDesdeJSON(JSONObject obj) {
        // obj ya viene normalizado: usamos variantes sólo para mostrar si faltara algo
        String numero     = optAny(obj, "id_albaran", "numeroAlbaran", "albaranId", "id");
        String fecha      = optAny(obj, "fecha");
        String nombre     = optAny(obj, "cliente", "nombre", "nombreCliente");
        String apellidos  = optAny(obj, "apellidos", "apellidosCliente");
        String direccion  = optAny(obj, "direccion", "dirección", "address");
        String cp         = optAny(obj, "cp", "codigo_postal", "código_postal", "postal_code", "postalCode", "zip");
        String localidad  = optAny(obj, "localidad", "poblacion", "población", "ciudad", "municipio", "city");
        String telefono   = optAny(obj, "telefono", "teléfono", "phone");
        String email      = optAny(obj, "email", "correo", "mail");
        String dni        = optAny(obj, "dni");
        String cif        = optAny(obj, "cif");
        String producto   = optAny(obj, "producto");
        String cantidad   = optAny(obj, "cantidad", "qty");
        String observ     = optAny(obj, "observaciones", "notas", "comments");
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

        if (btnEditar != null) btnEditar.setEnabled(true);

        updateSaveUI();
    }

    // ---------- CARGAR DESDE FIRESTORE ----------
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

                                        lastPayload = null;
                                        if (btnEditar != null) btnEditar.setEnabled(false);
                                        updateSaveUI();
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    show(R.id.tvNumeroAlbaran, "Número: ", posibleId + " (error de carga)");
                    lastPayload = null;
                    if (btnEditar != null) btnEditar.setEnabled(false);
                    updateSaveUI();
                });
    }

    private void pintarDesdeDoc(DocumentSnapshot doc) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("id_albaran", safe(doc.getString("id_albaran")));
            obj.put("fecha",      safe(doc.getString("fecha")));
            obj.put("cliente",    safe(doc.getString("cliente")));
            obj.put("apellidos",  safe(doc.getString("apellidosCliente")));
            obj.put("direccion",  safe(doc.getString("direccion")));
            obj.put("cp",         safe(doc.getString("cp")));
            obj.put("localidad",  safe(doc.getString("localidad")));
            obj.put("telefono",   safe(doc.getString("telefono")));
            obj.put("email",      safe(doc.getString("email")));
            obj.put("dni",        safe(doc.getString("dni")));
            obj.put("cif",        safe(doc.getString("cif")));
            obj.put("producto",   safe(doc.getString("producto")));
            obj.put("cantidad",   safe(doc.getString("cantidad")));
            obj.put("observaciones", safe(doc.getString("observaciones")));
            obj.put("estado",     safe(doc.getString("estado")));
        } catch (JSONException ignored) {}

        lastPayload = normalizeKeys(obj);  // 🔴 normalizar lo leído
        lastSource  = Stop.Source.QR;

        if (btnEditar != null) btnEditar.setEnabled(true);
        pintarDesdeJSON(lastPayload);
    }

    // =================== MODO EDICIÓN ===================

    private void enterEditMode() {
        editing = true;

        String cliente   = getPayloadAny("cliente", "nombre", "nombreCliente");
        String direccion = getPayloadAny("direccion", "dirección", "address");
        String cp        = getPayloadAny("cp", "codigo_postal", "código_postal", "postal_code", "postalCode", "zip");
        String localidad = getPayloadAny("localidad", "poblacion", "población", "ciudad", "municipio", "city");
        String telefono  = getPayloadAny("telefono", "teléfono", "phone");
        String email     = getPayloadAny("email", "correo", "mail");
        String notas     = getPayloadAny("observaciones", "notas", "comments");

        setText(etCliente, cliente);
        setText(etDireccion, direccion);
        setText(etCP, cp);
        setText(etLocalidad, localidad);
        setText(etTelefono, telefono);
        setText(etEmail, email);
        setText(etNotas, notas);

        clearErrors();

        panelLectura.setVisibility(View.GONE);
        panelEdicion.setVisibility(View.VISIBLE);
        btnEditar.setVisibility(View.GONE);
        btnGuardarEdicion.setVisibility(View.VISIBLE);
        btnCancelarEdicion.setVisibility(View.VISIBLE);

        if (btnAddToRoute != null) {
            btnAddToRoute.setEnabled(false);
            btnAddToRoute.setText("Añadir a la ruta");
        }

        if (tvAvisoGuardar != null) tvAvisoGuardar.setVisibility(View.GONE);
    }

    private void exitEditMode() {
        editing = false;
        clearErrors();

        panelEdicion.setVisibility(View.GONE);
        panelLectura.setVisibility(View.VISIBLE);
        btnGuardarEdicion.setVisibility(View.GONE);
        btnCancelarEdicion.setVisibility(View.GONE);
        btnEditar.setVisibility(View.VISIBLE);

        updateSaveUI();
    }

    private void onSaveEdits() {
        if (lastPayload == null) return;

        clearErrors();

        String cliente   = getText(etCliente);
        String direccion = getText(etDireccion);
        String cp        = getText(etCP);
        String localidad = getText(etLocalidad);
        String telefono  = getText(etTelefono);
        String email     = getText(etEmail);
        String notas     = getText(etNotas);

        boolean ok = true;

        if (TextUtils.isEmpty(direccion)) { tilDireccion.setError("La dirección es obligatoria"); ok = false; }
        boolean hasCpOrLoc = !TextUtils.isEmpty(cp) || !TextUtils.isEmpty(localidad);
        if (!hasCpOrLoc) { tilCP.setError("Indica CP o Localidad"); tilLocalidad.setError("Indica CP o Localidad"); ok = false; }
        if (!TextUtils.isEmpty(cp) && !isValidCP(cp)) { tilCP.setError("CP inválido (ej: 35005)"); ok = false; }
        if (!TextUtils.isEmpty(telefono) && !isValidTelefono(telefono)) { tilTelefono.setError("Teléfono inválido"); ok = false; }
        if (!TextUtils.isEmpty(email) && !isValidEmail(email)) { tilEmail.setError("Email inválido"); ok = false; }

        if (!ok) return;

        try {
            lastPayload.put("cliente", cliente);
            lastPayload.put("direccion", direccion);
            lastPayload.put("cp", cp);
            lastPayload.put("localidad", localidad);
            lastPayload.put("telefono", telefono);
            lastPayload.put("email", email);
            lastPayload.put("observaciones", notas);
        } catch (JSONException ignored) {}

        pintarDesdeJSON(lastPayload);
        exitEditMode();

        Toast.makeText(this, "Cambios guardados", Toast.LENGTH_SHORT).show();
    }

    private void clearErrors() {
        tilCliente.setError(null);
        tilDireccion.setError(null);
        tilCP.setError(null);
        tilLocalidad.setError(null);
        tilTelefono.setError(null);
        tilEmail.setError(null);
        tilNotas.setError(null);
    }

    // Validaciones (España)
    private boolean isValidCP(String cp) {
        return cp != null && cp.matches("(?:0[1-9]|[1-4][0-9]|5[0-2])\\d{3}");
    }

    private boolean isValidTelefono(String t) {
        if (t == null) return false;
        Pattern p = Pattern.compile("\\b(?:\\+?34\\s*[-.]?\\s*)?(\\d{3}\\s*[-.]?\\s*\\d{2}\\s*[-.]?\\s*\\d{2}\\s*[-.]?\\s*\\d{2}|\\d{9})\\b");
        Matcher m = p.matcher(t.replaceAll("\\s+", ""));
        return m.find();
    }

    private boolean isValidEmail(String e) {
        if (e == null) return false;
        Pattern p = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);
        return p.matcher(e).find();
    }

    // ---------- HELPERS UI ----------
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
        if (obj == null) return "";
        for (String k : keys) {
            if (obj.has(k)) {
                String v = obj.optString(k, "");
                if (v != null && !v.trim().isEmpty()) return v.trim();
            }
        }
        return "";
    }

    private void setText(TextInputEditText et, String v) {
        if (et != null) et.setText(v == null ? "" : v);
    }

    private String getText(TextInputEditText et) {
        return et == null || et.getText() == null ? "" : et.getText().toString().trim();
    }

    private String getPayloadAny(String... keys) {
        return lastPayload == null ? "" : optAny(lastPayload, keys);
    }

    // --------- Save UI (canónicas + fallback) -----------
    private void updateSaveUI() {
        if (btnAddToRoute == null || tvAvisoGuardar == null) return;

        if (lastPayload == null) {
            btnAddToRoute.setEnabled(false);
            tvAvisoGuardar.setVisibility(View.GONE);
            return;
        }

        String direccion = safe(lastPayload.optString("direccion", getPayloadAny("direccion","dirección","Direccion","Dirección","address")));
        String cp        = safe(lastPayload.optString("cp",        getPayloadAny("cp","CP","codigo_postal","código_postal","codigoPostal","postal_code","postalCode","zip")));
        String loc       = safe(lastPayload.optString("localidad", getPayloadAny("localidad","Localidad","poblacion","población","ciudad","municipio","city","pueblo")));

        String falta = "";
        if (TextUtils.isEmpty(direccion)) falta = appendFalta(falta, "Dirección");
        if (TextUtils.isEmpty(cp) && TextUtils.isEmpty(loc)) falta = appendFalta(falta, "CP o Localidad");

        // Dedupe
        Stop temp = Stop.fromJson(lastPayload, lastSource);
        boolean dup = StopRepository.get().existsDuplicate(temp);

        if (dup) {
            btnAddToRoute.setEnabled(false);
            tvAvisoGuardar.setText("Este albarán ya está en la ruta.");
            tvAvisoGuardar.setVisibility(View.VISIBLE);
            return;
        }

        boolean ok = falta.isEmpty();
        btnAddToRoute.setEnabled(ok);

        if (ok) {
            tvAvisoGuardar.setVisibility(View.GONE);
        } else {
            tvAvisoGuardar.setText("Necesitas editar y completar: " + falta + " para poder guardar en la ruta.");
            tvAvisoGuardar.setVisibility(View.VISIBLE);
        }
    }

    private String appendFalta(String base, String item) {
        return (base == null || base.isEmpty()) ? item : base + ", " + item;
    }

    // --- Normalización robusta de claves ---
    private JSONObject normalizeKeys(JSONObject src) {
        if (src == null) return new JSONObject();
        JSONObject out = new JSONObject();
        try {
            java.util.Iterator<String> it = src.keys();
            while (it.hasNext()) {
                String k = it.next();
                Object v = src.opt(k);
                out.put(normalizeKey(k), v);
            }
        } catch (Exception ignored) {}
        return out;
    }

    private String normalizeKey(String k) {
        if (k == null) return "";
        String n = java.text.Normalizer.normalize(k, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "") // quitar acentos
                .trim()
                .toLowerCase(java.util.Locale.ROOT)
                .replaceAll("\\s+", " ")
                .replace("_", " ")
                .trim();

        switch (n) {
            case "numero albaran": case "numeroalbaran":
            case "id albaran": case "id_albaran":
            case "albaranid": case "id":
                return "id_albaran";
            case "direccion": case "direc": case "address":
                return "direccion";
            case "cp": case "codigo postal": case "codigo_postal":
            case "codigopostal": case "postal_code": case "postalcode":
            case "zip":
                return "cp";
            case "localidad": case "poblacion": case "población":
            case "municipio": case "ciudad": case "city": case "pueblo":
                return "localidad";
            case "cliente": case "nombre": case "nombre cliente":
                return "cliente";
            case "apellidos": case "apellidos cliente":
                return "apellidos";
            case "telefono": case "tel": case "phone": case "tlf":
                return "telefono";
            case "email": case "correo": case "mail":
                return "email";
            case "observaciones": case "notas": case "nota":
            case "comentarios": case "comments":
                return "observaciones";
            default:
                return n.replace(" ", "_");
        }
    }

    private void setSavingState(boolean saving) {
        if (btnAddToRoute == null) return;
        btnAddToRoute.setEnabled(!saving);
        btnAddToRoute.setText(saving ? "Añadiendo…" : "Añadir a la ruta");
    }


}

