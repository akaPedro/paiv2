package com.example.paiv2;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.paiv2.database.AppDatabase;
import com.example.paiv2.entity.Categoria;
import com.example.paiv2.entity.Produto;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // Espera o usuário parar de digitar antes de buscar, evitando refazer a busca a cada tecla
    private static final long ATRASO_BUSCA_MS = 200;

    private ProdutoAdapter adapter;
    private AppDatabase db;

    private EditText edtPesquisar;
    private ScrollView scrollCategorias;
    private RecyclerView recycler;
    private TextView txtSemResultados;

    private final Handler buscaHandler = new Handler(Looper.getMainLooper());
    private Runnable buscaAgendada;
    // Cada busca ganha um número de versão; resultados de buscas antigas são descartados
    private int versaoBusca = 0;

    // Produtos com nome já normalizado, para filtrar em memória sem depender de acentos
    private List<ItemBusca> cacheBusca;

    private static class ItemBusca {
        final Produto produto;
        final String nomeNormalizado;

        ItemBusca(Produto produto) {
            this.produto = produto;
            this.nomeNormalizado = ProdutoUtils.normalizar(produto.getNome());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Referências do Layout
        edtPesquisar = findViewById(R.id.edtPesquisar);
        scrollCategorias = findViewById(R.id.scrollCategorias);
        recycler = findViewById(R.id.recyclerPesquisa);
        txtSemResultados = findViewById(R.id.txtSemResultados);

        // Configuração do Banco e Adapter
        adapter = new ProdutoAdapter(this, new ArrayList<>());
        db = AppDatabase.getInstance(this);

        recycler.setLayoutManager(new GridLayoutManager(this, 2));
        recycler.setAdapter(adapter);

        // IDs dos Botões conforme seu XML
        Button BAlim = findViewById(R.id.btnAlimentos);
        Button BBebi = findViewById(R.id.btnBebidas);
        Button BHili = findViewById(R.id.btnHigiene);
        Button BOut = findViewById(R.id.btnOutros);
        Button BDoc = findViewById(R.id.btnDoces);

        // Listeners dos Botões: todas as categorias abrem a mesma tela.
        // A Main não é finalizada, então o botão "voltar" retorna para cá naturalmente.
        BAlim.setOnClickListener(v -> abrirCategoria(Categoria.ALIMENTOS));
        BBebi.setOnClickListener(v -> abrirCategoria(Categoria.BEBIDAS));
        BHili.setOnClickListener(v -> abrirCategoria(Categoria.HIGIENE));
        BOut.setOnClickListener(v -> abrirCategoria(Categoria.OUTROS));
        BDoc.setOnClickListener(v -> abrirCategoria(Categoria.DOCES));

        // Lógica de Pesquisa e Visibilidade
        edtPesquisar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (buscaAgendada != null) buscaHandler.removeCallbacks(buscaAgendada);
                String texto = s.toString().trim();

                if (texto.isEmpty()) {
                    // Invalida qualquer busca em andamento e volta para as categorias
                    versaoBusca++;
                    recycler.setVisibility(View.GONE);
                    txtSemResultados.setVisibility(View.GONE);
                    scrollCategorias.setVisibility(View.VISIBLE);
                    return;
                }

                scrollCategorias.setVisibility(View.GONE);
                recycler.setVisibility(View.VISIBLE);

                buscaAgendada = () -> buscarProdutos(texto);
                buscaHandler.postDelayed(buscaAgendada, ATRASO_BUSCA_MS);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Correção do EdgeToEdge: Usando scrollCategorias como referência de View
        // Para evitar o crash, o findViewById deve encontrar uma View existente.
        ViewCompat.setOnApplyWindowInsetsListener(scrollCategorias, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), systemBars.top, v.getPaddingRight(), systemBars.bottom);
            return insets;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Produtos podem ter sido adicionados/editados/removidos em outras telas
        invalidarCacheBusca();
        String texto = edtPesquisar.getText().toString().trim();
        if (!texto.isEmpty()) {
            buscarProdutos(texto);
        }
    }

    private void abrirCategoria(Categoria categoria) {
        startActivity(CategoriaActivity.novaIntent(this, categoria));
    }

    private void buscarProdutos(String texto) {
        final int versao = ++versaoBusca;
        new Thread(() -> {
            List<Produto> resultado = filtrar(obterCacheBusca(), texto);
            runOnUiThread(() -> {
                // Se outra busca começou (ou o campo foi limpo), esse resultado já é velho
                if (versao != versaoBusca) return;
                adapter.atualizarLista(resultado);
                txtSemResultados.setVisibility(resultado.isEmpty() ? View.VISIBLE : View.GONE);
            });
        }).start();
    }

    private synchronized List<ItemBusca> obterCacheBusca() {
        if (cacheBusca == null) {
            List<ItemBusca> itens = new ArrayList<>();
            for (Produto p : db.produtoDao().listarTodos()) {
                itens.add(new ItemBusca(p));
            }
            cacheBusca = itens;
        }
        return cacheBusca;
    }

    private synchronized void invalidarCacheBusca() {
        cacheBusca = null;
    }

    /**
     * Filtra ignorando acentos e maiúsculas. Cada palavra digitada precisa aparecer
     * no nome ("leite po" encontra "Leite em Pó"). Resultados onde o termo aparece
     * mais no início do nome vêm primeiro.
     */
    private static List<Produto> filtrar(List<ItemBusca> itens, String texto) {
        String termo = ProdutoUtils.normalizar(texto);
        String[] palavras = termo.split("\\s+");

        List<ItemBusca> encontrados = new ArrayList<>();
        for (ItemBusca item : itens) {
            boolean contemTodas = true;
            for (String palavra : palavras) {
                if (!item.nomeNormalizado.contains(palavra)) {
                    contemTodas = false;
                    break;
                }
            }
            if (contemTodas) encontrados.add(item);
        }

        String primeiraPalavra = palavras[0];
        encontrados.sort((a, b) -> {
            int posA = a.nomeNormalizado.indexOf(primeiraPalavra);
            int posB = b.nomeNormalizado.indexOf(primeiraPalavra);
            if (posA != posB) return Integer.compare(posA, posB);
            return a.nomeNormalizado.compareTo(b.nomeNormalizado);
        });

        List<Produto> resultado = new ArrayList<>(encontrados.size());
        for (ItemBusca item : encontrados) {
            resultado.add(item.produto);
        }
        return resultado;
    }
}
