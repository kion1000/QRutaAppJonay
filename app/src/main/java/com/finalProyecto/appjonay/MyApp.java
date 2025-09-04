package com.finalProyecto.appjonay;

import android.app.Application;
import com.finalProyecto.appjonay.data.Repos;  // ← si aún no creaste Repos, créalo o comenta esta línea temporalmente

public class MyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // false = usamos almacenamiento LOCAL (SharedPreferences) por ahora.
        // Cuando migremos a Firestore, cambiaremos a true y Repos usará la
        // implementación de Firestore sin tocar Activities.
        Repos.init(this, /*usarFirestore=*/false);
    }
}
