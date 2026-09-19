package com.example.paiv2;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.paiv2.database.AppDatabase;
import com.example.paiv2.entity.Categoria;
import com.example.paiv2.entity.Produto;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Tela única para todas as categorias (Alimentos, Bebidas, Higiene, Doces, Outros).
 * A categoria exibida chega pela Intent. Bebidas ganham as seções
 * "Não alcoólicas" / "Alcoólicas" automaticamente.
 */
public class CategoriaActivity extends AppCompatActivity {

    public static final String EXTRA_CATEGORIA = "categoria";
    private static final int COLUNAS = 3;

    private ProdutoAdapter adapter;
    private AppDatabase db;
    private Categoria categoria;

    private MaterialToolbar toolbar;
    private Chip chipNomear;
    private Chip chipSelecionar;
    private RecyclerView recyclerView;
    private View vazio;

    public static Intent novaIntent(Context context, Categoria categoria) {
        Intent intent = new Intent(context, CategoriaActivity.class);
        intent.putExtra(EXTRA_CATEGORIA, categoria.name());
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Janela.telaCheia(this, true, false);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_categoria);

        String categoriaStr = getIntent().getStringExtra(EXTRA_CATEGORIA);
        if (categoriaStr == null) {
            finish();
            return;
        }
        categoria = Categoria.valueOf(categoriaStr);
        int cor = EstiloCategoria.cor(this, categoria);
        db = AppDatabase.getInstance(this);

        View cabecalho = findViewById(R.id.cabecalho);
        cabecalho.setBackgroundColor(cor);
        Janela.recuos(cabecalho, Janela.TOPO);

        toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(SeletorCategoria.rotulo(categoria));
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        chipNomear = findViewById(R.id.chipNomear);
        chipNomear.setOnClickListener(v -> NomearProdutosActivity.abrir(this, categoria));
        chipSelecionar = findViewById(R.id.chipSelecionar);
        chipSelecionar.setOnClickListener(v -> adapter.entrarModoSelecao());

        configurarGrade();
        configurarVazio(cor);

        ExtendedFloatingActionButton btnAdicionar = findViewById(R.id.addProds);
        btnAdicionar.setBackgroundTintList(ColorStateList.valueOf(cor));
        Janela.recuos(btnAdicionar, Janela.MARGEM_BASE);
        btnAdicionar.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddProdutoActivity.class);
            intent.putExtra(EXTRA_CATEGORIA, categoria.name());
            startActivity(intent);
        });

        // Encolhe o botão ao rolar para baixo, para não cobrir as fotos
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (dy > 12 && btnAdicionar.isExtended()) btnAdicionar.shrink();
                else if (dy < -12 && !btnAdicionar.isExtended()) btnAdicionar.extend();
            }
        });

        AcoesEmLote acoes = AcoesEmLote.instalar(this, adapter, cabecalho, btnAdicionar);
        acoes.setAncoraAvisos(btnAdicionar);
    }

    private void configurarGrade() {
        recyclerView = findViewById(R.id.recyclerProdutos);
        Janela.recuos(recyclerView, Janela.BASE);

        GridLayoutManager manager = new GridLayoutManager(this, COLUNAS);
        // Cabeçalho de seção ocupa a linha inteira; produto ocupa 1 coluna
        manager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return adapter.isDivisor(position) ? COLUNAS : 1;
            }
        });
        recyclerView.setLayoutManager(manager);

        adapter = new ProdutoAdapter(this, new ArrayList<>());
        // Editar, excluir ou mover muda a lista: recarrega do banco
        adapter.setAoAlterarProdutos(this::carregarProdutos);
        recyclerView.setAdapter(adapter);
    }

    private void configurarVazio(int cor) {
        vazio = findViewById(R.id.vazioCategoria);
        findViewById(R.id.vazioCirculo).setBackgroundTintList(
                ColorStateList.valueOf(EstiloCategoria.corClara(this, categoria)));
        ImageView icone = findViewById(R.id.vazioIcone);
        icone.setImageResource(EstiloCategoria.icone(categoria));
        icone.setImageTintList(ColorStateList.valueOf(cor));
        ((TextView) findViewById(R.id.vazioTitulo))
                .setText("Nenhum produto em " + SeletorCategoria.rotulo(categoria));
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

            int semNome = 0;
            for (Produto p : produtos) {
                if (ProdutoUtils.semNome(p)) semNome++;
            }

            List<Produto> listaFinal = (categoria == Categoria.BEBIDAS)
                    ? organizarBebidas(produtos)
                    : produtos;

            final int faltamNomear = semNome;
            runOnUiThread(() -> {
                adapter.atualizarLista(listaFinal);
                toolbar.setSubtitle(ProdutoUtils.contagem(produtos.size(), "produto", "produtos"));

                chipNomear.setVisibility(faltamNomear > 0 ? View.VISIBLE : View.GONE);
                chipNomear.setText("Nomear · " + ProdutoUtils.numero(faltamNomear));
                chipSelecionar.setVisibility(produtos.isEmpty() ? View.GONE : View.VISIBLE);

                vazio.setVisibility(produtos.isEmpty() ? View.VISIBLE : View.GONE);
                recyclerView.setVisibility(produtos.isEmpty() ? View.GONE : View.VISIBLE);
            });
        }).start();
    }

    // Separa as bebidas em seções com cabeçalho
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
            listaFinal.add(ProdutoUtils.criarDivisor(ProdutoUtils.TITULO_NAO_ALCOOLICAS, Categoria.BEBIDAS));
            listaFinal.addAll(naoAlcoolicos);
        }

        if (!alcoolicos.isEmpty()) {
            listaFinal.add(ProdutoUtils.criarDivisor(ProdutoUtils.TITULO_ALCOOLICAS, Categoria.BEBIDAS));
            listaFinal.addAll(alcoolicos);
        }

        return listaFinal;
    }
}
