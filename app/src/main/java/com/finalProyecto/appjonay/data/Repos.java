package com.finalProyecto.appjonay.data;

import android.content.Context;

public final class Repos {
    private static RutaRepository rutaRepo;

    private Repos(){}

    public static void init(Context ctx, boolean usarFirestore) {
        // Por ahora siempre local. Cuando hagamos Firestore, elegiremos aquí.
        rutaRepo = new RutaPrefsRepository(ctx.getApplicationContext());
    }

    public static RutaRepository ruta() {
        if (rutaRepo == null) throw new IllegalStateException("Repos.init() no llamado");
        return rutaRepo;
    }
}
