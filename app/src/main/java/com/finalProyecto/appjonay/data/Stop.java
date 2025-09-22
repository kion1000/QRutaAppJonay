package com.finalProyecto.appjonay.data;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;

/**
 * Modelo de parada (Stop) con soporte de:
 * - Dedupe por id estable.
 * - Borrado lógico (tombstone): isDeleted + deletedAt.
 * - Timestamps: createdAt (local), updatedAt (local y server en Firestore).
 * - Serialización local (JSON) y mapeo a Firestore.
 */
public class Stop {
    public enum Source { OCR, QR, MANUAL }

    // --------- Campos principales ----------
    public String id = UUID.randomUUID().toString();
    public String albaranId = "";
    public String cliente = "";
    public String direccion = "";
    public String cp = "";
    public String localidad = "";
    public String provincia = "";
    public String telefono = "";
    public String email = "";
    public String notas = "";
    public Source source = Source.MANUAL;

    // --------- Estado/tiempos ----------
    public long createdAt = System.currentTimeMillis();
    /** Timestamp local (ms) de última modificación. Actualiza con touch(). */
    public long updatedAt = createdAt;
    /** Borrado lógico (tombstone). */
    public boolean isDeleted = false;
    /** Momento de borrado (ms) o null si no está borrado. */
    public Long deletedAt = null;

    // --------- UX ----------
    public boolean done = false;

    // ======================= JSON (local) =======================

