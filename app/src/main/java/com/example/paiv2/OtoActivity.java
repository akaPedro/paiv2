package com.example.paiv2;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.paiv2.database.AppDatabase;
import com.example.paiv2.entity.Categoria;
import com.example.paiv2.entity.Produto;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class OtoActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProdutoAdapter adapter;
    private AppDatabase db;

    private static final Categoria CATEGORIA = Categoria.OUTROS;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_other);

        recyclerView = findViewById(R.id.recyclerProdutos);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));

        db = AppDatabase.getInstance(this);

        // ✅ Adapter criado UMA VEZ
        adapter = new ProdutoAdapter(this, new ArrayList<>());
        recyclerView.setAdapter(adapter);

        FloatingActionButton addProds = findViewById(R.id.addProds);
        addProds.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddProdutoActivity.class);
            intent.putExtra("categoria", CATEGORIA.name());
            startActivity(intent);
        });

        // Voltar para Main
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent intent = new Intent(OtoActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            }
        });
    }

    // 🔁 TODA VEZ QUE VOLTAR PARA A TELA
    @Override
    protected void onResume() {
        super.onResume();
        carregarProdutos();
    }

    private void carregarProdutos() {
        // Criamos uma thread separada para não travar a UI
        new Thread(() -> {
            List<Produto> produtos = db.produtoDao().listarPorCategoriaOrdenado(CATEGORIA);

            // Para atualizar a lista, precisamos voltar para a Main Thread
            runOnUiThread(() -> {
                adapter.atualizarLista(produtos);
                Toast.makeText(OtoActivity.this, "Qtd: " + produtos.size(), Toast.LENGTH_SHORT).show();
            });
        }).start();
    }
}
