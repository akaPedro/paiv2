package com.example.paiv2;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;

import com.bumptech.glide.Glide;
import com.example.paiv2.database.AppDatabase;
import com.example.paiv2.entity.Categoria;
import com.example.paiv2.entity.Produto;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Passa pelos produtos que ainda têm nome provisório, um de cada vez. Lê o texto
 * da embalagem e oferece como sugestões tocáveis, para nomear em poucos toques.
 */
public class NomearProdutosActivity extends AppCompatActivity {

    private static final String EXTRA_CATEGORIA = "categoria";

    private AppDatabase db;
    private LeitorDeRotulo leitor;
    private Categoria filtro;

    private final List<Produto> fila = new ArrayList<>();
    private final Set<Integer> nomeadosNestaVez = new HashSet<>();
    private int indice = -1;
    // Descarta sugestões que chegam depois de a pessoa já ter ido para outro produto
    private int versaoLeitura = 0;
    private Categoria categoriaEscolhida;

    private MaterialToolbar toolbar;
    private LinearProgressIndicator progresso;
    private NestedScrollView scroll;
    private View painelConcluido;
    private View rodape;
    private ImageView imgProduto;
    private TextView txtNomeAtual;
    private View progressoLeitura;
    private ChipGroup chipsSugestoes;
    private View txtSemSugestoes;
    private TextInputLayout campoNome;
    private TextInputEditText edtNome;
    private ChipGroup chipsCategoria;
    private MaterialButton btnAnterior;

    public static void abrir(Context context, @Nullable Categoria categoria) {
        Intent intent = new Intent(context, NomearProdutosActivity.class);
        if (categoria != null) intent.putExtra(EXTRA_CATEGORIA, categoria.name());
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Janela.telaCheia(this, true, false);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nomear);

        String categoria = getIntent().getStringExtra(EXTRA_CATEGORIA);
        filtro = categoria == null ? null : Categoria.valueOf(categoria);
        db = AppDatabase.getInstance(this);
        leitor = new LeitorDeRotulo(this);

        toolbar = findViewById(R.id.toolbar);
        progresso = findViewById(R.id.progressoNomear);
        scroll = findViewById(R.id.scrollNomear);
        painelConcluido = findViewById(R.id.painelConcluido);
        rodape = findViewById(R.id.rodapeNomear);
        imgProduto = findViewById(R.id.imgNomear);
        txtNomeAtual = findViewById(R.id.txtNomeAtual);
        progressoLeitura = findViewById(R.id.progressoLeitura);
        chipsSugestoes = findViewById(R.id.chipsSugestoes);
        txtSemSugestoes = findViewById(R.id.txtSemSugestoes);
        campoNome = findViewById(R.id.inputNomeNomear);
        edtNome = findViewById(R.id.edtNomeNomear);
        chipsCategoria = findViewById(R.id.chipsCategoriaNomear);
        btnAnterior = findViewById(R.id.btnAnteriorNomear);

        Janela.recuos(findViewById(R.id.cabecalho), Janela.TOPO);
        Janela.recuos(rodape, Janela.BASE | Janela.TECLADO);
        Janela.recuos(painelConcluido, Janela.BASE);

        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.setSubtitle("Carregando…");
        btnAnterior.setOnClickListener(v -> mostrar(indice - 1));
        findViewById(R.id.btnPular).setOnClickListener(v -> avancar());
        findViewById(R.id.btnSalvarProximo).setOnClickListener(v -> salvarEAvancar());
        findViewById(R.id.btnConcluir).setOnClickListener(v -> finish());

