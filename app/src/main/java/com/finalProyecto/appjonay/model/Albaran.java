package com.finalProyecto.appjonay.model;

import org.json.JSONException;
import org.json.JSONObject;

public class Albaran {
    public String id_albaran = "";
    public String fecha = "";        // ISO yyyy-MM-dd si la tienes
    public String cliente = "";
    public String direccion = "";
    public String cp = "";
    public String localidad = "";
    public String telefono = "";
    public String email = "";
    public String dni = "";
    public String cif = "";
    public String observaciones = "";
    public String origen = "";       // "OCR" | "QR" | "MANUAL"
    public long createdAt = 0L;
    public long updatedAt = 0L;
    public int  orden = -1;          // posición en la lista de hoy

    public JSONObject toJson() {
        JSONObject o = new JSONObject();
        try {
            o.put("id_albaran", id_albaran);
            o.put("fecha", fecha);
            o.put("cliente", cliente);
            o.put("direccion", direccion);
            o.put("cp", cp);
            o.put("localidad", localidad);
            o.put("telefono", telefono);
            o.put("email", email);
            o.put("dni", dni);
            o.put("cif", cif);
            o.put("observaciones", observaciones);
            o.put("origen", origen);
            o.put("createdAt", createdAt);
            o.put("updatedAt", updatedAt);
            o.put("orden", orden);
        } catch (JSONException ignored) {}
        return o;
    }

    public static Albaran fromJson(JSONObject o) {
        Albaran a = new Albaran();
        a.id_albaran   = o.optString("id_albaran", "");
        a.fecha        = o.optString("fecha", "");
        a.cliente      = o.optString("cliente", o.optString("nombreCliente",""));
        a.direccion    = o.optString("direccion", o.optString("dirección",""));
        a.cp           = o.optString("cp", "");
        a.localidad    = o.optString("localidad", "");
        a.telefono     = o.optString("telefono", o.optString("teléfono",""));
        a.email        = o.optString("email", "");
        a.dni          = o.optString("dni", "");
        a.cif          = o.optString("cif", "");
        a.observaciones= o.optString("observaciones", o.optString("notas",""));
        a.origen       = o.optString("origen", "");
        a.createdAt    = o.optLong("createdAt", 0L);
        a.updatedAt    = o.optLong("updatedAt", 0L);
        a.orden        = o.optInt ("orden", -1);
        return a;
    }
}
