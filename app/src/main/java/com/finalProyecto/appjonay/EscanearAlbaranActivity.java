package com.finalProyecto.appjonay;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import org.json.JSONObject;

public class EscanearAlbaranActivity extends AppCompatActivity {

    // ZXing launcher (ActivityResult API)
    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(
            new ScanContract(),
            result -> {
                if(result.getContents() != null) {
                    String qrData = result.getContents();
                    try {
                        JSONObject obj = new JSONObject(qrData);
                        // Aquí puedes lanzar tu activity de detalle
                        Intent intent = new Intent(this, DetalleAlbaranActivity.class);
                        intent.putExtra("albaranJson", obj.toString());
                        startActivity(intent);
                        finish();
                    } catch (Exception e) {
                        Toast.makeText(this, "QR no contiene un JSON válido:\n" + qrData, Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(this, "Escaneo cancelado", Toast.LENGTH_SHORT).show();
                    finish(); // Opcional: cerrar si cancela
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_escanear_albaran);

        // Lanza el escaneo nada más abrir la activity
        ScanOptions options = new ScanOptions();
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        options.setPrompt("Escanea el QR del albarán");
        options.setBeepEnabled(true);
        options.setBarcodeImageEnabled(false);

        barcodeLauncher.launch(options);

        // Botón cerrar (por si tienes uno en el layout)
        Button btnCerrar = findViewById(R.id.btnCerrar);
        btnCerrar.setOnClickListener(v -> finish());
    }
}
