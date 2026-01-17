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
            if (uri.startsWith("content://") || uri.startsWith("file://")) {
                Glide.with(this)
                        .load(Uri.parse(uri))
                        .into(img);
            } else {
                Glide.with(this)
                        .load("file:///android_asset/" + uri)
                        .into(img);
            }
        }

        // Fecha ao tocar fora da imagem
        fundo.setOnClickListener(v -> finish());
        img.setOnClickListener(v -> finish());

    }

}
