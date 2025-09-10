package com.finalProyecto.appjonay.data;

import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Sincronización simple con Firestore.
 * - saveStop: sube un Stop al día actual del usuario.
 * - loadStops: lee todos los stops del día y los devuelve.
 */
public class CloudRouteStore {

    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface SaveCallback {
        void onComplete(boolean ok, @Nullable Exception e);
    }

    public interface LoadCallback {
        void onComplete(List<Stop> stops, @Nullable Exception e);
    }

    /** AAAA-MM-DD en zona del dispositivo */
    public static String todayKey() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    @Nullable
    public static String currentUid() {
        return FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;
    }

    public static void saveStop(String uid, String dayKey, Stop stop, SaveCallback cb) {
        if (uid == null || dayKey == null || stop == null) {
            if (cb != null) cb.onComplete(false, new IllegalArgumentException("args nulos"));
            return;
        }
        // Usamos stop.id como documentId (ya es un UUID). Guardamos el JSON del Stop.
        JSONObject json = stop.toJson();
        db.collection("users").document(uid)
                .collection("rutas").document(dayKey)
                .collection("stops").document(stop.id)
                .set(JsonUtils.toMap(json), SetOptions.merge())
                .addOnSuccessListener(unused -> { if (cb != null) cb.onComplete(true, null); })
                .addOnFailureListener(e -> { if (cb != null) cb.onComplete(false, e); });
    }

    public static void loadStops(String uid, String dayKey, LoadCallback cb) {
        if (uid == null || dayKey == null) {
            if (cb != null) cb.onComplete(new ArrayList<>(), new IllegalArgumentException("args nulos"));
            return;
        }
        db.collection("users").document(uid)
                .collection("rutas").document(dayKey)
                .collection("stops")
                .get()
                .addOnSuccessListener(snap -> {
                    List<Stop> list = new ArrayList<>();
                    for (QueryDocumentSnapshot d : snap) {
                        try {
                            // El documento son claves planas -> a JSONObject
                            JSONObject o = new JSONObject(d.getData());
                            // Conserva el id de documento si no estuviera en el JSON
                            if (!o.has("id")) o.put("id", d.getId());
                            list.add(Stop.fromJson(o));
                        } catch (Exception ignored) {}
                    }
                    if (cb != null) cb.onComplete(list, null);
                })
                .addOnFailureListener(e -> {
                    if (cb != null) cb.onComplete(new ArrayList<>(), e);
                });
    }
}
