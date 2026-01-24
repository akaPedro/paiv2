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
import java.util.Collections;
import java.util.List;

public class BebActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProdutoAdapter adapter;
    private AppDatabase db;

    private static final Categoria CATEGORIA = Categoria.BEBIDAS;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bebs);

        recyclerView = findViewById(R.id.recyclerProdutos);
        GridLayoutManager manager = new GridLayoutManager(this, 3);
        manager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                // divisor ocupa a linha toda
                return adapter.isDivisor(position) ? 3 : 1;
            }
        });

        recyclerView.setLayoutManager(manager);

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
                Intent intent = new Intent(BebActivity.this, MainActivity.class);
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

            ordenarBebidas(produtos);

            // Para atualizar a lista, precisamos voltar para a Main Thread
            runOnUiThread(() -> {
                adapter.atualizarLista(produtos);
                Toast.makeText(BebActivity.this, "Qtd: " + produtos.size(), Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    private boolean isAlcoolica(Produto p) {
        String nome = p.getNome().toLowerCase();

        return nome.contains("cerveja")
                || nome.contains("vinho")
                || nome.contains("vodka")
                || nome.contains("whisky")
                || nome.contains("cachaça")
                || nome.contains("rum")
                || nome.contains("caninha")
                || nome.contains("montila")
                || nome.contains("ice")
                || nome.contains("catuaba")
                || nome.contains("licor")
                || nome.contains("conhaque")
                || nome.contains("sidra")
                || nome.contains("aguardente")
                || nome.contains("raiz amarga")
                || nome.contains("corote")
                || nome.contains("gin");
    }

    private void ordenarBebidas(List<Produto> lista) {

        List<Produto> novaLista = new ArrayList<>();

        // 🔵 Divisor NÃO alcoólicas
        Produto divNao = new Produto();
        divNao.setId(-1);
        divNao.setNome("NÃO ALCOÓLICAS");
        novaLista.add(divNao);

        for (Produto p : lista) {
            if (!isAlcoolica(p)) {
                novaLista.add(p);
            }
        }

        // 🔴 Divisor alcoólicas
        Produto divAlc = new Produto();
        divAlc.setId(-1);
        divAlc.setNome("ALCOÓLICAS");
        novaLista.add(divAlc);

        for (Produto p : lista) {
            if (isAlcoolica(p)) {
                novaLista.add(p);
            }
        }

        // substitui a lista original
        lista.clear();
        lista.addAll(novaLista);
    }


}
