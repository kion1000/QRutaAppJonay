
package com.finalProyecto.appjonay;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.finalProyecto.appjonay.data.Stop;
import com.finalProyecto.appjonay.data.StopRepository;

import java.util.ArrayList;
import java.util.List;

public class StopsAdapter extends RecyclerView.Adapter<StopsAdapter.VH> {

    private final ArrayList<Stop> items = new ArrayList<>();

    public void submit(List<Stop> data) {
        items.clear();
        items.addAll(data);
        notifyDataSetChanged();
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
        String cliente = emptyToDash(s.cliente);
        String dir = emptyToDash(s.direccion);
        h.tv1.setText(cliente + " — " + dir);

        String cp = emptyToDash(s.cp);
        String loc = emptyToDash(s.localidad);
        h.tv2.setText(cp + " • " + loc);

        h.tvSource.setText(s.source != null ? s.source.name() : "MANUAL");

        h.btnEliminar.setOnClickListener(v -> {
            StopRepository.get().removeAt(h.getAdapterPosition());
            submit(StopRepository.get().getAll());
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

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
