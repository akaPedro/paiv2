package com.example.paiv2;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import java.io.File;

public class ImagemProdutoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_imagem_produto);

        ImageView img = findViewById(R.id.imgProdutoGrande);
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

            // Carrega a imagem
            Glide.with(this)
                    .load(model)
                    .fitCenter() // Garante que a imagem inteira apareça na tela
                    .into(img);
        }

        // Fecha ao tocar fora da imagem ou na própria imagem
        fundo.setOnClickListener(v -> finish());
        img.setOnClickListener(v -> finish());
    }
}