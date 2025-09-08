package com.finalProyecto.appjonay.data;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;
import java.util.UUID;

public class Stop {
    public enum Source { OCR, QR, MANUAL }

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
    public long createdAt = System.currentTimeMillis();
    public boolean done = false;

    /** --- Overload NUEVO: lee también "source" del JSON si existe --- */
    public static Stop fromJson(JSONObject obj) {
        Stop s = parseCommon(obj);

        // Lee source del JSON si existe; por defecto MANUAL
        String src = safe(obj.optString("source", "MANUAL"));
        try {
            if (!src.isEmpty()) s.source = Source.valueOf(src.toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            s.source = Source.MANUAL;
        }
        return s;
    }

    /** Mantiene tu firma antigua. Sobrescribe el source si se pasa. */
    public static Stop fromJson(JSONObject obj, Source src) {
        Stop s = fromJson(obj);           // parsea común y lee source del JSON
        if (src != null) s.source = src;  // si nos lo dan explícito, manda el parámetro
        return s;
    }

    /** Serializa a JSON (incluye "source"). */
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
            o.put("done", done);
        } catch (JSONException ignored) {}
        return o;
    }

    // ------------------------ Helpers ------------------------

    /** Parseo común de campos aceptando variantes frecuentes de clave. */
    private static Stop parseCommon(JSONObject obj) {
        Stop s = new Stop();
        if (obj == null) return s;

        // si ya viene un id/createdAt/done, respétalos
        String idIn = safe(obj.optString("id", ""));
        if (!idIn.isEmpty()) s.id = idIn;
        if (obj.has("createdAt")) s.createdAt = obj.optLong("createdAt", s.createdAt);
        if (obj.has("done"))      s.done      = obj.optBoolean("done", false);

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

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }

    // --- Añadir dentro de Stop.java ---

    /** ¿Tiene dirección mínimamente válida para ruta? */
    public boolean isAddressReady() {
        return !safe(direccion).isEmpty();
    }

    /** Normaliza campos típicos (opcional, llama a esto antes de guardar si quieres). */
    public void normalize() {
        telefono = normalizeTelefono(telefono);
        cp = normalizeCp(cp);
        // Puedes añadir más normalizaciones si las necesitas
    }

    private static String normalizeTelefono(String t) {
        String digits = safe(t).replaceAll("[^0-9+]", "");
        // Si empieza por 34 sin '+', añádelo
        if (digits.matches("^34\\d{9}$")) digits = "+" + digits;
        // Si son 9 dígitos (móvil fijo ES), déjalo tal cual
        // Si ya es +34 y 9 dígitos, también vale
        return digits;
    }

    private static String normalizeCp(String c) {
        String only = safe(c).replaceAll("[^0-9]", "");
        if (only.length() >= 5) only = only.substring(0, 5);
        return only;
    }

    /** Útil si metes los stops en estructuras que dependen de igualdad. */
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Stop)) return false;
        Stop other = (Stop) o;
        return safe(id).equals(safe(other.id));
    }

    @Override public int hashCode() {
        return safe(id).hashCode();
    }

}
