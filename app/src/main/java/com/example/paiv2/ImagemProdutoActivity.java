package com.example.paiv2;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy;
import com.example.paiv2.entity.Categoria;
import com.example.paiv2.entity.Produto;

import java.util.Collections;
import java.util.List;

/**
 * Produto em tela cheia. Desliza para os lados entre os produtos da lista de onde
 * veio, mostrando nome e categoria de cada um.
 */
public class ImagemProdutoActivity extends AppCompatActivity {

    private static final String EXTRA_IDS = "ids";
    private static final String EXTRA_NOMES = "nomes";
    private static final String EXTRA_URIS = "uris";
    private static final String EXTRA_CATEGORIAS = "categorias";
    private static final String EXTRA_POSICAO = "posicao";
    private static final String EXTRA_INICIO = "inicio";
    private static final String EXTRA_TOTAL = "total";
    private static final String ESTADO_ATUAL = "atual";

    // Só os vizinhos do produto tocado vão para a tela: a busca pode ter milhares de
    // resultados e uma Intent grande demais derruba o app
    private static final int VIZINHOS = 200;
    // Maior que a tela, com teto: em tamanho original, uma foto de 2340x2500 ocuparia 22 MB
    private static final int LADO_MAXIMO = 1280;

    private int[] ids;
    private String[] nomes;
    private String[] uris;
    private String[] categorias;
    private int atual;
    // O contador mostra a posição na lista inteira, não só no trecho recebido
    private int inicio;
    private int total;

    private RecyclerView paginas;
    private LinearLayoutManager gerenciador;
    private PagerSnapHelper encaixe;

    private View topo;
    private View base;
    private View btnAnterior;
    private View btnProximo;
    private TextView txtPosicao;
    private TextView txtNome;
    private TextView txtCategoria;
    private View selo18;
    private boolean controlesVisiveis = true;

    public static void abrir(Context context, List<Produto> lista, Produto escolhido) {
        int indice = -1;
        for (int i = 0; i < lista.size(); i++) {
            if (lista.get(i).getId() == escolhido.getId()) {
                indice = i;
                break;
            }
        }
        if (indice < 0) {
            lista = Collections.singletonList(escolhido);
            indice = 0;
        }

        int inicio = Math.max(0, indice - VIZINHOS);
        int fim = Math.min(lista.size(), indice + VIZINHOS + 1);
        int quantidade = fim - inicio;

        int[] ids = new int[quantidade];
        String[] nomes = new String[quantidade];
        String[] uris = new String[quantidade];
        String[] categorias = new String[quantidade];
        for (int i = 0; i < quantidade; i++) {
            Produto p = lista.get(inicio + i);
            ids[i] = p.getId();
            nomes[i] = p.getNome();
            uris[i] = p.getImageUri();
            categorias[i] = p.getCategoria() == null ? null : p.getCategoria().name();
        }

        Intent intent = new Intent(context, ImagemProdutoActivity.class);
        intent.putExtra(EXTRA_IDS, ids);
        intent.putExtra(EXTRA_NOMES, nomes);
        intent.putExtra(EXTRA_URIS, uris);
        intent.putExtra(EXTRA_CATEGORIAS, categorias);
        intent.putExtra(EXTRA_POSICAO, indice - inicio);
        intent.putExtra(EXTRA_INICIO, inicio);
        intent.putExtra(EXTRA_TOTAL, lista.size());
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Janela.telaCheia(this, true, true);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_imagem_produto);

        Intent intent = getIntent();
        ids = intent.getIntArrayExtra(EXTRA_IDS);
        nomes = intent.getStringArrayExtra(EXTRA_NOMES);
        uris = intent.getStringArrayExtra(EXTRA_URIS);
        categorias = intent.getStringArrayExtra(EXTRA_CATEGORIAS);
        if (ids == null || ids.length == 0 || nomes == null || uris == null || categorias == null) {
            finish();
            return;
        }
        atual = savedInstanceState != null
                ? savedInstanceState.getInt(ESTADO_ATUAL)
                : intent.getIntExtra(EXTRA_POSICAO, 0);
        inicio = intent.getIntExtra(EXTRA_INICIO, 0);
        total = Math.max(intent.getIntExtra(EXTRA_TOTAL, ids.length), ids.length);

        topo = findViewById(R.id.topoVisualizador);
        base = findViewById(R.id.baseVisualizador);
        btnAnterior = findViewById(R.id.btnAnterior);
        btnProximo = findViewById(R.id.btnProximo);
        txtPosicao = findViewById(R.id.txtPosicao);
        txtNome = findViewById(R.id.txtNomeVisualizador);
        txtCategoria = findViewById(R.id.txtCategoriaVisualizador);
        selo18 = findViewById(R.id.selo18Visualizador);

