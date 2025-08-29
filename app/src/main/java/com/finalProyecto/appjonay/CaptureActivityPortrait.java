package com.finalProyecto.appjonay;

import com.journeyapps.barcodescanner.CaptureActivity;

/**
 * Activity "vacía" usada por ZXing para forzar el escaneo en orientación vertical.
 * La orientación se fija en el AndroidManifest con android:screenOrientation="portrait".
 */
public class CaptureActivityPortrait extends CaptureActivity {
    // No es necesario añadir código aquí.
}
