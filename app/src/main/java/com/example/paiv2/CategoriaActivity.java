package com.example.paiv2;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

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

        adapter = new ProdutoAdapter(this, new ArrayList<>());
        recyclerView.setAdapter(adapter);

        db = AppDatabase.getInstance(this);

        FloatingActionButton addProds = findViewById(R.id.addProds);
        addProds.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddProdutoActivity.class);
            intent.putExtra(EXTRA_CATEGORIA, categoria.name());
            startActivity(intent);
        });
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
            listaFinal.add(criarDivisor("NÃO ALCOÓLICAS"));
            listaFinal.addAll(naoAlcoolicos);
        }

        if (!alcoolicos.isEmpty()) {
            listaFinal.add(criarDivisor("ALCOÓLICAS"));
            listaFinal.addAll(alcoolicos);
        }

        return listaFinal;
    }

    private Produto criarDivisor(String titulo) {
        Produto divisor = new Produto();
        divisor.setId(ProdutoUtils.ID_DIVISOR);
        divisor.setNome(titulo);
        return divisor;
    }
}
