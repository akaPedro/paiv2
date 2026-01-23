package com.example.paiv2;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.paiv2.database.AppDatabase;
import com.example.paiv2.database.PopuladorBanco;
import com.example.paiv2.entity.Categoria;
import com.example.paiv2.entity.Produto;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ProdutoAdapter adapter;
    private AppDatabase db;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        EditText edtPesquisar = findViewById(R.id.edtPesquisar);
        LinearLayout layoutCategorias = findViewById(R.id.layoutCategorias);
        RecyclerView recycler = findViewById(R.id.recyclerPesquisa);

        adapter = new ProdutoAdapter(this, new ArrayList<>());
        db = AppDatabase.getInstance(this);

        recycler.setLayoutManager(new GridLayoutManager(this, 2));
        recycler.setAdapter(adapter);

        Button BAlim = findViewById(R.id.btnAlimentos);
        Button BBebi = findViewById(R.id.btnBebidas);
        Button BHili = findViewById(R.id.btnHigiene);
        Button BOut = findViewById(R.id.btnOutros);
        Button BDoc = findViewById(R.id.btnDoces);


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

        BDoc.setOnClickListener(v -> {
            Intent IntDoc = new Intent(MainActivity.this, DoceActivity.class);
            startActivity(IntDoc);
        });

        edtPesquisar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String texto = s.toString().trim();

                if (texto.isEmpty()) {
                    recycler.setVisibility(View.GONE);
                    layoutCategorias.setVisibility(View.VISIBLE);
                } else {
                    layoutCategorias.setVisibility(View.GONE);
                    recycler.setVisibility(View.VISIBLE);
                    buscarProdutos(texto);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });


//        \\\\\\\\\\\\\\\\\\  POPULADORES  \\\\\\\\\\\\\\\\\\\\\\\\\
        
//            PopuladorBanco.importarCategoria(
//                    this,
//                    db,
//                    "alim",
//                    Categoria.ALIMENTOS
//            );
//
//            PopuladorBanco.importarCategoria(
//                    this,
//                    db,
//                    "bebes",
//                    Categoria.BEBIDAS
//            );
//
//                PopuladorBanco.importarCategoria(
//                        this,
//                        db,
//                        "doces",
//                        Categoria.DOCES
//                );
//            PopuladorBanco.importarCategoria(
//                    this,
//                    db,
//                    "high",
//                    Categoria.HIGIENE
//            );
//
//            PopuladorBanco.importarCategoria(
//                    this,
//                    db,
//                    "oto",
//                    Categoria.OUTROS
//            );



        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void buscarProdutos(String texto) {
        new Thread(() -> {
            List<Produto> lista = db.produtoDao().buscarPorNome(texto);

            runOnUiThread(() -> adapter.atualizarLista(lista));
        }).start();
    }


}