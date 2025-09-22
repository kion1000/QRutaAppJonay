package com.finalProyecto.appjonay.data;

import androidx.annotation.Nullable;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Sincronización con Firestore (rutas por día):
 *  - saveStop: guarda/actualiza stop con updatedAt en server y isDeleted=false.
 *  - deleteStop: marca tombstone (isDeleted=true) con deletedAt/updatedAt en server.
 *  - clearDay: marca tombstone a todos los docs del día (batch).
 *  - clearDayHard: elimina físicamente todos los docs del día (opcional).
 *  - loadStops: lee todos los stops del día usando Stop.fromDoc().
 *  - listenStops: escucha en tiempo real todos los cambios del día.
 */
public class CloudRouteStore {

    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String TAG = "CloudRouteStore";

    // --------- Callbacks públicos ---------
    public interface SaveCallback {
        void onComplete(boolean ok, @Nullable Exception e);
    }
    public interface LoadCallback {
        void onComplete(List<Stop> stops, @Nullable Exception e);
    }
    public interface ListenCallback {
        void onEvent(List<Stop> stops, @Nullable Exception e);
    }
    // Overload sin Exception para comodidad (éxito/fracaso separados)
    public interface ListHandler { void onList(List<Stop> stops); }
    public interface ErrorHandler { void onError(Exception e); }

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

    // --------- Helpers privados ---------
    private static CollectionReference stopsRef(String uid, String dayKey) {
        return db.collection("users").document(uid)
                .collection("rutas").document(dayKey)
                .collection("stops");
    }
    private static boolean badArgs(Object... xs) {
        for (Object x : xs) if (x == null) return true;
        return false;
    }

    // --------- API pública ---------

