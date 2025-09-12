package com.finalProyecto.appjonay.data;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Repositorio local (SharedPreferences) de paradas/Stops para la ruta del día.
 * - Dedupe por id (UUID) o por (direccion|cp|localidad) normalizados.
 * - API thread-safe con métodos sincronizados.
 */
public class StopRepository {

    // ---------------- Singleton ----------------
    private static StopRepository INSTANCE;
    public static StopRepository get() { return INSTANCE; }
    public static boolean isReady() { return INSTANCE != null; }
    public static synchronized void init(Context ctx) {
        if (INSTANCE == null) {
            INSTANCE = new StopRepository(ctx.getApplicationContext());
        }
    }

    // ---------------- Estado ----------------
    private final Context app;
    private final ArrayList<Stop> stops = new ArrayList<>();
    private static final String SP_NAME  = "route_repo";
    private static final String KEY_STOPS = "stops_today";

    private StopRepository(Context appCtx) {
        this.app = appCtx;
        load();
    }

    // ---------------- Altas / Bajas / Consulta ----------------
    public synchronized void add(Stop s) {
        if (s == null) return;
        stops.add(s);
        save();
    }
    public synchronized boolean addUnique(Stop s) { return addIfNotExists(s); }
    public synchronized boolean addIfNotExists(Stop s) {
        if (s == null) return false;
        if (existsDuplicate(s)) return false;
        add(s);
        return true;
    }

    /** ¿Existe ya un stop equivalente? */
    public synchronized boolean existsDuplicate(Stop s) {
        if (s == null) return false;
        String key = dedupeKey(s);
        for (Stop it : stops) {
            if (dedupeKey(it).equals(key)) return true;
        }
        return false;
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

    // ---------------- Merge remoto (Firestore -> Local) ----------------
    public synchronized void mergeRemote(Collection<Stop> remoteStops) {
        mergeRemote(remoteStops, /*replaceLocal=*/false);
    }

    public synchronized void mergeRemote(Collection<Stop> remoteStops, boolean replaceLocal) {
        if (remoteStops == null) return;

        if (replaceLocal) {
            stops.clear();
        }

        for (Stop s : remoteStops) {
            if (s == null) continue;
            if (existsDuplicate(s)) continue; // evita dobles
            stops.add(s);
        }
        save();
    }

    @SuppressWarnings("unused")
    private synchronized void removeByIdentity(Stop incoming) {
        if (incoming == null) return;
        String k = dedupeKey(incoming);
        Iterator<Stop> it = stops.iterator();
        while (it.hasNext()) {
            Stop cur = it.next();
            if (dedupeKey(cur).equals(k)) {
                it.remove();
                return;
            }
        }
    }

    // ---------------- Persistencia ----------------
    private void save() {
        try {
            JSONArray arr = new JSONArray();
            for (Stop s : stops) arr.put(s.toJson());
            sp().edit().putString(KEY_STOPS, arr.toString()).apply();
        } catch (Throwable ignored) {}
    }

    private void load() {
        try {
            String raw = sp().getString(KEY_STOPS, "[]");
            JSONArray arr = new JSONArray(raw);
            stops.clear();
            for (int i = 0; i < arr.length(); i++) {
                stops.add(Stop.fromJson(arr.getJSONObject(i)));
            }
        } catch (Throwable ignored) {}
    }

    private SharedPreferences sp() {
        return app.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
    }

    // ---------------- Dedupe helpers ----------------
    private static String norm(String s) {
        if (s == null) return "";
        String n = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .trim()
                .toLowerCase(java.util.Locale.ROOT);
        return n.replaceAll("\\s+", " ");
    }

    private String dedupeKey(Stop s) {
        // 1) PRIMARIO: UUID único del stop (debe ser el mismo local/remoto)
        String uuid = norm(s.id);
        if (!uuid.isEmpty()) return "uuid:" + uuid;

        // 2) SECUNDARIO: id de albarán (si existiera)
        String alb = norm(s.albaranId);
        if (!alb.isEmpty()) return "alb:" + alb;

        // 3) Fallback: dirección | cp | localidad (normalizados)
        return "addr:" + norm(s.direccion) + "|" + norm(s.cp) + "|" + norm(s.localidad);
    }
}
