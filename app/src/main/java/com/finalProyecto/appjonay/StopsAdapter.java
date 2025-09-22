package com.finalProyecto.appjonay;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.finalProyecto.appjonay.data.Stop;

import java.util.ArrayList;
import java.util.List;

public class StopsAdapter extends RecyclerView.Adapter<StopsAdapter.VH> {

    /** Callback para acciones del usuario en cada item (p.ej. Eliminar). */
    public interface OnStopActionListener {
        void onDelete(@NonNull Stop stop, int position);
    }

    private final ArrayList<Stop> items = new ArrayList<>();
    private final OnStopActionListener listener;

    public StopsAdapter(@NonNull OnStopActionListener listener) {
        this.listener = listener;
        setHasStableIds(true);
    }

    /** Sustituye toda la lista que muestra el adapter. */
    public void submit(@NonNull List<Stop> data) {
        items.clear();
        items.addAll(data);
        notifyDataSetChanged();
    }

    /** Acceso seguro al item para swipe, etc. */
    public Stop getItem(int position) {
        if (position < 0 || position >= items.size()) return null;
        return items.get(position);
    }

    @Override public long getItemId(int position) {
        Stop s = getItem(position);
        return (s != null && s.id != null) ? s.id.hashCode() : RecyclerView.NO_ID;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_stop, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Stop s = items.get(pos);

        h.tv1.setText(emptyToDash(s.cliente) + " — " + emptyToDash(s.direccion));
        h.tv2.setText(emptyToDash(s.cp) + " • " + emptyToDash(s.localidad));
        h.tvSource.setText(s.source != null ? String.valueOf(s.source) : "MANUAL");

        h.btnEliminar.setOnClickListener(v -> {
            int adapterPos = h.getAdapterPosition();                 // ✅ compat
            if (adapterPos == RecyclerView.NO_POSITION) return;      // seguridad
            Stop current = getItem(adapterPos);
            if (current != null && listener != null) {
                listener.onDelete(current, adapterPos);
            }
        });
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tv1, tv2, tvSource;
        final Button btnEliminar;
        VH(@NonNull View v) {
            super(v);
            tv1 = v.findViewById(R.id.tvLinea1);
            tv2 = v.findViewById(R.id.tvLinea2);
            tvSource = v.findViewById(R.id.tvSource);
            btnEliminar = v.findViewById(R.id.btnEliminar);
        }
    }

    private String emptyToDash(String s) {
        return (s == null || s.trim().isEmpty()) ? "—" : s.trim();
    }
}
