package com.finalProyecto.appjonay.data;

import com.finalProyecto.appjonay.data.Stop;

import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StopRepository {
    private static final StopRepository INSTANCE = new StopRepository();
    public static StopRepository get() { return INSTANCE; }

    private final List<Stop> todaysStops = new ArrayList<>();

    private StopRepository() {}

    public synchronized List<Stop> getAll() {
        return Collections.unmodifiableList(new ArrayList<>(todaysStops));
    }

    public synchronized void clear() {
        todaysStops.clear();
    }

    public synchronized void add(Stop s) {
        if (s == null) return;
        // De-duplicar por id_albaran si existe
        if (s.albaranId != null && !s.albaranId.isEmpty()) {
            for (int i = 0; i < todaysStops.size(); i++) {
                if (s.albaranId.equalsIgnoreCase(todaysStops.get(i).albaranId)) {
                    todaysStops.set(i, s);
                    return;
                }
            }
        }
        todaysStops.add(s);
    }

    public synchronized Stop addFromJson(JSONObject obj, Stop.Source src) {
        Stop s = Stop.fromJson(obj, src);
        add(s);
        return s;
    }

    public synchronized Stop findByAlbaran(String albaranId) {
        if (albaranId == null) return null;
        for (Stop s : todaysStops) {
            if (albaranId.equalsIgnoreCase(s.albaranId)) return s;
        }
        return null;
    }

    public synchronized void update(Stop updated) {
        if (updated == null) return;
        for (int i = 0; i < todaysStops.size(); i++) {
            if (todaysStops.get(i).id.equals(updated.id)) {
                todaysStops.set(i, updated);
                return;
            }
        }
    }

    public synchronized void removeById(String id) {
        todaysStops.removeIf(s -> s.id.equals(id));
    }
}
