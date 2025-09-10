package com.finalProyecto.appjonay.data;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StopRepository {
    private static StopRepository INSTANCE;
    public static StopRepository get() { return INSTANCE; }

    /** ✅ Nuevo: saber si está inicializado */
    public static boolean isReady() { return INSTANCE != null; }

    /** ✅ Nuevo: inicializador seguro (usa Application Context) */
    public static synchronized void init(Context ctx) {
        if (INSTANCE == null) {
            INSTANCE = new StopRepository(ctx.getApplicationContext());
        }
    }

    private final Context app;
    private final ArrayList<Stop> stops = new ArrayList<>();
    private static final String SP  = "route_repo";
    private static final String KEY = "stops_today";

    private StopRepository(Context app) {
        this.app = app;
        load();
    }

    public synchronized void add(Stop s) {
        stops.add(s);
        save();
    }

    /** ✅ Nuevo: añade sólo si no existe ya */
    public synchronized boolean addIfNotExists(Stop s) {
        if (existsDuplicate(s)) return false;
        add(s);
        return true;
    }

    /** ✅ Nuevo: comprueba duplicados por id_albaran o por (dirección+cp+localidad) */
    public synchronized boolean existsDuplicate(Stop s) {
        if (s == null) return false;
        String key = dedupeKey(s);
        for (Stop it : stops) {
            if (dedupeKey(it).equalsIgnoreCase(key)) return true;
        }
        return false;
    }

    private String dedupeKey(Stop s) {
        String id = safe(s.albaranId);
        if (!id.isEmpty()) return "id:" + id;
        return "addr:" + safe(s.direccion) + "|" + safe(s.cp) + "|" + safe(s.localidad);
    }

    private static String safe(String x) { return x == null ? "" : x.trim(); }

    public synchronized void removeAt(int position) {
        if (position >= 0 && position < stops.size()) {
            stops.remove(position);
            save();
        }
    }

    public synchronized void clear() {
        stops.clear();
        save();
    }

    public synchronized int size() { return stops.size(); }

    public synchronized List<Stop> getAll() {
        return Collections.unmodifiableList(stops);
    }

    private void save() {
        try {
            JSONArray arr = new JSONArray();
            for (Stop s : stops) arr.put(s.toJson());
            sp().edit().putString(KEY, arr.toString()).apply();
        } catch (Throwable ignored) {}
    }

    private void load() {
        try {
            String raw = sp().getString(KEY, "[]");
            JSONArray arr = new JSONArray(raw);
            stops.clear();
            for (int i = 0; i < arr.length(); i++) {
                stops.add(Stop.fromJson(arr.getJSONObject(i)));
            }
        } catch (Throwable ignored) {}
    }

    private SharedPreferences sp() {
        return app.getSharedPreferences(SP, Context.MODE_PRIVATE);
    }
}