    /** Overload: lee también "source" del JSON si existe. */
    public static Stop fromJson(JSONObject obj) {
        Stop s = parseCommon(obj);
        // Lee source del JSON si existe; por defecto MANUAL
        String src = safe(obj.optString("source", "MANUAL"));
        try {
            if (!src.isEmpty()) s.source = Source.valueOf(src.toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            s.source = Source.MANUAL;
        }
        // Campos nuevos (tolerante a ausencia)
        if (obj != null) {
            if (obj.has("updatedAt")) s.updatedAt = obj.optLong("updatedAt", s.updatedAt);
            if (obj.has("isDeleted")) s.isDeleted = obj.optBoolean("isDeleted", false);
            if (obj.has("deletedAt")) {
                long del = obj.optLong("deletedAt", -1L);
                s.deletedAt = (del >= 0) ? del : null;
            }
        }
        return s;
    }

    /** Mantiene tu firma antigua. Sobrescribe el source si se pasa. */
    public static Stop fromJson(JSONObject obj, Source src) {
        Stop s = fromJson(obj);
        if (src != null) s.source = src; // si nos lo dan explícito, manda el parámetro
        return s;
    }

    /** Serializa a JSON (incluye nuevos campos). */
    public JSONObject toJson() {
        JSONObject o = new JSONObject();
        try {
            o.put("id", id);
            o.put("id_albaran", albaranId);
            o.put("cliente", cliente);
            o.put("direccion", direccion);
            o.put("cp", cp);
            o.put("localidad", localidad);
            o.put("provincia", provincia);
            o.put("telefono", telefono);
            o.put("email", email);
            o.put("notas", notas);
            o.put("source", source.name());
            o.put("createdAt", createdAt);
            o.put("updatedAt", updatedAt);
            o.put("isDeleted", isDeleted);
            if (deletedAt != null) o.put("deletedAt", deletedAt);
            o.put("done", done);
        } catch (JSONException ignored) {}
        return o;
    }

    // ======================= Firestore (nube) =======================

    /** Mapea a Firestore: updatedAt/deletedAt se ponen con serverTimestamp(). */
    public Map<String, Object> toMapForFirestore() {
        Map<String, Object> m = new HashMap<>();
        m.put("id", id);
        m.put("albaranId", albaranId);
        m.put("cliente", cliente);
        m.put("direccion", direccion);
        m.put("cp", cp);
        m.put("localidad", localidad);
        m.put("provincia", provincia);
        m.put("telefono", telefono);
        m.put("email", email);
        m.put("notas", notas);
        m.put("source", source.name());

        m.put("isDeleted", isDeleted);
        // En Firestore, updatedAt lo pone el servidor SIEMPRE que guardamos:
        m.put("updatedAt", FieldValue.serverTimestamp());
        if (isDeleted) {
            m.put("deletedAt", FieldValue.serverTimestamp());
        }
        // Opcional: persistir también createdAt local (solo informativo)
        m.put("createdAtLocal", createdAt);
        return m;
    }

    /** Crea Stop desde un DocumentSnapshot de Firestore. */
    public static Stop fromDoc(DocumentSnapshot d) {
        Stop s = new Stop();
        if (d == null) return s;

        s.id = safe(d.getString("id"));
        s.albaranId = safe(firstNonNull(d.getString("albaranId"), d.getString("id_albaran")));
        s.cliente = safe(d.getString("cliente"));
        s.direccion = safe(d.getString("direccion"));
        s.cp = safe(d.getString("cp"));
        s.localidad = safe(d.getString("localidad"));
        s.provincia = safe(d.getString("provincia"));
        s.telefono = safe(d.getString("telefono"));
        s.email = safe(d.getString("email"));
        s.notas = safe(d.getString("notas"));

        String src = safe(d.getString("source"));
        try { if (!src.isEmpty()) s.source = Source.valueOf(src.toUpperCase(Locale.ROOT)); }
        catch (Exception ignored) { s.source = Source.MANUAL; }

        Boolean del = d.getBoolean("isDeleted");
        s.isDeleted = del != null && del;

        Timestamp upTs = d.getTimestamp("updatedAt");
        s.updatedAt = (upTs != null) ? upTs.toDate().getTime() : s.updatedAt;

        Timestamp delTs = d.getTimestamp("deletedAt");
        s.deletedAt = (delTs != null) ? delTs.toDate().getTime() : null;

        // Informativo si lo subiste alguna vez:
        Long createdLocal = d.getLong("createdAtLocal");
        if (createdLocal != null) s.createdAt = createdLocal;

        return s;
    }

    // ======================= Helpers =======================

    /** Parseo común de campos aceptando variantes frecuentes de clave. */
    private static Stop parseCommon(JSONObject obj) {
        Stop s = new Stop();
        if (obj == null) return s;

        // si ya viene un id/createdAt/done/updatedAt/isDeleted/deletedAt, respétalos
        String idIn = safe(obj.optString("id", ""));
        if (!idIn.isEmpty()) s.id = idIn;
        if (obj.has("createdAt")) s.createdAt = obj.optLong("createdAt", s.createdAt);
        if (obj.has("updatedAt")) s.updatedAt = obj.optLong("updatedAt", s.updatedAt);
        if (obj.has("done"))      s.done      = obj.optBoolean("done", false);
        if (obj.has("isDeleted")) s.isDeleted = obj.optBoolean("isDeleted", false);
        if (obj.has("deletedAt")) {
            long del = obj.optLong("deletedAt", -1L);
            s.deletedAt = (del >= 0) ? del : null;
        }

        s.albaranId = optAny(obj, "id_albaran", "numeroAlbaran", "albaranId", "id");
        s.cliente   = optAny(obj, "cliente", "nombre");
        s.direccion = optAny(obj, "direccion", "dirección");
        s.cp        = optAny(obj, "cp", "codigo_postal", "código_postal");
        s.localidad = optAny(obj, "localidad", "poblacion", "población", "ciudad");
        s.provincia = optAny(obj, "provincia");
        s.telefono  = optAny(obj, "telefono", "teléfono", "phone");
        s.email     = optAny(obj, "email", "correo");
        s.notas     = optAny(obj, "observaciones", "notas");

        return s;
    }

    private static String optAny(JSONObject o, String... keys) {
        for (String k : keys) {
            if (o.has(k)) {
                String v = safe(o.optString(k, ""));
                if (!v.isEmpty()) return v;
            }
        }
        return "";
    }

    private static String safe(String s) { return s == null ? "" : s.trim(); }
    private static String firstNonNull(String a, String b) { return (a != null) ? a : b; }

    /** ¿Tiene dirección mínimamente válida para ruta? */
    public boolean isAddressReady() { return !safe(direccion).isEmpty(); }

    /** Normaliza campos típicos (opcional, llama a esto antes de guardar si quieres). */
    public void normalize() {
        telefono = normalizeTelefono(telefono);
        cp = normalizeCp(cp);
    }

    private static String normalizeTelefono(String t) {
        String digits = safe(t).replaceAll("[^0-9+]", "");
        if (digits.matches("^34\\d{9}$")) digits = "+" + digits; // 34XXXXXXXXX -> +34XXXXXXXXX
        return digits;
    }

    private static String normalizeCp(String c) {
        String only = safe(c).replaceAll("[^0-9]", "");
        if (only.length() >= 5) only = only.substring(0, 5);
        return only;
    }

    /** Marca modificado ahora (actualiza updatedAt). Llama a esto antes de guardar local. */
    public void touch() { this.updatedAt = System.currentTimeMillis(); }

    /** Marca como borrado lógico y actualiza tiempos. */
    public void setDeleted(boolean value) {
        this.isDeleted = value;
        if (value) {
            this.deletedAt = System.currentTimeMillis();
        } else {
            this.deletedAt = null;
        }
        this.touch();
    }

    // Útil si metes los stops en estructuras que dependen de igualdad.
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Stop)) return false;
        Stop other = (Stop) o;
        return safe(id).equals(safe(other.id));
    }

    @Override public int hashCode() { return safe(id).hashCode(); }
}
