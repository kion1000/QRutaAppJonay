package com.finalProyecto.appjonay.data;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class StopRepository {

    // ---------- Singleton ----------
    private static volatile StopRepository INSTANCE;

    /** Llamar una vez (p.ej., en MyApp.onCreate) */
    public static synchronized void init(Context ctx) {
        if (INSTANCE == null) {
            INSTANCE = new StopRepository(ctx.getApplicationContext());
        }
    }

    /** Devuelve la instancia ya inicializada o lanza si no lo está */
    public static StopRepository get() {
        if (INSTANCE == null) {
            throw new IllegalStateException("StopRepository no inicializado. Llama a StopRepository.init(context) primero.");
        }
        return INSTANCE;
    }

    /** Por si quieres comprobar y re-inicializar defensivamente en alguna Activity */
    public static boolean isReady() { return INSTANCE != null; }

    // ---------- Estado ----------
    private final Context app;
    private final ArrayList<Stop> stops = new ArrayList<>();

    private static final String SP  = "route_repo";
    private static final String KEY = "stops_today";

    // Constructor privado (usa init)
    private StopRepository(Context app) {
        this.app = app;
        load();
    }

    // ---------- API ----------
    public synchronized void add(Stop s) {
        stops.add(s);
        save();
    }

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

    // ---------- Persistencia ----------
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
                JSONObject o = arr.getJSONObject(i);
                // Requiere que Stop tenga la sobrecarga fromJson(JSONObject)
                stops.add(Stop.fromJson(o));
            }
        } catch (Throwable ignored) {}
    }

    private SharedPreferences sp() {
        return app.getSharedPreferences(SP, Context.MODE_PRIVATE);
    }
}
