package com.example.paiv2;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.request.FutureTarget;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Lê o texto da foto da embalagem no próprio celular, sem internet, e devolve
 * sugestões de nome.
 */
public class LeitorDeRotulo {

    public interface AoLer {
        void lidas(List<String> sugestoes);
    }

    private static final String TAG = "LeitorDeRotulo";
    // Resolução suficiente para ler o nome sem pesar na memória
    private static final int LADO = 1024;

    private final Context context;
    private final TextRecognizer reconhecedor = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
    private final ExecutorService fila = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    public LeitorDeRotulo(Context context) {
        this.context = context.getApplicationContext();
    }

    public void ler(String imageUri, AoLer aoLer) {
        fila.execute(() -> {
            List<String> sugestoes = Collections.emptyList();
            FutureTarget<Bitmap> alvo = null;
            try {
                alvo = Glide.with(context)
                        .asBitmap()
                        .load(ImagemUtils.modelo(imageUri))
                        // O ML Kit não consegue ler bitmaps guardados na GPU
                        .disallowHardwareConfig()
                        .format(DecodeFormat.PREFER_ARGB_8888)
                        .submit(LADO, LADO);
                Bitmap bitmap = alvo.get();

                Text texto = Tasks.await(reconhecedor.process(InputImage.fromBitmap(bitmap, 0)));
                List<SugestoesDeNome.Linha> linhas = new ArrayList<>();
                for (Text.TextBlock bloco : texto.getTextBlocks()) {
                    for (Text.Line linha : bloco.getLines()) {
                        Rect caixa = linha.getBoundingBox();
                        linhas.add(new SugestoesDeNome.Linha(linha.getText(), caixa == null ? 0 : caixa.height()));
                    }
                }
                sugestoes = SugestoesDeNome.escolher(linhas);
            } catch (Exception e) {
                Log.w(TAG, "Não foi possível ler a foto " + imageUri, e);
            } finally {
                if (alvo != null) Glide.with(context).clear(alvo);
            }

            List<String> resultado = sugestoes;
            main.post(() -> aoLer.lidas(resultado));
        });
    }

    public void fechar() {
        fila.shutdownNow();
        reconhecedor.close();
    }
}
