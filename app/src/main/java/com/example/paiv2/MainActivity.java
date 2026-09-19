package com.example.paiv2;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.paiv2.database.AppDatabase;
import com.example.paiv2.database.PopuladorBanco;
import com.example.paiv2.entity.Categoria;
import com.example.paiv2.entity.Produto;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    // Espera o usuário parar de digitar antes de buscar, evitando refazer a busca a cada tecla
    private static final long ATRASO_BUSCA_MS = 200;
    private static final int COLUNAS = 2;

    private ProdutoAdapter adapter;
    private AppDatabase db;

    private EditText edtPesquisar;
    private ImageButton btnLimparBusca;
    private View blocoSaudacao;
    private TextView txtResumo;
    private View scrollCategorias;
    private RecyclerView recycler;
    private View vazioBusca;
    private TextView txtVazioDica;
    private View btnVazioNomear;
    private View cardNomear;
    private TextView txtNomearTitulo;
    private View cardRepetidos;
    private TextView txtRepetidosTitulo;
    private View cardPromocao;
    private View txtPromocaoVazia;
    private final View[] vitrines = new View[Promocao.DESTAQUES];
    private View painelCarregando;

    private final Map<Categoria, MaterialCardView> cartoesCategoria = new EnumMap<>(Categoria.class);

    private final Handler buscaHandler = new Handler(Looper.getMainLooper());
    private Runnable buscaAgendada;
    // Cada busca ganha um número de versão; resultados de buscas antigas são descartados
    private int versaoBusca = 0;

    // Produtos com o nome já preparado, para filtrar em memória sem depender de acentos
    private List<BuscaProdutos.Item> cacheBusca;
    private int totalProdutos = 0;
    private int totalSemNome = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Janela.telaCheia(this, true, false);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        edtPesquisar = findViewById(R.id.edtPesquisar);
        btnLimparBusca = findViewById(R.id.btnLimparBusca);
        blocoSaudacao = findViewById(R.id.blocoSaudacao);
        txtResumo = findViewById(R.id.txtResumo);
        scrollCategorias = findViewById(R.id.scrollCategorias);
        recycler = findViewById(R.id.recyclerPesquisa);
        vazioBusca = findViewById(R.id.vazioBusca);
        txtVazioDica = findViewById(R.id.txtVazioDica);
        btnVazioNomear = findViewById(R.id.btnVazioNomear);
        cardNomear = findViewById(R.id.cardNomear);
        txtNomearTitulo = findViewById(R.id.txtNomearTitulo);
        cardRepetidos = findViewById(R.id.cardRepetidos);
        txtRepetidosTitulo = findViewById(R.id.txtRepetidosTitulo);
        cardPromocao = findViewById(R.id.cardPromocao);
        txtPromocaoVazia = findViewById(R.id.txtPromocaoVazia);
        vitrines[0] = findViewById(R.id.vitrine1);
        vitrines[1] = findViewById(R.id.vitrine2);
        vitrines[2] = findViewById(R.id.vitrine3);
        painelCarregando = findViewById(R.id.painelCarregando);

        Janela.recuos(findViewById(R.id.cabecalho), Janela.TOPO);
        Janela.recuos(scrollCategorias, Janela.BASE);
        Janela.recuos(recycler, Janela.BASE);

        ((TextView) findViewById(R.id.txtSaudacao)).setText(saudacao());

        db = AppDatabase.getInstance(this);
        adapter = new ProdutoAdapter(this, new ArrayList<>());
        // Editar, excluir ou mover muda contagens e resultados: recarrega tudo
        adapter.setAoAlterarProdutos(this::carregarResumo);

        configurarCartoesCategoria();
        configurarGrade();
        configurarPesquisa();

        cardNomear.setOnClickListener(v -> NomearProdutosActivity.abrir(this, null));
        btnVazioNomear.setOnClickListener(v -> NomearProdutosActivity.abrir(this, null));
        cardRepetidos.setOnClickListener(v -> DuplicadosActivity.abrir(this));
        cardPromocao.setOnClickListener(v -> abrirPromocao());

        // Dá para marcar vários resultados da busca e mover, excluir ou pôr em promoção
        AcoesEmLote.instalar(this, adapter, findViewById(R.id.cabecalho));

        importarAssetsNaPrimeiraAbertura();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Produtos podem ter sido adicionados, editados ou removidos em outras telas
        carregarResumo();
    }

    private static String saudacao() {
        int hora = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hora >= 5 && hora < 12) return "Bom dia!";
        if (hora >= 12 && hora < 18) return "Boa tarde!";
        return "Boa noite!";
    }

    private void abrirPromocao() {
        startActivity(new Intent(this, PromocaoActivity.class));
    }

    private void configurarCartoesCategoria() {
        cartoesCategoria.put(Categoria.ALIMENTOS, findViewById(R.id.tileAlimentos));
        cartoesCategoria.put(Categoria.DOCES, findViewById(R.id.tileDoces));
        cartoesCategoria.put(Categoria.BEBIDAS, findViewById(R.id.tileBebidas));
        cartoesCategoria.put(Categoria.HIGIENE, findViewById(R.id.tileHigiene));
        cartoesCategoria.put(Categoria.OUTROS, findViewById(R.id.tileOutros));

        for (Map.Entry<Categoria, MaterialCardView> entrada : cartoesCategoria.entrySet()) {
            Categoria categoria = entrada.getKey();
            MaterialCardView cartao = entrada.getValue();

            cartao.setCardBackgroundColor(EstiloCategoria.cor(this, categoria));
            // Recorta a marca-d'água nos cantos arredondados
            cartao.setClipToOutline(true);
            ((ImageView) cartao.findViewById(R.id.tileIcone)).setImageResource(EstiloCategoria.icone(categoria));
            ((ImageView) cartao.findViewById(R.id.tileMarcaDagua)).setImageResource(EstiloCategoria.icone(categoria));
            ((TextView) cartao.findViewById(R.id.tileNome)).setText(SeletorCategoria.rotulo(categoria));
            cartao.setOnClickListener(v -> abrirCategoria(categoria));
        }
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
        recycler.setAdapter(adapter);

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
                // Durante a busca a saudação sai de cena, sobrando espaço para os resultados
                blocoSaudacao.setVisibility(texto.isEmpty() ? View.VISIBLE : View.GONE);

                if (texto.isEmpty()) {
                    // Invalida qualquer busca em andamento e volta para as categorias
                    versaoBusca++;
                    recycler.setVisibility(View.GONE);
                    vazioBusca.setVisibility(View.GONE);
                    scrollCategorias.setVisibility(View.VISIBLE);
                    return;
                }

                scrollCategorias.setVisibility(View.GONE);
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
            carregarResumo();
        });
    }

    private void abrirCategoria(Categoria categoria) {
        startActivity(CategoriaActivity.novaIntent(this, categoria));
    }

    /** Recarrega contagens, vitrine da promoção e a base da busca. */
    private void carregarResumo() {
        new Thread(() -> {
            List<Produto> todos = db.produtoDao().listarTodos();

            List<BuscaProdutos.Item> itens = BuscaProdutos.preparar(todos);
            Map<Categoria, Integer> porCategoria = new EnumMap<>(Categoria.class);
            int semNome = 0;
            for (Produto p : todos) {
                if (p.getCategoria() != null) porCategoria.merge(p.getCategoria(), 1, Integer::sum);
                if (ProdutoUtils.semNome(p)) semNome++;
            }
            List<Produto> promocao = Promocao.produtos(this, todos);
            synchronized (this) {
                cacheBusca = itens;
            }

            final int faltamNomear = semNome;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                totalProdutos = todos.size();
                totalSemNome = faltamNomear;

                txtResumo.setText(ProdutoUtils.contagem(totalProdutos,
                        "produto no catálogo", "produtos no catálogo"));

                for (Map.Entry<Categoria, MaterialCardView> entrada : cartoesCategoria.entrySet()) {
                    int quantidade = porCategoria.getOrDefault(entrada.getKey(), 0);
                    String texto = quantidade == 0 ? "Vazio"
                            : ProdutoUtils.contagem(quantidade, "produto", "produtos");
                    ((TextView) entrada.getValue().findViewById(R.id.tileQuantidade)).setText(texto);
                    entrada.getValue().setContentDescription(
                            SeletorCategoria.rotulo(entrada.getKey()) + ", " + texto);
                }

                cardNomear.setVisibility(faltamNomear > 0 ? View.VISIBLE : View.GONE);
                txtNomearTitulo.setText(ProdutoUtils.contagem(faltamNomear,
                        "produto sem nome", "produtos sem nome"));

                atualizarVitrine(promocao);
                rebuscar();
                procurarRepetidos();
            });
        }).start();
    }

    /** Mostra na tela inicial os três primeiros produtos da promoção. */
    private void atualizarVitrine(List<Produto> promocao) {
        txtPromocaoVazia.setVisibility(promocao.isEmpty() ? View.VISIBLE : View.GONE);

        for (int i = 0; i < vitrines.length; i++) {
            View vaga = vitrines[i];
            MaterialCardView cartao = vaga.findViewById(R.id.vitrineCard);
            ImageView foto = vaga.findViewById(R.id.vitrineFoto);
            View semFoto = vaga.findViewById(R.id.vitrineVazia);
            TextView nome = vaga.findViewById(R.id.vitrineNome);

            if (i < promocao.size()) {
                Produto produto = promocao.get(i);
                semFoto.setVisibility(View.GONE);
                foto.setVisibility(View.VISIBLE);
                Glide.with(this)
                        .load(ImagemUtils.modelo(produto.getImageUri()))
                        .override(240, 240)
                        .fitCenter()
                        .error(R.drawable.default_image)
                        .into(foto);
                nome.setText(produto.getNome());
                cartao.setContentDescription(produto.getNome());
                cartao.setOnClickListener(v ->
                        ImagemProdutoActivity.abrir(this, new ArrayList<>(promocao), produto));
            } else {
                Glide.with(this).clear(foto);
                foto.setVisibility(View.GONE);
                semFoto.setVisibility(View.VISIBLE);
                nome.setText("");
                cartao.setContentDescription("Vaga livre na promoção");
                cartao.setOnClickListener(v -> abrirPromocao());
            }
        }
    }

    /** Procura produtos repetidos em segundo plano e avisa se achar. */
    private void procurarRepetidos() {
        DetectorDuplicados.procurar(this, db, grupos -> {
            if (isFinishing() || isDestroyed()) return;
            int copias = DetectorDuplicados.totalRepetidos(grupos);
            cardRepetidos.setVisibility(copias > 0 ? View.VISIBLE : View.GONE);
            txtRepetidosTitulo.setText(ProdutoUtils.contagem(copias,
                    "produto repetido", "produtos repetidos"));
        });
    }

    /** Refaz a busca atual, se houver algo digitado. */
    private void rebuscar() {
        String texto = edtPesquisar.getText().toString().trim();
        if (!texto.isEmpty()) buscarProdutos(texto);
    }

    private void buscarProdutos(String texto) {
        final int versao = ++versaoBusca;
        new Thread(() -> {
            List<Produto> resultado = BuscaProdutos.buscar(obterCacheBusca(), texto);
            runOnUiThread(() -> {
                // Se outra busca começou (ou o campo foi limpo), esse resultado já é velho
                if (versao != versaoBusca) return;
                adapter.atualizarLista(resultado);

                boolean vazio = resultado.isEmpty();
                recycler.setVisibility(vazio ? View.GONE : View.VISIBLE);
                vazioBusca.setVisibility(vazio ? View.VISIBLE : View.GONE);
                if (vazio) atualizarDicaBuscaVazia();
            });
        }).start();
    }

    /** Se boa parte do catálogo está sem nome, explica por que a busca não acha e oferece nomear. */
    private void atualizarDicaBuscaVazia() {
        boolean muitosSemNome = totalProdutos > 0 && totalSemNome * 2 >= totalProdutos;
        txtVazioDica.setText(muitosSemNome
                ? ProdutoUtils.numero(totalSemNome) + " produtos ainda estão sem nome. Dê nomes a eles para encontrá-los na busca."
                : "Confira a escrita ou tente outra palavra.");
        btnVazioNomear.setVisibility(totalSemNome > 0 ? View.VISIBLE : View.GONE);
    }

    private synchronized List<BuscaProdutos.Item> obterCacheBusca() {
        if (cacheBusca == null) {
            cacheBusca = BuscaProdutos.preparar(db.produtoDao().listarTodos());
        }
        return cacheBusca;
    }
}
