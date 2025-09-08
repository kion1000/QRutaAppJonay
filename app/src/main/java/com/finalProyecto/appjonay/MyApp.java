
package com.finalProyecto.appjonay;

import android.app.Application;
import com.finalProyecto.appjonay.data.StopRepository;

public class MyApp extends Application {
    @Override public void onCreate() {
        super.onCreate();
        // Inicializa el singleton una vez, al arrancar la app
        StopRepository.init(this);
    }
}
