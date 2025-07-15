package com.finalProyecto.appjonay;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONObject;

public class DetalleAlbaranActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_albaran);

        // Obtener el JSON del Intent
        String albaranJson = getIntent().getStringExtra("albaranJson");
        if (albaranJson == null) {
            finish();
            return;
        }

        try {
            JSONObject obj = new JSONObject(albaranJson);

            ((TextView) findViewById(R.id.tvNumeroAlbaran)).setText("Número: " + obj.optString("numeroAlbaran"));
            ((TextView) findViewById(R.id.tvFecha)).setText("Fecha: " + obj.optString("fecha"));
            ((TextView) findViewById(R.id.tvCliente)).setText("Cliente: " + obj.optString("nombreCliente") + " " + obj.optString("apellidosCliente"));
            ((TextView) findViewById(R.id.tvDireccion)).setText("Dirección: " + obj.optString("direccion"));
            ((TextView) findViewById(R.id.tvTelefono)).setText("Teléfono: " + obj.optString("telefono"));
            ((TextView) findViewById(R.id.tvEmail)).setText("Email: " + obj.optString("email"));
            ((TextView) findViewById(R.id.tvDNI)).setText("DNI: " + obj.optString("dni"));
            ((TextView) findViewById(R.id.tvCIF)).setText("CIF: " + obj.optString("cif"));
            ((TextView) findViewById(R.id.tvProducto)).setText("Producto: " + obj.optString("producto"));
            ((TextView) findViewById(R.id.tvCantidad)).setText("Cantidad: " + obj.optString("cantidad"));
            ((TextView) findViewById(R.id.tvObservaciones)).setText("Observaciones: " + obj.optString("observaciones"));
            ((TextView) findViewById(R.id.tvEstado)).setText("Estado: " + obj.optString("estado"));
        } catch (Exception e) {
            finish();
        }

        // Botón para cerrar
        Button btnCerrar = findViewById(R.id.btnCerrarDetalle);
        btnCerrar.setOnClickListener(v -> finish());
    }
}
