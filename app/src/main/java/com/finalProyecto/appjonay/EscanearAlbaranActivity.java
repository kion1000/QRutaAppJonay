package com.finalProyecto.appjonay;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

public class EscanearAlbaranActivity extends AppCompatActivity {

    private boolean hasLaunched = false;

    public static final String EXTRA_ALBARAN_RAW = "EXTRA_ALBARAN_RAW";

    private final ActivityResultLauncher<ScanOptions> barcodeLauncher =
            registerForActivityResult(new ScanContract(), result -> {
                if (result.getContents() != null) {
                    String contenido = result.getContents();
                    lanzarDetalle(contenido);
                } else {
                    // Si cancelan, te quedas en esta pantalla por si quieren cerrar o reintentar
                    // (Puedes mostrar un Snackbar si quieres)
                }
            });

    private void lanzarDetalle(String contenido) {
        Intent i = new Intent(this, DetalleAlbaranActivity.class);
        i.putExtra(EXTRA_ALBARAN_RAW, contenido);
        startActivity(i);
        finish(); // cierras la pantalla de escaneo
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_escanear_albaran);

        // Botón Cerrar (existe en tu XML)
        View btnCerrar = findViewById(R.id.btnCerrar);
        if (btnCerrar != null) {
            btnCerrar.setOnClickListener(v -> {
                setResult(RESULT_CANCELED);
                finish();
            });
        }

        if (savedInstanceState != null) {
            hasLaunched = savedInstanceState.getBoolean("hasLaunched", false);
        }

        // Lanza el escaneo automáticamente una sola vez
        if (!hasLaunched) {
            hasLaunched = true;
            iniciarEscaneo();
        }
    }

    @Override
    protected void onSaveInstanceState(@Nullable Bundle outState) {
        super.onSaveInstanceState(outState);
        if (outState != null) outState.putBoolean("hasLaunched", hasLaunched);
    }

    private void iniciarEscaneo() {
        ScanOptions options = new ScanOptions();
        options.setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES);
        options.setPrompt("Enfoca el código del albarán");
        options.setBeepEnabled(true);
        options.setOrientationLocked(true); // ¡clave!
        options.setCaptureActivity(CaptureActivityPortrait.class); // ¡clave!
        options.setBarcodeImageEnabled(false);
        barcodeLauncher.launch(options);
    }
}
