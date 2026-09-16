package com.example.paiv2;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.paiv2.database.AppDatabase;
import com.example.paiv2.entity.Categoria;
import com.example.paiv2.entity.Produto;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Tela única para todas as categorias (Alimentos, Bebidas, Higiene, Doces, Outros).
 * A categoria exibida chega pela Intent. Bebidas ganham os divisores
 * "NÃO ALCOÓLICAS" / "ALCOÓLICAS" automaticamente.
 */
public class CategoriaActivity extends AppCompatActivity {

    public static final String EXTRA_CATEGORIA = "categoria";
    private static final int COLUNAS = 3;

    private static final String PREFS = "paiv2_prefs";
    // Chave nova: quem já tinha visto a dica antiga precisa ver a da seleção
    private static final String KEY_DICA_VISTA = "dica_selecao_vista";

    private ProdutoAdapter adapter;
    private AppDatabase db;
    private Categoria categoria;

    public static Intent novaIntent(Context context, Categoria categoria) {
        Intent intent = new Intent(context, CategoriaActivity.class);
        intent.putExtra(EXTRA_CATEGORIA, categoria.name());
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_categoria);

        String categoriaStr = getIntent().getStringExtra(EXTRA_CATEGORIA);
        if (categoriaStr == null) {
            finish();
            return;
        }
        categoria = Categoria.valueOf(categoriaStr);

        RecyclerView recyclerView = findViewById(R.id.recyclerProdutos);
        GridLayoutManager manager = new GridLayoutManager(this, COLUNAS);

        // Divisor ocupa a largura toda; produto ocupa 1 coluna
        manager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return adapter.isDivisor(position) ? COLUNAS : 1;
            }
        });
        recyclerView.setLayoutManager(manager);

        FloatingActionButton addProds = findViewById(R.id.addProds);

        adapter = new ProdutoAdapter(this, new ArrayList<>());
        // Editar, excluir ou mudar a categoria muda a lista: recarrega do banco
        adapter.setAoAlterarProdutos(this::carregarProdutos);
        AcoesEmLote.instalar(this, adapter, addProds);
        recyclerView.setAdapter(adapter);

        db = AppDatabase.getInstance(this);

        mostrarDicaToqueLongo();

        addProds.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddProdutoActivity.class);
            intent.putExtra(EXTRA_CATEGORIA, categoria.name());
            startActivity(intent);
        });
    }

    /**
     * A seleção começa segurando um card, o que não é óbvio.
     * Explica uma vez só, na primeira visita a uma categoria.
     */
    private void mostrarDicaToqueLongo() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        if (prefs.getBoolean(KEY_DICA_VISTA, false)) return;

        Toast.makeText(this,
                "Segure em um produto para selecionar. Toque nos outros para marcar vários.",
                Toast.LENGTH_LONG).show();
        prefs.edit().putBoolean(KEY_DICA_VISTA, true).apply();
    }

    // TODA VEZ QUE VOLTAR PARA A TELA
    @Override
    protected void onResume() {
        super.onResume();
        carregarProdutos();
    }

    private void carregarProdutos() {
        // Criamos uma thread separada para não travar a UI
        new Thread(() -> {
            List<Produto> produtos = db.produtoDao().listarPorCategoriaOrdenado(categoria);

            List<Produto> listaFinal = (categoria == Categoria.BEBIDAS)
                    ? organizarBebidas(produtos)
                    : produtos;

            runOnUiThread(() -> adapter.atualizarLista(listaFinal));
        }).start();
    }

    // Separa as bebidas em grupos com cabeçalhos (divisores com id -1)
    private List<Produto> organizarBebidas(List<Produto> listaOriginal) {
        List<Produto> listaFinal = new ArrayList<>();
        List<Produto> naoAlcoolicos = new ArrayList<>();
        List<Produto> alcoolicos = new ArrayList<>();

        for (Produto p : listaOriginal) {
            if (ProdutoUtils.isAlcoolica(p)) {
                alcoolicos.add(p);
            } else {
                naoAlcoolicos.add(p);
            }
        }

        if (!naoAlcoolicos.isEmpty()) {
            listaFinal.add(ProdutoUtils.criarDivisor("NÃO ALCOÓLICAS"));
            listaFinal.addAll(naoAlcoolicos);
        }

        if (!alcoolicos.isEmpty()) {
            listaFinal.add(ProdutoUtils.criarDivisor("ALCOÓLICAS"));
            listaFinal.addAll(alcoolicos);
        }

        return listaFinal;
    }
}
