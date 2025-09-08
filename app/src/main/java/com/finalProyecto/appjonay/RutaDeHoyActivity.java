package com.finalProyecto.appjonay;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.finalProyecto.appjonay.data.Stop;
import com.finalProyecto.appjonay.data.StopRepository;

import java.util.List;

public class RutaDeHoyActivity extends AppCompatActivity {

    private StopsAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ruta_hoy);

        // Red de seguridad: si por cualquier motivo no está listo, inicializa aquí.
        if (!StopRepository.isReady()) {
            StopRepository.init(getApplicationContext());
        }

        RecyclerView rv = findViewById(R.id.rvStops);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        adapter = new StopsAdapter();
        rv.setAdapter(adapter);

        Button btnVaciar = findViewById(R.id.btnVaciar);
        if (btnVaciar != null) {
            btnVaciar.setOnClickListener(v -> {
                StopRepository.get().clear();
                refresh();
                Toast.makeText(this, "Ruta vaciada", Toast.LENGTH_SHORT).show();
            });
        }

        Button btnOptimizar = findViewById(R.id.btnOptimizar);
        if (btnOptimizar != null) {
            btnOptimizar.setOnClickListener(v ->
                    Toast.makeText(this, "Optimización: próximamente 🚧", Toast.LENGTH_SHORT).show()
            );
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        List<Stop> data = StopRepository.get().getAll();
        adapter.submit(data);
    }
}
