package com.example.paiv2;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    // Espera o usuário parar de digitar antes de buscar, evitando refazer a busca a cada tecla
    private static final long ATRASO_BUSCA_MS = 200;
    private static final int COLUNAS = 2;
    // Abaixo disso, tolerar erro de digitação traria resultados demais sem relação
    private static final int MIN_LETRAS_TOLERANCIA = 4;

    private ProdutoAdapter adapter;
    private AppDatabase db;

    private EditText edtPesquisar;
    private ImageButton btnLimparBusca;
    private ScrollView scrollCategorias;
    private RecyclerView recycler;
    private TextView txtSemResultados;
    private View painelCarregando;

    private final Handler buscaHandler = new Handler(Looper.getMainLooper());
    private Runnable buscaAgendada;
    // Cada busca ganha um número de versão; resultados de buscas antigas são descartados
    private int versaoBusca = 0;

    // Produtos com nome já normalizado, para filtrar em memória sem depender de acentos
    private List<ItemBusca> cacheBusca;

    private static class ItemBusca {
        final Produto produto;
        final String nomeNormalizado;
        final String[] palavras;

        ItemBusca(Produto produto) {
            this.produto = produto;
            this.nomeNormalizado = ProdutoUtils.normalizar(produto.getNome());
            this.palavras = this.nomeNormalizado.split("\\s+");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Referências do Layout
        edtPesquisar = findViewById(R.id.edtPesquisar);
        btnLimparBusca = findViewById(R.id.btnLimparBusca);
        scrollCategorias = findViewById(R.id.scrollCategorias);
        recycler = findViewById(R.id.recyclerPesquisa);
        txtSemResultados = findViewById(R.id.txtSemResultados);
        painelCarregando = findViewById(R.id.painelCarregando);

        // Configuração do Banco e Adapter
        adapter = new ProdutoAdapter(this, new ArrayList<>());
        // Editar, excluir ou mudar a categoria muda os resultados: refaz a busca
        adapter.setAoAlterarProdutos(() -> {
            invalidarCacheBusca();
            rebuscar();
        });
        db = AppDatabase.getInstance(this);

        importarAssetsNaPrimeiraAbertura();

        configurarGrade();
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

        configurarPesquisa();

        // Correção do EdgeToEdge: Usando scrollCategorias como referência de View
        // Para evitar o crash, o findViewById deve encontrar uma View existente.
        ViewCompat.setOnApplyWindowInsetsListener(scrollCategorias, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), systemBars.top, v.getPaddingRight(), systemBars.bottom);
            return insets;
        });
    }

    private void configurarGrade() {
        GridLayoutManager manager = new GridLayoutManager(this, COLUNAS);
        // O cabeçalho de cada categoria ocupa a linha inteira
        manager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return adapter.isDivisor(position) ? COLUNAS : 1;
            }
        });
        recycler.setLayoutManager(manager);

        // Rolar os resultados tira o teclado da frente
        recycler.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView rv, int novoEstado) {
                if (novoEstado == RecyclerView.SCROLL_STATE_DRAGGING) esconderTeclado();
            }
        });
    }

    private void configurarPesquisa() {
        edtPesquisar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (buscaAgendada != null) buscaHandler.removeCallbacks(buscaAgendada);
                String texto = s.toString().trim();

                btnLimparBusca.setVisibility(texto.isEmpty() ? View.INVISIBLE : View.VISIBLE);

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

        btnLimparBusca.setOnClickListener(v -> {
            edtPesquisar.setText("");
            esconderTeclado();
        });

        edtPesquisar.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                esconderTeclado();
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Produtos podem ter sido adicionados/editados/removidos em outras telas
        invalidarCacheBusca();
        rebuscar();
    }

    private void esconderTeclado() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(edtPesquisar.getWindowToken(), 0);
    }

    /**
     * Na primeira abertura depois da instalação, cadastra as imagens que vêm
     * dentro do app. O painel cobre a tela para ninguém abrir uma categoria
     * ainda vazia no meio do processo.
     */
    private void importarAssetsNaPrimeiraAbertura() {
        if (PopuladorBanco.jaImportado(this)) return;

        painelCarregando.setVisibility(View.VISIBLE);
        PopuladorBanco.importarSeNecessario(this, db, () -> {
            if (isFinishing() || isDestroyed()) return;
            painelCarregando.setVisibility(View.GONE);
            invalidarCacheBusca();
        });
    }

    private void abrirCategoria(Categoria categoria) {
        startActivity(CategoriaActivity.novaIntent(this, categoria));
    }

    /** Refaz a busca atual, se houver algo digitado. */
    private void rebuscar() {
        String texto = edtPesquisar.getText().toString().trim();
        if (!texto.isEmpty()) buscarProdutos(texto);
    }

    private void buscarProdutos(String texto) {
        final int versao = ++versaoBusca;
        new Thread(() -> {
            List<Produto> resultado = buscar(obterCacheBusca(), texto);
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

    // --- BUSCA ---

    /**
     * Procura ignorando acentos e maiúsculas. Cada palavra digitada precisa aparecer
     * no nome ("leite po" encontra "Leite em Pó"). Se nada for encontrado, tenta de
     * novo perdoando um erro de digitação por palavra. O resultado vem agrupado por
     * categoria, para o usuário saber onde cada produto está.
     */
    private static List<Produto> buscar(List<ItemBusca> itens, String texto) {
        String[] palavras = ProdutoUtils.normalizar(texto).split("\\s+");

        List<ItemBusca> encontrados = casar(itens, palavras, false);
        if (encontrados.isEmpty()) {
            encontrados = casar(itens, palavras, true);
        }

        ordenarPorRelevancia(encontrados, palavras[0]);
        return agruparPorCategoria(encontrados);
    }

    private static List<ItemBusca> casar(List<ItemBusca> itens, String[] palavras, boolean tolerante) {
        List<ItemBusca> encontrados = new ArrayList<>();
        for (ItemBusca item : itens) {
            boolean contemTodas = true;
            for (String palavra : palavras) {
                if (!combina(item, palavra, tolerante)) {
                    contemTodas = false;
                    break;
                }
            }
            if (contemTodas) encontrados.add(item);
        }
        return encontrados;
    }

    private static boolean combina(ItemBusca item, String palavra, boolean tolerante) {
        if (item.nomeNormalizado.contains(palavra)) return true;
        if (!tolerante || palavra.length() < MIN_LETRAS_TOLERANCIA) return false;

        // Compara com cada palavra do nome aceitando um erro: "arros" acha "Arroz"
        for (String parte : item.palavras) {
            if (ProdutoUtils.ateUmaEdicao(parte, palavra)) return true;
        }
        return false;
    }

    /** Quem tem o termo mais no começo do nome aparece primeiro. */
    private static void ordenarPorRelevancia(List<ItemBusca> encontrados, String primeiraPalavra) {
        encontrados.sort((a, b) -> {
            int posA = posicao(a.nomeNormalizado, primeiraPalavra);
            int posB = posicao(b.nomeNormalizado, primeiraPalavra);
            if (posA != posB) return Integer.compare(posA, posB);
            return a.nomeNormalizado.compareTo(b.nomeNormalizado);
        });
    }

    private static int posicao(String nome, String termo) {
        int pos = nome.indexOf(termo);
        // Achado só por semelhança: vai para o fim da lista
        return pos < 0 ? Integer.MAX_VALUE : pos;
    }

    private static List<Produto> agruparPorCategoria(List<ItemBusca> encontrados) {
        // LinkedHashMap pré-carregado mantém a ordem em que as categorias são exibidas
        Map<Categoria, List<Produto>> grupos = new LinkedHashMap<>();
        for (Categoria categoria : SeletorCategoria.opcoes()) {
            grupos.put(categoria, new ArrayList<>());
        }

        for (ItemBusca item : encontrados) {
            Categoria categoria = item.produto.getCategoria();
            List<Produto> grupo = grupos.get(categoria);
            // Categoria desconhecida não some da busca: cai em Outros
            if (grupo == null) grupo = grupos.get(Categoria.OUTROS);
            grupo.add(item.produto);
        }

        List<Produto> resultado = new ArrayList<>();
        for (Map.Entry<Categoria, List<Produto>> grupo : grupos.entrySet()) {
            if (grupo.getValue().isEmpty()) continue;
            resultado.add(ProdutoUtils.criarDivisor(grupo.getKey().name()));
            resultado.addAll(grupo.getValue());
        }
        return resultado;
    }
}
