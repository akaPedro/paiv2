package com.example.paiv2;

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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.paiv2.database.AppDatabase;
import com.example.paiv2.entity.Produto;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Escolha de vários produtos, com busca, para entrar na promoção.
 */
public class EscolherProdutosActivity extends AppCompatActivity {

    private static final long ATRASO_BUSCA_MS = 200;
    private static final int COLUNAS = 2;

    private AppDatabase db;
    private ProdutoAdapter adapter;
    private List<Produto> todos = new ArrayList<>();
    private List<BuscaProdutos.Item> itens = new ArrayList<>();

    private MaterialToolbar toolbar;
    private EditText edtPesquisar;
    private ImageButton btnLimparBusca;
    private RecyclerView recycler;
    private View vazio;
    private View carregando;
    private MaterialButton btnConfirmar;

    private final Handler buscaHandler = new Handler(Looper.getMainLooper());
    private Runnable buscaAgendada;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Janela.telaCheia(this, true, false);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_escolher_produtos);

        db = AppDatabase.getInstance(this);

        Janela.recuos(findViewById(R.id.cabecalho), Janela.TOPO);
        Janela.recuos(findViewById(R.id.rodape), Janela.BASE | Janela.TECLADO);

        toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.setSubtitle("Nenhum marcado");

        edtPesquisar = findViewById(R.id.edtPesquisar);
        btnLimparBusca = findViewById(R.id.btnLimparBusca);
        vazio = findViewById(R.id.vazioEscolher);
        carregando = findViewById(R.id.carregandoEscolher);
        btnConfirmar = findViewById(R.id.btnConfirmar);
        btnConfirmar.setOnClickListener(v -> confirmar());

        configurarGrade();
        configurarBusca();
        carregar();
    }

    private void configurarGrade() {
        recycler = findViewById(R.id.recyclerEscolher);
        GridLayoutManager manager = new GridLayoutManager(this, COLUNAS);
        manager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return adapter.isDivisor(position) ? COLUNAS : 1;
            }
        });
        recycler.setLayoutManager(manager);

        adapter = new ProdutoAdapter(this, new ArrayList<>());
        // A busca troca a lista o tempo todo; o que já foi marcado precisa continuar marcado
        adapter.setPodarSelecaoAoAtualizar(false);
        adapter.setAoMudarSelecao((ativo, quantidade) -> atualizarRodape(quantidade));
        adapter.entrarModoSelecao();
        recycler.setAdapter(adapter);

        recycler.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView rv, int novoEstado) {
                if (novoEstado == RecyclerView.SCROLL_STATE_DRAGGING) esconderTeclado();
            }
        });
    }

    private void configurarBusca() {
        edtPesquisar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (buscaAgendada != null) buscaHandler.removeCallbacks(buscaAgendada);
                String texto = s.toString().trim();
                btnLimparBusca.setVisibility(texto.isEmpty() ? View.INVISIBLE : View.VISIBLE);
                buscaAgendada = () -> mostrar(texto);
                buscaHandler.postDelayed(buscaAgendada, ATRASO_BUSCA_MS);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnLimparBusca.setOnClickListener(v -> {
            edtPesquisar.setText("");
            esconderTeclado();
        });

        edtPesquisar.setOnEditorActionListener((v, acao, evento) -> {
            if (acao == EditorInfo.IME_ACTION_SEARCH) {
                esconderTeclado();
                return true;
            }
            return false;
        });
    }

    private void carregar() {
        new Thread(() -> {
            List<Produto> lista = db.produtoDao().listarTodos();
            List<BuscaProdutos.Item> preparados = BuscaProdutos.preparar(lista);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                todos = lista;
                itens = preparados;
                mostrar(edtPesquisar.getText().toString().trim());
            });
        }).start();
    }

    private void mostrar(String texto) {
        // Sem busca, mostra o catálogo inteiro separado por categoria
        List<Produto> lista = texto.isEmpty()
                ? BuscaProdutos.agrupar(todos)
                : BuscaProdutos.buscar(itens, texto);
        adapter.atualizarLista(lista);
        carregando.setVisibility(View.GONE);
        vazio.setVisibility(lista.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void atualizarRodape(int quantidade) {
        toolbar.setSubtitle(quantidade == 0 ? "Nenhum marcado"
                : ProdutoUtils.contagem(quantidade, "marcado", "marcados"));
        btnConfirmar.setEnabled(quantidade > 0);
        btnConfirmar.setText(quantidade == 0
                ? "Marque os produtos em oferta"
                : "Adicionar " + ProdutoUtils.contagem(quantidade, "produto", "produtos"));
    }

    private void confirmar() {
        List<Integer> ids = adapter.getIdsSelecionados();
        if (ids.isEmpty()) return;
        Promocao.adicionar(this, ids);
        setResult(RESULT_OK);
        finish();
    }

    private void esconderTeclado() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(edtPesquisar.getWindowToken(), 0);
    }
}
