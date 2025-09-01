package com.finalProyecto.appjonay;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import org.json.JSONObject;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OcrAlbaranActivity extends AppCompatActivity {

    private TextView tvEstado;
    private ImageView imgPreview;

    private Uri photoUri;
    private File photoFile;

    private ActivityResultLauncher<Uri> takePictureLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ocr_albaran);

        tvEstado   = findViewById(R.id.tvEstado);
        imgPreview = findViewById(R.id.imgPreview);
        Button btnFoto = findViewById(R.id.btnFoto);

        tvEstado.setText("Listo para escanear (ML Kit).");

        // Registrar lanzadores (sin lambdas)
        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                new ActivityResultCallback<Boolean>() {
                    @Override
                    public void onActivityResult(Boolean ok) {
                        if (ok != null && ok.booleanValue()) {
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
                    @Override
                    public void onActivityResult(Boolean granted) {
                        if (granted != null && granted.booleanValue()) {
                            hacerFoto();
                        } else {
                            tvEstado.setText("Permiso de cámara denegado. Actívalo para continuar.");
                        }
                    }
                });

        btnFoto.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(
                    OcrAlbaranActivity.this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                hacerFoto();
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });
    }

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

    private void iniciarOCRConMLKit() {
        try {
            InputImage image = InputImage.fromFilePath(this, photoUri);

            com.google.mlkit.vision.text.TextRecognizer recognizer =
                    TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

            recognizer.process(image)
                    .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<Text>() {
                        @Override
                        public void onSuccess(Text visionText) {
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
                    .addOnFailureListener(e -> tvEstado.setText("OCR (ML Kit) error: " + e.getMessage()));

        } catch (Exception e) {
            tvEstado.setText("No pude preparar la imagen: " + e.getMessage());
        }
    }

    // ====================== PARSEO MEJORADO ======================

    private JSONObject extraerCampos(String raw) throws Exception {
        String text = normalize(raw);
        String[] lines = text.split("\n");

        String id        = buscarIdAlbaran(text);
        String fecha     = buscarFecha(text);
        String email     = buscarEmail(text);
        String dniNie    = buscarDniNie(text);
        String cif       = buscarCif(text);
        String tel       = buscarTelefono(text);
        String cp        = buscarCP(text);
        String direccion = buscarDireccion(lines);
        String cliente   = buscarNombre(lines);
        String notas     = buscarNotas(text);

        JSONObject obj = new JSONObject();
        obj.put("id_albaran", id);
        obj.put("fecha", fecha);
        obj.put("cliente", cliente);
        obj.put("direccion", direccion);
        obj.put("cp", cp);
        obj.put("telefono", tel);
        obj.put("email", email);
        obj.put("dni", dniNie);
        obj.put("cif", cif);
        obj.put("ventana_horaria", buscarVentana(text));
        obj.put("notas", notas);
        return obj;
    }

    private String normalize(String s) {
        if (s == null) return "";
        // Normaliza saltos de línea y espacios; corrige OCR típico (O/0, l/1) muy suave
        String t = s.replace("\r", "")
                .replaceAll("[\u00A0\t]+", " ")         // nbsp/tab -> espacio
                .replaceAll(" +", " ")                  // espacios repetidos
                .replaceAll("\\s*\\n\\s*", "\n");       // limpia bordes de línea
        return t;
    }

    private String buscarEmail(String text) {
        Pattern p = Pattern.compile("\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b",
                Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(text);
        return m.find() ? m.group(0) : "";
    }

    private String buscarDniNie(String text) {
        // DNI: 8 dígitos + letra. NIE: X/Y/Z + 7 dígitos + letra
        Pattern p = Pattern.compile("\\b(?:\\d{8}[ -]?[A-Z]|[XYZ]\\d{7}[ -]?[A-Z])\\b",
                Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(text);
        return m.find() ? m.group(0).replaceAll("\\s", "") : "";
    }

    private String buscarCif(String text) {
        // CIF: letra inicial y 8 caracteres (7 dígitos + control)
        Pattern p = Pattern.compile("\\b[ABCDEFGHJKLMNPQRSUVW]\\d{7}[0-9A-J]\\b",
                Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(text);
        return m.find() ? m.group(0) : "";
    }

    private String buscarTelefono(String text) {
        // +34 opcional, separadores opcionales
        Pattern p = Pattern.compile("\\b(?:\\+?34\\s*[-.]?\\s*)?(\\d{3}\\s*[-.]?\\s*\\d{2}\\s*[-.]?\\s*\\d{2}\\s*[-.]?\\s*\\d{2}|\\d{9})\\b");
        Matcher m = p.matcher(text);
        if (!m.find()) return "";
        return m.group(0).replaceAll("[\\s.-]", "");
    }

    private String buscarCP(String text) {
        // Códigos postales España 01000–52999
        Pattern p = Pattern.compile("\\b(?:0[1-9]|[1-4][0-9]|5[0-2])\\d{3}\\b");
        Matcher m = p.matcher(text);
        return m.find() ? m.group(0) : "";
    }

    private String buscarFecha(String text) {
        // yyyy-mm-dd | dd/mm/yyyy | dd-mm-yyyy
        Matcher m = Pattern.compile("\\b(20\\d{2})[-/](0?[1-9]|1[0-2])[-/](0?[1-9]|[12]\\d|3[01])\\b").matcher(text);
        if (m.find()) return m.group(0);
        m = Pattern.compile("\\b(0?[1-9]|[12]\\d|3[01])[\\-/](0?[1-9]|1[0-2])[\\-/](20\\d{2})\\b").matcher(text);
        if (m.find()) {
            // Normaliza a yyyy-MM-dd
            String d = String.format("%02d", Integer.parseInt(m.group(1)));
            String mo = String.format("%02d", Integer.parseInt(m.group(2)));
            String y = m.group(3);
            return y + "-" + mo + "-" + d;
        }
        return "";
    }

    private String buscarDireccion(String[] lines) {
        // Vías comunes
        String via = "(?i)(Calle|C/|Avenida|Av\\.?|Avda\\.?|Plaza|Pza\\.?|Camino|Carretera|Ctra\\.?|Paseo|Ps\\.?|Ronda|Traves[ií]a|Trv\\.?|Urb\\.?|Urbanizaci[oó]n|Pol[ií]gono|Pgno\\.?|Barrio|Parque|Partida|Ptge\\.?)";
        Pattern pLinea = Pattern.compile(via + ".*\\d+.*");     // vía + algún número (ej. “Calle X 12”)

        // Etiquetas que no deben colarse en la dirección (en línea o inicio de línea)
        Pattern pEtiquetaStart  = Pattern.compile("(?i)^(Tel(?:e|é)f?ono|Tlf\\.?|Telf\\.?|Email|Correo|DNI|NIE|CIF|N[ºo°]\\s*Albar[aá]n|No\\.?\\s*Albar[aá]n|N°|Id|Fecha|Cliente|Observaciones|Estado)\\b");
        Pattern pEtiquetaInline = Pattern.compile("(?i)\\b(Tel(?:e|é)f?ono|Tlf\\.?|Telf\\.?|Email|Correo|DNI|NIE|CIF|N[ºo°]\\s*Albar[aá]n|No\\.?\\s*Albar[aá]n|N°|Id|Fecha|Cliente|Observaciones|Estado)\\b.*$");

        // Código postal español 5 dígitos (10xxx..52xxx)
        Pattern pCP = Pattern.compile("\\b(?:0[1-9]|[1-4][0-9]|5[0-2])\\d{3}\\b");

        String mejor = "";

        for (int i = 0; i < lines.length; i++) {
            String raw = lines[i].trim();
            if (raw.isEmpty()) continue;

            // Quita “Dirección: ” si viene delante
            String cand = raw.replaceFirst("(?i)^Direcci[oó]n\\s*[:\\-]?\\s*", "").trim();

            // ¿Parece una línea de dirección?
            if (!(pLinea.matcher(raw).find() || pLinea.matcher(cand).find())) continue;

            // ⚠️ CORTA si dentro de la MISMA línea aparece otra etiqueta (Teléfono, Email, etc.)
            Matcher mInline = pEtiquetaInline.matcher(cand);
            if (mInline.find()) cand = cand.substring(0, mInline.start()).trim();

            // Si acaba en coma o no termina en punto: quizá la siguiente línea es continuación
            if (i + 1 < lines.length) {
                String next = lines[i + 1].trim();
                if (!next.isEmpty() && !pEtiquetaStart.matcher(next).find()) {
                    // Limpia etiquetas también en la línea siguiente
                    Matcher m2 = pEtiquetaInline.matcher(next);
                    if (m2.find()) next = next.substring(0, m2.start()).trim();

                    boolean continuacion = cand.endsWith(",") || pCP.matcher(next).find();
                    if (continuacion) {
                        cand = (cand + " " + next).replaceAll("\\s+", " ").trim();
                    }
                }
            }

            // Si por OCR quedó un móvil suelto al final, elimínalo.
            cand = cand.replaceFirst("\\s+\\b\\d{9}\\b$", "").trim();

            // Mantén la más larga/consistente
            if (cand.length() > mejor.length()) mejor = cand;
        }

        // Fallback: cualquier línea que tenga “Dirección” o CP
        if (mejor.isEmpty()) {
            for (String l : lines) {
                String s = l.replaceFirst("(?i)^Direcci[oó]n\\s*[:\\-]?\\s*", "").trim();
                if (l.toLowerCase().contains("direcci") || pCP.matcher(l).find()) {
                    Matcher mInline = pEtiquetaInline.matcher(s);
                    if (mInline.find()) s = s.substring(0, mInline.start()).trim();
                    s = s.replaceFirst("\\s+\\b\\d{9}\\b$", "").trim();
                    if (s.length() > mejor.length()) mejor = s;
                }
            }
        }

        return mejor;
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
        // fallback: línea en MAYÚSCULAS sin números (probable nombre)
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