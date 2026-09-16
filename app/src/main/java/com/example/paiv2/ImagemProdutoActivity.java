package com.example.paiv2;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy;

import java.io.File;

public class ImagemProdutoActivity extends AppCompatActivity {

    // Teto do lado maior da imagem carregada, em pixels
    private static final int LADO_MAXIMO = 2048;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_imagem_produto);

        ImagemZoomView img = findViewById(R.id.imgProdutoGrande);
        View fundo = findViewById(R.id.fundo);

        String uri = getIntent().getStringExtra("imageUri");

        if (uri != null && !uri.isEmpty()) {
            Object model;

            // 1. Verifica se é um arquivo interno (Cópia segura que criamos)
            if (uri.startsWith("/")) {
                model = new File(uri);
            }
            // 2. Verifica se é uma URI de Galeria (Legado ou Content Provider)
            else if (uri.startsWith("content://") || uri.startsWith("file://")) {
                model = Uri.parse(uri);
            }
            // 3. Se não for nenhum dos dois, assume que é um Asset
            else {
                model = "file:///android_asset/" + uri;
            }

            // Maior que a tela para o zoom ter detalhe, mas com teto: em tamanho
            // original, uma foto de 2340x2500 ocuparia 22 MB de memória sozinha.
            // AT_MOST garante que imagens menores não sejam esticadas à toa.
            Glide.with(this)
                    .load(model)
                    .override(LADO_MAXIMO, LADO_MAXIMO)
                    .downsample(DownsampleStrategy.AT_MOST)
                    .into(img);
        }

        // Fecha ao tocar fora da imagem, ou nela quando não estiver ampliada
        fundo.setOnClickListener(v -> finish());
        img.setAoTocarSemZoom(this::finish);
    }
}