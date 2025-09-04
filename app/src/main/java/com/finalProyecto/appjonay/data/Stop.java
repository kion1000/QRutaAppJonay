package com.finalProyecto.appjonay.data;

import org.json.JSONException;
import org.json.JSONObject;
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

    public static Stop fromJson(JSONObject obj, Source src) {
        Stop s = new Stop();
        s.source = (src == null) ? Source.MANUAL : src;
        s.albaranId = obj.optString("id_albaran", obj.optString("numeroAlbaran", ""));
        s.cliente   = obj.optString("cliente", obj.optString("nombre", ""));
        s.direccion = obj.optString("direccion", "");
        s.cp        = obj.optString("cp", "");
        s.localidad = obj.optString("localidad", "");
        s.provincia = obj.optString("provincia", "");
        s.telefono  = obj.optString("telefono", "");
        s.email     = obj.optString("email", "");
        s.notas     = obj.optString("observaciones", obj.optString("notas", ""));
        return s;
    }

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
}
