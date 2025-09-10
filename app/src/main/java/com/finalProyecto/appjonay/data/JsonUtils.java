package com.finalProyecto.appjonay.data;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** Conversión sencilla JSONObject -> Map<String, Object> aceptable por Firestore */
public class JsonUtils {
    public static Map<String, Object> toMap(JSONObject o) {
        Map<String, Object> map = new HashMap<>();
        if (o == null) return map;
        Iterator<String> it = o.keys();
        while (it.hasNext()) {
            String k = it.next();
            Object v = o.opt(k);
            if (v instanceof JSONObject) {
                map.put(k, toMap((JSONObject) v));
            } else if (v instanceof JSONArray) {
                map.put(k, v.toString()); // simplificamos: se guarda como string JSON
            } else {
                map.put(k, v);
            }
        }
        return map;
    }
}
