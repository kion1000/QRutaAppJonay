package com.finalProyecto.appjonay;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.annotation.NonNull;


import com.finalProyecto.appjonay.data.CloudRouteStore;
import com.finalProyecto.appjonay.data.Stop;
import com.finalProyecto.appjonay.data.StopRepository;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

public class RutaDeHoyActivity extends AppCompatActivity {

    private StopsAdapter adapter;
    private com.google.firebase.firestore.ListenerRegistration routeListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ruta_hoy);

        if (!StopRepository.isReady()) {
            StopRepository.init(getApplicationContext());
        }

        RecyclerView rv = findViewById(R.id.rvStops);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        // Adapter con callback: centralizamos el borrado en handleDeleteWithUndo(...)
        adapter = new StopsAdapter((stop, position) -> handleDeleteWithUndo(stop, rv));
        rv.setAdapter(adapter);

        // Swipe izquierda/derecha => mismo borrado con Undo
        ItemTouchHelper ith = new ItemTouchHelper(
                new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
                    @Override public boolean onMove(RecyclerView r, RecyclerView.ViewHolder v1, RecyclerView.ViewHolder v2) { return false; }

                    @Override public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int direction) {
                        int pos = vh.getAdapterPosition(); // compat con versiones antiguas
                        Stop s = (pos >= 0 && pos < adapter.getItemCount()) ? adapter.getItem(pos) : null;
                        if (s == null) { refresh(); return; }
                        handleDeleteWithUndo(s, rv);
                    }
                });
        ith.attachToRecyclerView(rv);


        // Vaciar ruta (soft-delete en lote + limpiar local)
        Button btnVaciar = findViewById(R.id.btnVaciar);
        if (btnVaciar != null) {
            btnVaciar.setOnClickListener(v -> {
                new MaterialAlertDialogBuilder(this)
                        .setTitle("Vaciar ruta de hoy")
                        .setMessage("Se marcarán como eliminadas todas las paradas de hoy.")
                        .setNegativeButton("Cancelar", null)
                        .setPositiveButton("Vaciar", (d, w) -> {
                            StopRepository.get().clear();   // local
                            refresh();
                            String uid = CloudRouteStore.currentUid();
                            if (uid != null) {
                                CloudRouteStore.clearDay(uid, CloudRouteStore.todayKey(), null); // nube (tombstones)
                            }
                            Toast.makeText(this, "Ruta vaciada", Toast.LENGTH_SHORT).show();
                        })
                        .show();
            });
        }

        Button btnOptimizar = findViewById(R.id.btnOptimizar);
        if (btnOptimizar != null) {
            btnOptimizar.setOnClickListener(v ->
                    Toast.makeText(this, "Optimización: próximamente 🚧", Toast.LENGTH_SHORT).show()
            );
        }
    }

    @Override protected void onStart() {
        super.onStart();
        String uid = CloudRouteStore.currentUid();
        String dayKey = CloudRouteStore.todayKey();
        if (uid != null) {
            routeListener = CloudRouteStore.listenStops(uid, dayKey, (stops, e) -> {
                if (e == null && stops != null) {
                    StopRepository.get().mergeRemote(stops, /*replaceLocal=*/false);
                    refresh();
                }
            });
        } else {
            Toast.makeText(this, "Inicia sesión para cargar tu ruta ☁️", Toast.LENGTH_LONG).show();
        }
    }

    @Override protected void onResume() {
        super.onResume();
        refresh(); // no recreamos el listener aquí
    }

    @Override protected void onStop() {
        super.onStop();
        if (routeListener != null) { routeListener.remove(); routeListener = null; }
    }

    private void refresh() {
        List<Stop> data = StopRepository.get().getAll();
        adapter.submit(data);
    }

    /** Borrado con Undo: quita local, sube tombstone y permite deshacer. */
    private void handleDeleteWithUndo(Stop s, RecyclerView anchor) {
        // 1) Local inmediato
        StopRepository.get().remove(s.id);
        refresh();

        // 2) Nube
        String uid = CloudRouteStore.currentUid();
        String dayKey = CloudRouteStore.todayKey();
        if (uid == null) {
            Toast.makeText(this, "No hay sesión activa: no puedo borrar en la nube ✋", Toast.LENGTH_LONG).show();
            android.util.Log.w("RutaDeHoy", "DELETE cancelado: uid=null, stopId=" + s.id);
        } else {
            android.util.Log.d("RutaDeHoy", "DELETE -> uid=" + uid + " dayKey=" + dayKey + " stopId=" + s.id);
            CloudRouteStore.deleteStop(uid, dayKey, s, (ok, e) -> {
                if (!ok) {
                    Toast.makeText(this, "Error borrando en la nube: " + (e != null ? e.getMessage() : ""), Toast.LENGTH_LONG).show();
                    android.util.Log.e("RutaDeHoy", "deleteStop fallo", e);
                }
            });
        }

        // 3) Snackbar con deshacer
        Snackbar.make(anchor, "Eliminado", Snackbar.LENGTH_LONG)
                .setAction("DESHACER", v -> {
                    s.setDeleted(false);
                    s.touch();
                    StopRepository.get().addUnique(s);
                    refresh();
                    if (uid != null) {
                        CloudRouteStore.saveStop(uid, dayKey, s, null);
                    }
                })
                .show();
    }
}
