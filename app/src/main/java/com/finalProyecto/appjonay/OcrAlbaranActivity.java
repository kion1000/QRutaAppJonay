package com.finalProyecto.appjonay;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import org.json.JSONObject;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OcrAlbaranActivity extends AppCompatActivity {

    // ---------- UI ----------
    private TextView tvEstado;
    private ImageView imgPreview;

    // ---------- Foto ----------
    private Uri photoUri;
    private File photoFile;

    // ---------- Activity Result ----------
    private ActivityResultLauncher<Uri> takePictureLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ocr_albaran);

        tvEstado   = findViewById(R.id.tvEstado);
        imgPreview = findViewById(R.id.imgPreview);
        Button btnFoto = findViewById(R.id.btnFoto);

        tvEstado.setText("Listo para escanear (ML Kit).");

        // Lanzadores sin lambdas (total compat.)
        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                new ActivityResultCallback<Boolean>() {
                    @Override public void onActivityResult(Boolean ok) {
                        if (ok != null && ok) {
                            tvEstado.setText("Foto hecha. Reconociendo texto…");
                            imgPreview.setImageURI(photoUri);
                            iniciarOCRConMLKit();
                        } else {
                            tvEstado.setText("Cancelado.");
                        }
                    }
                });

        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                new ActivityResultCallback<Boolean>() {
                    @Override public void onActivityResult(Boolean granted) {
                        if (granted != null && granted) {
                            hacerFoto();
                        } else {
                            tvEstado.setText("Permiso de cámara denegado. Actívalo para continuar.");
                        }
                    }
                });

        btnFoto.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                hacerFoto();
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });
    }

    // ----------------------------- FOTO -----------------------------
    private void hacerFoto() {
        try {
            File imagesDir = new File(getCacheDir(), "images");
            if (!imagesDir.exists()) imagesDir.mkdirs();
            photoFile = new File(imagesDir, "albaran.jpg");
            photoUri = FileProvider.getUriForFile(
                    this, getPackageName() + ".fileprovider", photoFile);
            takePictureLauncher.launch(photoUri);
        } catch (Exception e) {
            tvEstado.setText("Error creando archivo de foto: " + e.getMessage());
        }
    }

    // ----------------------------- OCR (ML Kit) -----------------------------
    private void iniciarOCRConMLKit() {
        try {
            InputImage image = InputImage.fromFilePath(this, photoUri);

            com.google.mlkit.vision.text.TextRecognizer recognizer =
                    TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

            recognizer.process(image)
                    .addOnSuccessListener(new OnSuccessListener<Text>() {
                        @Override public void onSuccess(Text visionText) {
                            try {
                                String raw = visionText.getText();
                                if (raw == null) raw = "";
                                JSONObject json = extraerCampos(raw);

                                Intent i = new Intent(OcrAlbaranActivity.this, DetalleAlbaranActivity.class);
                                i.putExtra(EscanearAlbaranActivity.EXTRA_ALBARAN_RAW, json.toString());

                                tvEstado.setText("Reconocimiento OK (ML Kit). Abriendo detalle…");
                                startActivity(i);
                                finish();
                            } catch (Exception e) {
                                tvEstado.setText("Error parseando texto: " + e.getMessage());
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override public void onFailure(Exception e) {
                            tvEstado.setText("OCR (ML Kit) error: " + e.getMessage());
                        }
                    });

        } catch (Exception e) {
            tvEstado.setText("No pude preparar la imagen: " + e.getMessage());
        }
    }

    // ====================== PARSEO / INTELIGENCIA ======================

    private JSONObject extraerCampos(String raw) throws Exception {
        String text = normalize(raw);
        String[] lines = text.split("\n");

        // Dirección + CP + Localidad (heurística robusta)
        AddressParts addr = parseDireccionCPyLocalidad(text, lines);

        String id        = buscarIdAlbaran(text);
        String fecha     = buscarFecha(text);
        String email     = buscarEmail(text);
        String dniNie    = buscarDniNie(text);
        String cif       = buscarCif(text);
        String tel       = buscarTelefono(text); // mejorado (6/7/8/9 y +34 opcional)
        String notas     = buscarNotas(text);
        String ventana   = buscarVentana(text);

        JSONObject obj = new JSONObject();
        obj.put("id_albaran", id);
        obj.put("fecha", fecha);
        obj.put("cliente", buscarNombre(lines));
        obj.put("direccion", addr.address);
        obj.put("cp", addr.cp);
        obj.put("localidad", addr.localidad);
        obj.put("telefono", tel);
        obj.put("email", email);
        obj.put("dni", dniNie);
        obj.put("cif", cif);
        obj.put("ventana_horaria", ventana);
        obj.put("notas", notas);
        return obj;
    }

    private String normalize(String s) {
        if (s == null) return "";
        return s.replace("\r", "")
                .replaceAll("[\u00A0\t]+", " ")
                .replaceAll(" +", " ")
                .replaceAll("\\s*\\n\\s*", "\n");
    }

    // ---------- Heurística de Dirección + CP + Localidad ----------
    private static class AddressParts {
        final String address;
        final String cp;
        final String localidad;
        AddressParts(String a, String c, String l) { address = a; cp = c; localidad = l; }
    }

    private AddressParts parseDireccionCPyLocalidad(String full, String[] lines) {
        // Vías comunes (con tildes y abreviaturas)
        final Pattern VIA = Pattern.compile("(?iu)\\b(Calle|C/|Avenida|Av\\.?|Avda\\.?|Plaza|Pza\\.?|Camino|Carretera|Ctra\\.?|Paseo|Ps\\.?|Ronda|Traves[ií]a|Trv\\.?|Urb\\.?|Urbanizaci[oó]n|Pol[ií]gono|Pol\\.?|Pgno\\.?|Parque|Barrio|Partida|Km\\.?|Edificio|Bloque|Esc\\.?|Escalera|Portal|Puerta)\\b");
        final Pattern CP  = Pattern.compile("\\b(?:0[1-9]|[1-4][0-9]|5[0-2])\\d{3}\\b");
        final Pattern LABEL_START  = Pattern.compile("(?iu)^(Tel(?:e|é)f?ono|Tlf\\.?|Telf\\.?|Email|Correo|DNI|NIE|CIF|N[ºo°]\\s*Albar[aá]n|No\\.?\\s*Albar[aá]n|Id|Fecha|Cliente|Observaciones|Estado)\\b");
        final Pattern LABEL_INLINE = Pattern.compile("(?iu)\\b(Tel(?:e|é)f?ono|Tlf\\.?|Telf\\.?|Email|Correo|DNI|NIE|CIF|N[ºo°]\\s*Albar[aá]n|No\\.?\\s*Albar[aá]n|Id|Fecha|Cliente|Observaciones|Estado)\\b.*$");
        final Pattern HOUSE_NO     = Pattern.compile("\\b\\d+[A-Za-zºª\\-/]*\\b");

        String bestAddress = "";
        String bestCP = "";
        String bestLoc = "";
        int bestScore = -9999;

        // 1) Buscamos una "semilla" de dirección (línea con VÍA y/o número)
        for (int i = 0; i < lines.length; i++) {
            String raw = lines[i].trim();
            if (raw.isEmpty()) continue;
            if (LABEL_START.matcher(raw).find()) continue;

            String cand = raw.replaceFirst("(?iu)^Direcci[oó]n\\s*[:\\-]?\\s*", "").trim();
            // corta en caso de etiquetas inline
            Matcher mi = LABEL_INLINE.matcher(cand);
            if (mi.find()) cand = cand.substring(0, mi.start()).trim();
            if (cand.isEmpty()) continue;

            boolean hasVia = VIA.matcher(cand).find();
            boolean hasNum = HOUSE_NO.matcher(cand).find();
            boolean hasCP  = CP.matcher(cand).find();

            // Si no parece calle, deja pasar pero con menos puntuación
            int score = 0;
            if (hasVia) score += 3;
            if (hasNum) score += 2;
            if (hasCP)  score += 3;
            score += Math.min(cand.length() / 10, 3); // longitud ayuda un poco

            // ¿extender con la siguiente línea? (continuación típica)
            String joined = cand;
            String cpFound = hasCP ? firstMatch(CP, cand) : "";
            String locFound = "";

            if (i + 1 < lines.length) {
                String next = lines[i + 1].trim();
                if (!next.isEmpty() && !LABEL_START.matcher(next).find()) {
                    // limpia inline
                    Matcher mi2 = LABEL_INLINE.matcher(next);
                    if (mi2.find()) next = next.substring(0, mi2.start()).trim();

                    boolean seemsContinuation =
                            cand.endsWith(",")
                                    || CP.matcher(next).find()
                                    || (!hasVia && VIA.matcher(next).find())
                                    || (next.matches("(?u)^[A-Za-zÁÉÍÓÚÜÑ][\\p{L} .\\-]{2,}$") && !next.matches(".*\\d.*"));

                    if (seemsContinuation) {
                        joined = (cand + " " + next).replaceAll("\\s+", " ").trim();
                        score += 2;
                    }
                }
            }

            // Busca CP y localidad en el "joined"
            if (cpFound.isEmpty()) cpFound = firstMatch(CP, joined);
            if (!cpFound.isEmpty()) {
                // Patrón: CP + localidad || localidad + CP
                Matcher m1 = Pattern.compile("(?iu)\\b" + cpFound + "\\b\\s*[,\\-]?\\s*([A-ZÁÉÍÓÚÜÑ][\\p{L} .\\-]{2,})$").matcher(joined);
                if (m1.find()) {
                    locFound = m1.group(1).trim();
                    score += 2;
                } else {
                    Matcher m2 = Pattern.compile("(?iu)([A-ZÁÉÍÓÚÜÑ][\\p{L} .\\-]{2,})\\s*[,\\-]?\\s*\\b" + cpFound + "\\b").matcher(joined);
                    if (m2.find()) {
                        locFound = m2.group(1).trim();
                        score += 2;
                    }
                }
            }

            // Evita colar teléfonos al final
            joined = joined.replaceFirst("\\s+\\b(?:\\+?34[\\s\\-\\.]*)?(?:[6789](?:[\\s\\-\\.]?\\d){8})\\b$", "").trim();

            if (score > bestScore) {
                bestScore = score;
                bestAddress = joined;
                bestCP = cpFound;
                bestLoc = locFound;
            }
        }

        // 2) Fallback: si no hay dirección pero sí CP, combina línea del CP + anterior
        if (bestAddress.isEmpty()) {
            for (int i = 0; i < lines.length; i++) {
                String s = lines[i].trim();
                Matcher mcp = CP.matcher(s);
                if (mcp.find()) {
                    String cp = mcp.group(0);
                    String prev = (i > 0) ? lines[i - 1].trim() : "";
                    if (!prev.isEmpty() && !LABEL_START.matcher(prev).find()) {
                        String cand = (prev + " " + s).replaceAll("\\s+", " ").trim();
                        cand = cand.replaceFirst("\\s+\\b(?:\\+?34[\\s\\-\\.]*)?(?:[6789](?:[\\s\\-\\.]?\\d){8})\\b$", "").trim();
                        String loc = "";
                        Matcher m1 = Pattern.compile("(?iu)\\b" + cp + "\\b\\s*[,\\-]?\\s*([A-ZÁÉÍÓÚÜÑ][\\p{L} .\\-]{2,})$").matcher(cand);
                        if (m1.find()) loc = m1.group(1).trim();
                        bestAddress = cand; bestCP = cp; bestLoc = loc;
                        break;
                    }
                }
            }
        }

        // 3) Último recurso: cualquier línea con VÍA
        if (bestAddress.isEmpty()) {
            for (String l : lines) {
                String s = l.trim();
                if (s.isEmpty() || LABEL_START.matcher(s).find()) continue;
                if (VIA.matcher(s).find()) {
                    bestAddress = s;
                    break;
                }
            }
        }

        // Si aún no hay CP, intenta en todo el texto
        if (bestCP.isEmpty()) bestCP = firstMatch(CP, full);

        return new AddressParts(bestAddress, bestCP, bestLoc);
    }

    private String firstMatch(Pattern p, String s) {
        Matcher m = p.matcher(s);
        return m.find() ? m.group(0) : "";
    }

    // ---------- Campos estándar ----------
    private String buscarEmail(String text) {
        Pattern p = Pattern.compile("\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(text);
        return m.find() ? m.group(0) : "";
    }

    private String buscarDniNie(String text) {
        Pattern p = Pattern.compile("\\b(?:\\d{8}[ -]?[A-Z]|[XYZ]\\d{7}[ -]?[A-Z])\\b", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(text);
        return m.find() ? m.group(0).replaceAll("\\s", "") : "";
    }

    private String buscarCif(String text) {
        Pattern p = Pattern.compile("\\b[ABCDEFGHJKLMNPQRSUVW]\\d{7}[0-9A-J]\\b", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(text);
        return m.find() ? m.group(0) : "";
    }

    private String buscarTelefono(String text) {
        // Solo prefijos españoles válidos 6/7/8/9, +34 opcional. Evita confundir con CP.
        Pattern p = Pattern.compile("\\b(?:\\+?34[\\s\\-\\.]*)?(?:[6789](?:[\\s\\-\\.]?\\d){8})\\b");
        Matcher m = p.matcher(text);
        if (!m.find()) return "";
        return m.group(0).replaceAll("[^\\d]", ""); // deja solo dígitos
    }

    private String buscarFecha(String text) {
        Matcher m = Pattern.compile("\\b(20\\d{2})[-/](0?[1-9]|1[0-2])[-/](0?[1-9]|[12]\\d|3[01])\\b").matcher(text);
        if (m.find()) return m.group(0);
        m = Pattern.compile("\\b(0?[1-9]|[12]\\d|3[01])[\\-/](0?[1-9]|1[0-2])[\\-/](20\\d{2})\\b").matcher(text);
        if (m.find()) {
            String d = String.format("%02d", Integer.parseInt(m.group(1)));
            String mo = String.format("%02d", Integer.parseInt(m.group(2)));
            String y = m.group(3);
            return y + "-" + mo + "-" + d; // normaliza a yyyy-MM-dd
        }
        return "";
    }

    private String buscarNombre(String[] lines) {
        for (String l : lines) {
            String s = l.trim();
            if (s.toLowerCase().startsWith("cliente")
                    || s.toLowerCase().startsWith("att")
                    || s.toLowerCase().startsWith("nombre")) {
                return s.replaceFirst("(?i)^(cliente|att|nombre)\\s*[:\\-]?\\s*", "").trim();
            }
        }
        for (String s : lines) {
            if (s.matches("[A-ZÁÉÍÓÚÑ&\\s\\.]{5,}") && !s.matches(".*\\d.*")) return s.trim();
        }
        return "";
    }

    private String buscarNotas(String text) {
        Pattern p = Pattern.compile("(?is)(Observaciones|Notas)\\s*[:\\-]\\s*(.+?)(?:\\n\\s*\\n|$)");
        Matcher m = p.matcher(text);
        return m.find() ? m.group(2).trim() : "";
    }

    private String buscarVentana(String text) {
        Matcher m = Pattern.compile("(\\d{1,2}:\\d{2})\\s*-\\s*(\\d{1,2}:\\d{2})").matcher(text);
        return m.find() ? (m.group(1) + "-" + m.group(2)) : "";
    }

    private String buscarIdAlbaran(String text) {
        Matcher m = Pattern.compile("(?i)(?:N[ºo]\\s*Albar[aá]n|Albar[aá]n|ALB|ID)\\s*[:#-]*\\s*([A-Z0-9\\-_/]{4,})").matcher(text);
        if (m.find()) return m.group(1).trim();
        m = Pattern.compile("\\bALB[-_]?\\d{2,}\\b", Pattern.CASE_INSENSITIVE).matcher(text);
        if (m.find()) return m.group(0);
        m = Pattern.compile("\\b[A-Z0-9][A-Z0-9\\-_/]{5,}\\b").matcher(text);
        return m.find() ? m.group(0) : "";
    }
}
