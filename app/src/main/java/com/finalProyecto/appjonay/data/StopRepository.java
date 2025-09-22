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
 * - Respeta tombstones (isDeleted/deletedAt) al hacer merge con Firestore.
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

    /** Elimina por posición (legacy). */
    public synchronized void removeAt(int position) {
        if (position >= 0 && position < stops.size()) {
            stops.remove(position);
            save();
        }
    }

    /** NUEVO: elimina por id (usado por la UI con swipe). */
    public synchronized boolean remove(String id) {
        if (id == null) return false;
        int idx = indexOfById(id);
        if (idx >= 0) {
            stops.remove(idx);
            save();
            return true;
        }
        // Fallback: por clave de dedupe si el id no coincide (compat docs antiguos)
        String fakeKey = "uuid:" + norm(id);
        idx = indexOfByKey(fakeKey);
        if (idx >= 0) {
            stops.remove(idx);
            save();
            return true;
        }
        return false;
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

    /** Merge simple (compat): no reemplaza locales existentes. */
    public synchronized void mergeRemote(Collection<Stop> remoteStops) {
        mergeRemote(remoteStops, /*replaceLocal=*/false);
    }

    /**
     * Merge con respeto a tombstones y timestamps.
     * - Si remoto.isDeleted=true y (deletedAt|updatedAt) remoto >= updatedAt local => elimina local.
     * - Si remoto vivo:
     *    * no existe local => añade
     *    * existe y remoto.updatedAt > local.updatedAt => reemplaza por remoto
     * - replaceLocal=true limpia lista antes de fusionar.
     */
    public synchronized void mergeRemote(Collection<Stop> remoteStops, boolean replaceLocal) {
        if (remoteStops == null) return;

        if (replaceLocal) {
            stops.clear();
        }

        for (Stop r : remoteStops) {
            if (r == null) continue;

            // 1) Busca por ID (tombstones traen id)
            int idx = indexOfById(r.id);

            // 2) Si no, intenta por albarán (si viene)
            if (idx < 0 && r.albaranId != null && !r.albaranId.trim().isEmpty()) {
                String albKey = "alb:" + norm(r.albaranId);
                idx = indexOfByKey(albKey);
            }

            // 3) Último recurso: la clave habitual
            if (idx < 0) {
                String rKey = dedupeKey(r);
                idx = indexOfByKey(rKey);
            }

            if (r.isDeleted) {
                long rDel = (r.deletedAt != null) ? r.deletedAt : r.updatedAt;
                if (idx >= 0) {
                    Stop local = stops.get(idx);
                    long lUpd = Math.max(local.updatedAt, local.deletedAt != null ? local.deletedAt : -1L);
                    if (rDel >= lUpd) {
                        stops.remove(idx); // respeta borrado remoto reciente
                    }
                }
                // si no estaba local, nada que hacer
                continue;
            }

            // Remoto NO borrado
            if (idx < 0) {
                stops.add(r); // nuevo
            } else {
                Stop local = stops.get(idx);
                if (r.updatedAt > local.updatedAt) {
                    stops.set(idx, r); // remoto más nuevo => sustituir
                }
            }
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
                save();
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
        // 1) PRIMARIO: albarán si existe (estable)
        String alb = norm(s.albaranId);
        if (!alb.isEmpty()) return "alb:" + alb;

        // 2) SECUNDARIO: dirección | cp | localidad (para manuales)
        String dir = norm(s.direccion), cp = norm(s.cp), loc = norm(s.localidad);
        if (!dir.isEmpty() || !cp.isEmpty() || !loc.isEmpty()) {
            return "addr:" + dir + "|" + cp + "|" + loc;
        }

        // 3) Fallback: UUID
        String uuid = norm(s.id);
        if (!uuid.isEmpty()) return "uuid:" + uuid;

        return "fallback:" + norm(s.cliente);
    }


    private int indexOfById(String id) {
        if (id == null) return -1;
        String nid = norm(id);
        for (int i = 0; i < stops.size(); i++) {
            if (norm(stops.get(i).id).equals(nid)) return i;
        }
        return -1;
    }

    private int indexOfByKey(String key) {
        for (int i = 0; i < stops.size(); i++) {
            if (dedupeKey(stops.get(i)).equals(key)) return i;
        }
        return -1;
    }
}