        // "Próximo" no teclado salva e vai para o seguinte, sem precisar tirar a mão
        edtNome.setOnEditorActionListener((v, acao, evento) -> {
            if (acao == EditorInfo.IME_ACTION_NEXT || acao == EditorInfo.IME_ACTION_DONE) {
                salvarEAvancar();
                return true;
            }
            return false;
        });
        edtNome.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                campoNome.setError(null);
                // Campo apagado: as sugestões já usadas voltam a ficar disponíveis
                if (s.toString().trim().isEmpty()) reativarSugestoes();
            }
        });

        carregarFila();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (leitor != null) leitor.fechar();
    }

    private void carregarFila() {
        new Thread(() -> {
            List<Produto> encontrados = new ArrayList<>();
            Categoria[] categorias = filtro != null ? new Categoria[]{filtro} : SeletorCategoria.opcoes();
            for (Categoria categoria : categorias) {
                for (Produto p : db.produtoDao().listarPorCategoriaOrdenado(categoria)) {
                    if (ProdutoUtils.semNome(p)) encontrados.add(p);
                }
            }

            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                fila.addAll(encontrados);
                if (fila.isEmpty()) {
                    mostrarConcluido();
                } else {
                    mostrar(0);
                }
            });
        }).start();
    }

    private void mostrar(int posicao) {
        if (posicao < 0 || posicao >= fila.size()) return;
        indice = posicao;
        Produto produto = fila.get(posicao);

        String onde = filtro != null ? SeletorCategoria.rotulo(filtro) + " · " : "";
        toolbar.setSubtitle(onde + ProdutoUtils.numero(posicao + 1) + " de " + ProdutoUtils.numero(fila.size()));
        progresso.setMax(fila.size());
        progresso.setProgressCompat(posicao + 1, true);

        Glide.with(this)
                .load(ImagemUtils.modelo(produto.getImageUri()))
                .override(900, 900)
                .fitCenter()
                .error(R.drawable.default_image)
                .into(imgProduto);
        txtNomeAtual.setText(produto.getNome());

        // Voltando a um produto já nomeado nesta vez, o nome dado aparece para correção
        edtNome.setText(ProdutoUtils.semNome(produto) ? "" : produto.getNome());
        edtNome.setSelection(edtNome.length());
        campoNome.setError(null);

        categoriaEscolhida = produto.getCategoria();
        ChipsCategoria.montar(chipsCategoria, categoriaEscolhida, escolhida -> categoriaEscolhida = escolhida);

        btnAnterior.setEnabled(posicao > 0);
        scroll.scrollTo(0, 0);
        lerSugestoes(produto);
    }

    private void lerSugestoes(Produto produto) {
        final int pedido = ++versaoLeitura;
        chipsSugestoes.removeAllViews();
        txtSemSugestoes.setVisibility(View.GONE);
        progressoLeitura.setVisibility(View.VISIBLE);

        leitor.ler(produto.getImageUri(), sugestoes -> {
            if (pedido != versaoLeitura || isDestroyed()) return;
            progressoLeitura.setVisibility(View.GONE);
            if (sugestoes.isEmpty()) {
                txtSemSugestoes.setVisibility(View.VISIBLE);
                return;
            }
            for (String sugestao : sugestoes) {
                chipsSugestoes.addView(criarChipSugestao(sugestao));
            }
        });
    }

    private Chip criarChipSugestao(String texto) {
        Chip chip = (Chip) LayoutInflater.from(this).inflate(R.layout.item_chip_sugestao, chipsSugestoes, false);
        chip.setText(texto);
        chip.setOnClickListener(v -> {
            acrescentarAoNome(texto);
            // Marca como usada, para não entrar duas vezes no nome
            chip.setChipIconResource(R.drawable.ic_check);
            chip.setEnabled(false);
        });
        return chip;
    }

    private void acrescentarAoNome(String texto) {
        String atual = edtNome.getText() == null ? "" : edtNome.getText().toString().trim();
        String novo = atual.isEmpty() ? texto : atual + " " + texto;
        edtNome.setText(novo);
        edtNome.setSelection(novo.length());
    }

    private void reativarSugestoes() {
        for (int i = 0; i < chipsSugestoes.getChildCount(); i++) {
            Chip chip = (Chip) chipsSugestoes.getChildAt(i);
            chip.setEnabled(true);
            chip.setChipIconResource(R.drawable.ic_adicionar);
        }
    }

    private void salvarEAvancar() {
        if (indice < 0 || indice >= fila.size()) return;

        String nome = edtNome.getText() == null ? "" : edtNome.getText().toString().trim();
        if (nome.isEmpty()) {
            campoNome.setError("Digite um nome ou toque em Pular");
            return;
        }

        Produto produto = fila.get(indice);
        produto.setNome(nome);
        produto.setCategoria(categoriaEscolhida);
        nomeadosNestaVez.add(produto.getId());
        // Grava em segundo plano e já mostra o próximo, sem esperar
        new Thread(() -> db.produtoDao().atualizar(produto)).start();

        avancar();
    }

    private void avancar() {
        if (indice + 1 < fila.size()) {
            mostrar(indice + 1);
        } else {
            mostrarConcluido();
        }
    }

    private void mostrarConcluido() {
        esconderTeclado();
        versaoLeitura++;
        scroll.setVisibility(View.GONE);
        rodape.setVisibility(View.GONE);
        painelConcluido.setVisibility(View.VISIBLE);
        toolbar.setSubtitle(filtro != null ? SeletorCategoria.rotulo(filtro) : null);
        progresso.setMax(Math.max(1, fila.size()));
        progresso.setProgressCompat(Math.max(1, fila.size()), true);

        TextView titulo = findViewById(R.id.txtConcluidoTitulo);
        TextView resumo = findViewById(R.id.txtConcluidoResumo);
        int nomeados = nomeadosNestaVez.size();

        if (fila.isEmpty()) {
            titulo.setText("Nada para nomear");
            resumo.setText(filtro != null
                    ? "Todos os produtos de " + SeletorCategoria.rotulo(filtro) + " já têm nome."
                    : "Todos os produtos já têm nome.");
            return;
        }

        titulo.setText(nomeados > 0 ? "Pronto!" : "Fim da lista");
        String texto = nomeados == 0
                ? "Nenhum produto foi nomeado desta vez."
                : "Você deu nome a " + ProdutoUtils.contagem(nomeados, "produto", "produtos") + ".";
        int pulados = fila.size() - nomeados;
        if (nomeados > 0 && pulados > 0) {
            texto += "\n" + (pulados == 1 ? "1 ficou para depois." : ProdutoUtils.numero(pulados) + " ficaram para depois.");
        }
        resumo.setText(texto);
    }

    private void esconderTeclado() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(edtNome.getWindowToken(), 0);
    }
}
