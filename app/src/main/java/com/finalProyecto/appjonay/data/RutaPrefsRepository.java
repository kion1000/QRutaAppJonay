package com.finalProyecto.appjonay.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.finalProyecto.appjonay.model.Albaran;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Guarda la "Ruta de hoy" en SharedPreferences como un array JSON. */
class RutaPrefsRepository implements RutaRepository {

    private static final String PREFS = "rutas_prefs";
    private final SharedPreferences sp;

    RutaPrefsRepository(Context ctx) {
        sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private String keyHoy() {
        String d = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
        return "ruta_" + d;
    }

    @Override public List<Albaran> getHoy() {
        String raw = sp.getString(keyHoy(), "[]");
        List<Albaran> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i=0;i<arr.length();i++) {
                JSONObject o = arr.getJSONObject(i);
                out.add(Albaran.fromJson(o));
            }
        } catch (JSONException ignored) {}
        return out;
    }

    @Override public void add(Albaran a) {
        List<Albaran> list = getHoy();
        if (a.id_albaran == null || a.id_albaran.isEmpty()) {
            a.id_albaran = "ALB-" + System.currentTimeMillis();
        }
        long now = System.currentTimeMillis();
        if (a.createdAt == 0) a.createdAt = now;
        a.updatedAt = now;
        if (a.orden < 0) a.orden = list.size();
        list.add(a);
        save(list);
    }

    @Override public void update(Albaran a) {
        List<Albaran> list = getHoy();
        for (int i=0;i<list.size();i++) {
            if (list.get(i).id_albaran.equals(a.id_albaran)) {
                a.updatedAt = System.currentTimeMillis();
                list.set(i, a);
                save(list);
                return;
            }
        }
        // si no existía, lo añadimos
        add(a);
    }

    @Override public void remove(String idAlbaran) {
        List<Albaran> list = getHoy();
        for (int i=0;i<list.size();i++) {
            if (list.get(i).id_albaran.equals(idAlbaran)) {
                list.remove(i);
                break;
            }
        }
        // recompactar orden
        for (int i=0;i<list.size();i++) list.get(i).orden = i;
        save(list);
    }

    @Override public void clearHoy() {
        sp.edit().putString(keyHoy(), "[]").apply();
    }

    private void save(List<Albaran> list) {
        JSONArray arr = new JSONArray();
        for (Albaran a : list) arr.put(a.toJson());
        sp.edit().putString(keyHoy(), arr.toString()).apply();
    }
}
