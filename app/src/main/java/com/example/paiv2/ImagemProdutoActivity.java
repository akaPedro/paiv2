package com.example.paiv2;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

public class ImagemProdutoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_imagem_produto);

        ImageView img = findViewById(R.id.imgProdutoGrande);
        View fundo = findViewById(R.id.fundo);

        String uri = getIntent().getStringExtra("imageUri");

        if (uri != null) {

            // 👉 Se veio do assets
            if (!uri.startsWith("content://") && !uri.startsWith("file://")) {
                uri = "file:///android_asset/" + uri;
            }

            Glide.with(this)
                    .load(uri)
                    .into(img);
        }

        // Fecha ao tocar fora da imagem
        fundo.setOnClickListener(v -> finish());
        img.setOnClickListener(v -> finish());

    }

}