        Janela.recuos(topo, Janela.TOPO);
        Janela.recuos(base, Janela.BASE);

        findViewById(R.id.btnFechar).setOnClickListener(v -> finish());
        btnAnterior.setOnClickListener(v -> irPara(atual - 1));
        btnProximo.setOnClickListener(v -> irPara(atual + 1));
        findViewById(R.id.btnEditarVisualizador).setOnClickListener(v -> editar());

        configurarPaginas();
        mostrarDetalhes();
    }

    private void configurarPaginas() {
        paginas = findViewById(R.id.paginas);
        gerenciador = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        paginas.setLayoutManager(gerenciador);
        paginas.setAdapter(new PaginasAdapter());

        // Faz cada deslize parar exatamente em um produto
        encaixe = new PagerSnapHelper();
        encaixe.attachToRecyclerView(paginas);
        paginas.scrollToPosition(atual);

        paginas.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView rv, int estado) {
                if (estado != RecyclerView.SCROLL_STATE_IDLE) return;
                View pagina = encaixe.findSnapView(gerenciador);
                if (pagina == null) return;
                int posicao = gerenciador.getPosition(pagina);
                if (posicao != atual) {
                    atual = posicao;
                    mostrarDetalhes();
                }
            }
        });
    }

    private void irPara(int posicao) {
        if (posicao < 0 || posicao >= ids.length) return;
        paginas.smoothScrollToPosition(posicao);
    }

    private void mostrarDetalhes() {
        txtPosicao.setText(ProdutoUtils.numero(inicio + atual + 1) + " de " + ProdutoUtils.numero(total));
        txtNome.setText(nomes[atual]);

        Categoria categoria = categoriaAtual();
        txtCategoria.setText(SeletorCategoria.rotulo(categoria));
        txtCategoria.setBackgroundTintList(ColorStateList.valueOf(EstiloCategoria.cor(this, categoria)));
        Drawable icone = ContextCompat.getDrawable(this, EstiloCategoria.icone(categoria));
        if (icone != null) {
            int tamanho = Math.round(18 * getResources().getDisplayMetrics().density);
            icone.setBounds(0, 0, tamanho, tamanho);
        }
        txtCategoria.setCompoundDrawablesRelative(icone, null, null, null);

        selo18.setVisibility(ProdutoUtils.isAlcoolica(produtoAtual()) ? View.VISIBLE : View.GONE);
        atualizarSetas();
    }

    private void atualizarSetas() {
        btnAnterior.setVisibility(controlesVisiveis && atual > 0 ? View.VISIBLE : View.INVISIBLE);
        btnProximo.setVisibility(controlesVisiveis && atual < ids.length - 1 ? View.VISIBLE : View.INVISIBLE);
    }

    private Categoria categoriaAtual() {
        return categorias[atual] == null ? null : Categoria.valueOf(categorias[atual]);
    }

    private Produto produtoAtual() {
        Produto produto = new Produto(nomes[atual], uris[atual], categoriaAtual());
        produto.setId(ids[atual]);
        return produto;
    }

    private void editar() {
        final int indice = atual;
        Produto produto = produtoAtual();
        EditorProduto.abrir(this, produto, () -> {
            nomes[indice] = produto.getNome();
            categorias[indice] = produto.getCategoria() == null ? null : produto.getCategoria().name();
            if (indice == atual) mostrarDetalhes();
        });
    }

    /** Tocar na foto esconde nome e botões, para ver só o produto. */
    private void alternarControles() {
        controlesVisiveis = !controlesVisiveis;
        float alfa = controlesVisiveis ? 1f : 0f;
        topo.animate().alpha(alfa).setDuration(180).start();
        base.animate().alpha(alfa).setDuration(180).start();
        topo.setClickable(controlesVisiveis);
        base.setClickable(controlesVisiveis);
        // Com os controles escondidos, o botão "Editar" não pode continuar recebendo toques
        findViewById(R.id.btnEditarVisualizador).setEnabled(controlesVisiveis);
        findViewById(R.id.btnFechar).setEnabled(controlesVisiveis);
        atualizarSetas();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(ESTADO_ATUAL, atual);
    }

    private class PaginasAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View pagina = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_pagina_produto, parent, false);
            pagina.setOnClickListener(v -> alternarControles());
            return new RecyclerView.ViewHolder(pagina) {
            };
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            Glide.with(ImagemProdutoActivity.this)
                    .load(ImagemUtils.modelo(uris[position]))
                    .override(LADO_MAXIMO, LADO_MAXIMO)
                    .downsample(DownsampleStrategy.AT_MOST)
                    .error(R.drawable.default_image)
                    .into((ImageView) holder.itemView);
        }

        @Override
        public int getItemCount() {
            return ids.length;
        }
    }
}
