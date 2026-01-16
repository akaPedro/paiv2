package com.example.paiv2;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.paiv2.database.AppDatabase;
import com.example.paiv2.database.PopuladorBanco;
import com.example.paiv2.entity.Categoria;

public class MainActivity extends AppCompatActivity {

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        AppDatabase db = AppDatabase.getInstance(this);

        Button BAlim = findViewById(R.id.btnAlimentos);
        Button BBebi = findViewById(R.id.btnBebidas);
        Button BHili = findViewById(R.id.btnHigiene);
        Button BOut = findViewById(R.id.btnOutros);



        BAlim.setOnClickListener(view -> {
            Intent IntAlim = new Intent(MainActivity.this, AlimActivity.class);
            startActivity(IntAlim);
            finish();
        });

        BBebi.setOnClickListener(v -> {
            Intent IntBeb = new Intent(MainActivity.this, BebActivity.class);
            startActivity(IntBeb);
            finish();
        });

        BHili.setOnClickListener(v -> {
            Intent IntHili = new Intent(MainActivity.this, HighActivity.class);
            startActivity(IntHili);
        });

        BOut.setOnClickListener(v -> {
            Intent IntOut = new Intent(MainActivity.this, OtoActivity.class);
            startActivity(IntOut);
        });

        // Dentro do onCreate, adicione um botão de teste ou use um existente

//        btnImportar.setOnClickListener(v -> {
//            Toast.makeText(this, "Importando produtos em massa...", Toast.LENGTH_LONG).show();
//
            PopuladorBanco.importarCategoria(
                    this,
                    db,
                    "alim",
                    Categoria.ALIMENTOS
            );

            PopuladorBanco.importarCategoria(
                    this,
                    db,
                    "bebes",
                    Categoria.BEBIDAS
            );

            PopuladorBanco.importarCategoria(
                    this,
                    db,
                    "high",
                    Categoria.HIGIENE
            );

            PopuladorBanco.importarCategoria(
                    this,
                    db,
                    "oto",
                    Categoria.OUTROS
            );



        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}