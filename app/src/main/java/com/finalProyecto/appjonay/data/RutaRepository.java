package com.finalProyecto.appjonay.data;

import com.finalProyecto.appjonay.model.Albaran;
import java.util.List;

public interface RutaRepository {
    List<Albaran> getHoy();
    void add(Albaran a);
    void update(Albaran a);              // por id_albaran
    void remove(String idAlbaran);
    void clearHoy();
}