    /** Guarda/actualiza un Stop. Fuerza isDeleted=false y updatedAt=serverTimestamp. */
    public static void saveStop(String uid, String dayKey, Stop stop, @Nullable SaveCallback cb) {
        if (badArgs(uid, dayKey, stop)) {
            if (cb != null) cb.onComplete(false, new IllegalArgumentException("args nulos"));
            return;
        }
        // Estado coherente local antes de subir
        stop.setDeleted(false);
        stop.normalize();
        stop.touch(); // updatedAt local (informativo)

        Map<String, Object> data;
        try {
            data = stop.toMapForFirestore(); // si tu Stop lo implementa
        } catch (Throwable t) {
            data = new HashMap<>();
            data.put("id", stop.id);
            // mantén consistencia con tu esquema (usa "id_albaran" si es lo que hay en la DB)
            data.put("id_albaran", stop.albaranId);
            data.put("cliente", stop.cliente);
            data.put("direccion", stop.direccion);
            data.put("cp", stop.cp);
            data.put("localidad", stop.localidad);
            data.put("provincia", stop.provincia);
            data.put("telefono", stop.telefono);
            data.put("email", stop.email);
            data.put("notas", stop.notas);
            data.put("done", stop.done);
            data.put("isDeleted", false);
            if (stop.createdAt == 0L) data.put("createdAt", FieldValue.serverTimestamp());
            else data.put("createdAt", stop.createdAt);
            data.put("updatedAt", FieldValue.serverTimestamp());
        }

        stopsRef(uid, dayKey).document(stop.id)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> { if (cb != null) cb.onComplete(true, null); })
                .addOnFailureListener(e -> { if (cb != null) cb.onComplete(false, e); });
    }

    /** Marca un Stop como borrado (tombstone) subiendo deletedAt/updatedAt con serverTimestamp. */
    public static void deleteStop(String uid, String dayKey, Stop s, @Nullable SaveCallback cb) {
        if (uid == null || dayKey == null || s == null || s.id == null) {
            if (cb != null) cb.onComplete(false, new IllegalArgumentException("args nulos"));
            return;
        }

        Log.d(TAG, "deleteStop uid=" + uid + " dayKey=" + dayKey + " docId=" + s.id + " albaran=" + s.albaranId);

        Map<String, Object> tomb = new HashMap<>();
        tomb.put("id", s.id);
        // mantén consistencia del nombre del campo
        if (s.albaranId != null) tomb.put("id_albaran", s.albaranId);
        if (s.direccion != null) tomb.put("direccion", s.direccion);
        if (s.cp != null) tomb.put("cp", s.cp);
        if (s.localidad != null) tomb.put("localidad", s.localidad);
        tomb.put("isDeleted", true);
        tomb.put("updatedAt", FieldValue.serverTimestamp());
        tomb.put("deletedAt", FieldValue.serverTimestamp());

        stopsRef(uid, dayKey).document(s.id)
                .set(tomb, SetOptions.merge())
                .addOnSuccessListener(unused -> { if (cb != null) cb.onComplete(true, null); })
                .addOnFailureListener(e -> { if (cb != null) cb.onComplete(false, e); });
    }

    /** Overload por id (por si en algún sitio solo tienes el id). */
    public static void deleteStop(String uid, String dayKey, String stopId, @Nullable SaveCallback cb) {
        if (uid == null || dayKey == null || stopId == null) {
            if (cb != null) cb.onComplete(false, new IllegalArgumentException("args nulos"));
            return;
        }
        Log.d(TAG, "deleteStopById uid=" + uid + " dayKey=" + dayKey + " docId=" + stopId);

        Map<String, Object> tomb = new HashMap<>();
        tomb.put("id", stopId);
        tomb.put("isDeleted", true);
        tomb.put("updatedAt", FieldValue.serverTimestamp());
        tomb.put("deletedAt", FieldValue.serverTimestamp());

        stopsRef(uid, dayKey).document(stopId)
                .set(tomb, SetOptions.merge())
                .addOnSuccessListener(unused -> { if (cb != null) cb.onComplete(true, null); })
                .addOnFailureListener(e -> { if (cb != null) cb.onComplete(false, e); });
    }

    /** Marca todos los stops del día como borrados (soft-delete batch). */
    public static void clearDay(String uid, String dayKey, @Nullable SaveCallback cb) {
        if (badArgs(uid, dayKey)) {
            if (cb != null) cb.onComplete(false, new IllegalArgumentException("args nulos"));
            return;
        }
        stopsRef(uid, dayKey).get()
                .addOnSuccessListener((QuerySnapshot snap) -> {
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        Map<String, Object> tomb = new HashMap<>();
                        tomb.put("isDeleted", true);
                        tomb.put("updatedAt", FieldValue.serverTimestamp());
                        tomb.put("deletedAt", FieldValue.serverTimestamp());
                        batch.set(d.getReference(), tomb, SetOptions.merge());
                    }
                    batch.commit()
                            .addOnSuccessListener(unused -> { if (cb != null) cb.onComplete(true, null); })
                            .addOnFailureListener(e -> { if (cb != null) cb.onComplete(false, e); });
                })
                .addOnFailureListener(e -> { if (cb != null) cb.onComplete(false, e); });
    }

    /** Elimina físicamente todos los docs del día (purgado duro). */
    public static void clearDayHard(String uid, String dayKey, @Nullable SaveCallback cb) {
        if (badArgs(uid, dayKey)) {
            if (cb != null) cb.onComplete(false, new IllegalArgumentException("args nulos"));
            return;
        }
        stopsRef(uid, dayKey).get()
                .addOnSuccessListener((QuerySnapshot snap) -> {
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        batch.delete(d.getReference());
                    }
                    batch.commit()
                            .addOnSuccessListener(unused -> { if (cb != null) cb.onComplete(true, null); })
                            .addOnFailureListener(e -> { if (cb != null) cb.onComplete(false, e); });
                })
                .addOnFailureListener(e -> { if (cb != null) cb.onComplete(false, e); });
    }

    /** Carga los stops del día y los mapea con Stop.fromDoc(). */
    public static void loadStops(String uid, String dayKey, @Nullable LoadCallback cb) {
        if (badArgs(uid, dayKey)) {
            if (cb != null) cb.onComplete(new ArrayList<>(), new IllegalArgumentException("args nulos"));
            return;
        }
        stopsRef(uid, dayKey).get()
                .addOnSuccessListener(snap -> {
                    List<Stop> list = new ArrayList<>();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        try {
                            Stop s = Stop.fromDoc(d);
                            // ⚠️ SIEMPRE forzar que el id sea el documentId real
                            s.id = d.getId();
                            list.add(s);
                        } catch (Exception ignored) {}
                    }
                    if (cb != null) cb.onComplete(list, null);
                })
                .addOnFailureListener(e -> {
                    if (cb != null) cb.onComplete(new ArrayList<>(), e);
                });
    }

    // ---------------- Tiempo real ----------------

    /** Listener simple (un único callback con lista o excepción). */
    public static ListenerRegistration listenStops(String uid, String dayKey, @Nullable ListenCallback cb) {
        if (uid == null || dayKey == null) return null;
        return stopsRef(uid, dayKey)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) { if (cb != null) cb.onEvent(new ArrayList<>(), e); return; }
                    List<Stop> list = new ArrayList<>();
                    if (snap != null) {
                        for (QueryDocumentSnapshot d : snap) {
                            try {
                                Stop s = Stop.fromDoc(d);
                                // ⚠️ SIEMPRE usar documentId como id
                                s.id = d.getId();
                                list.add(s);
                            } catch (Exception ignored) {}
                        }
                    }
                    if (cb != null) cb.onEvent(list, null);
                });
    }

    /** Listener con callbacks separados para éxito y error. */
    public static ListenerRegistration listenStops(String uid,
                                                   String dayKey,
                                                   @Nullable ListHandler onList,
                                                   @Nullable ErrorHandler onError) {
        if (uid == null || dayKey == null) return null;
        return stopsRef(uid, dayKey)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) { if (onError != null) onError.onError(e); return; }
                    List<Stop> list = new ArrayList<>();
                    if (snap != null) {
                        for (QueryDocumentSnapshot d : snap) {
                            try {
                                Stop s = Stop.fromDoc(d);
                                // ⚠️ SIEMPRE usar documentId como id
                                s.id = d.getId();
                                list.add(s);
                            } catch (Exception ignored) {}
                        }
                    }
                    if (onList != null) onList.onList(list);
                });
    }
}
