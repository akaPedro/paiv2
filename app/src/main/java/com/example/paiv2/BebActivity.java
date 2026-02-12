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
import java.util.Comparator;
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
        db = AppDatabase.getInstance(this);

        // --- CONFIGURAÇÃO DO GRID ---
        GridLayoutManager manager = new GridLayoutManager(this, 3);

        // Essa lógica é vital: define que o Divisor ocupa a largura total (3 colunas)
        // e o produto ocupa apenas 1 coluna.
        manager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                // Se o ID for -1 (nossa convenção para divisor), retorna 3 (tela toda)
                // Senão, retorna 1 (card normal)
                if (adapter != null && adapter.getItemCount() > position) {
                    // Estamos acessando o método público que precisamos ter no Adapter ou checando o item
                    // Como não tenho acesso direto ao seu "getItem" no adapter aqui fora, vamos supor lógica segura:
                    // O ideal é adicionar um método "isDivisor(int position)" no seu Adapter.
                    // Mas baseando no ID -1 que usamos antes:
                    return adapter.isDivisor(position) ? 3 : 1;
                }
                return 1;
            }
        });

        recyclerView.setLayoutManager(manager);

        adapter = new ProdutoAdapter(this, new ArrayList<>());
        recyclerView.setAdapter(adapter);

        FloatingActionButton addProds = findViewById(R.id.addProds);
        addProds.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddProdutoActivity.class);
            intent.putExtra("categoria", CATEGORIA.name());
            startActivity(intent);
        });

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

    @Override
    protected void onResume() {
        super.onResume();
        carregarProdutos();
    }

    private void carregarProdutos() {
        new Thread(() -> {
            // Pega tudo do banco
            List<Produto> produtosBrutos = db.produtoDao().listarPorCategoriaOrdenado(CATEGORIA);

            // Aplica a lógica de separação e adição de divisores
            List<Produto> listaOrganizada = organizarBebidas(produtosBrutos);

            runOnUiThread(() -> {
                adapter.atualizarLista(listaOrganizada);
            });
        }).start();
    }

    // Função que recebe a lista bagunçada e retorna a lista com Headers
    private List<Produto> organizarBebidas(List<Produto> listaOriginal) {
        List<Produto> listaFinal = new ArrayList<>();
        List<Produto> naoAlcoolicos = new ArrayList<>();
        List<Produto> alcoolicos = new ArrayList<>();

        // 1. Separa
        for (Produto p : listaOriginal) {
            if (isAlcoolica(p)) {
                alcoolicos.add(p);
            } else {
                naoAlcoolicos.add(p);
            }
        }

        // (Opcional) Ordenar alfabeticamente dentro dos grupos
        // Comparator<Produto> comparador = (p1, p2) -> p1.getNome().compareToIgnoreCase(p2.getNome());
        // Collections.sort(naoAlcoolicos, comparador);
        // Collections.sort(alcoolicos, comparador);

        // 2. Monta: Cabeçalho Não Alcoólico + Itens
        if (!naoAlcoolicos.isEmpty()) {
            Produto divNao = new Produto();
            divNao.setId(-1); // ID -1 indica Divisor
            divNao.setNome("NÃO ALCOÓLICAS"); // Texto que aparecerá no divisor
            listaFinal.add(divNao);
            listaFinal.addAll(naoAlcoolicos);
        }

        // 3. Monta: Cabeçalho Alcoólico + Itens
        if (!alcoolicos.isEmpty()) {
            Produto divAlc = new Produto();
            divAlc.setId(-1); // ID -1 indica Divisor
            divAlc.setNome("ALCOÓLICAS");
            listaFinal.add(divAlc);
            listaFinal.addAll(alcoolicos);
        }

        return listaFinal;
    }

    private boolean isAlcoolica(Produto p) {
        if (p.getNome() == null) return false;
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
                || nome.contains("gin")
                || nome.contains("caipirinha");
    }
}