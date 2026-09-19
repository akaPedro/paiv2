package com.example.paiv2;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.paiv2.database.AppDatabase;
import com.example.paiv2.entity.Categoria;
import com.example.paiv2.entity.Produto;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class AddProdutoActivity extends AppCompatActivity {

    // Largura final das fotos copiadas: leve para lotes grandes e nítida na tela
    private static final int LARGURA_FINAL = 800;

    private static final int[][] ESTADOS_BOTAO = {{-android.R.attr.state_enabled}, {}};

    private View cabecalho;
    private View areaFotos;
    private View fotosVazio;
    private View fotosEscolhidas;
    private TextView txtQuantidadeFotos;
    private TextInputLayout campoNome;
    private TextInputEditText edtNome;
    private ChipGroup chipsCategoria;
    private TextView txtProgresso;
    private LinearProgressIndicator progresso;
    private MaterialButton btnSalvar;

    private final List<Uri> imagensSelecionadas = new ArrayList<>();
    private RecyclerView.Adapter<RecyclerView.ViewHolder> miniaturas;

    private Categoria categoria;
    private int corAtual;
    private boolean salvando = false;

    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Janela.telaCheia(this, true, false);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_addproduto);

        // A tela de origem sugere a categoria, mas o usuário pode trocar antes de salvar
        String categoriaStr = getIntent().getStringExtra(CategoriaActivity.EXTRA_CATEGORIA);
        categoria = categoriaStr == null ? Categoria.OUTROS : Categoria.valueOf(categoriaStr);
        db = AppDatabase.getInstance(this);

        cabecalho = findViewById(R.id.cabecalho);
        areaFotos = findViewById(R.id.areaFotos);
        fotosVazio = findViewById(R.id.fotosVazio);
        fotosEscolhidas = findViewById(R.id.fotosEscolhidas);
        txtQuantidadeFotos = findViewById(R.id.txtQuantidadeFotos);
        campoNome = findViewById(R.id.inputLayoutNome);
        edtNome = findViewById(R.id.edtNomeProduto);
        chipsCategoria = findViewById(R.id.chipsCategoria);
        txtProgresso = findViewById(R.id.txtProgresso);
        progresso = findViewById(R.id.progressoSalvar);
        btnSalvar = findViewById(R.id.btnSalvarProduto);

        Janela.recuos(cabecalho, Janela.TOPO);
        Janela.recuos(findViewById(R.id.rodape), Janela.BASE | Janela.TECLADO);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        areaFotos.setOnClickListener(v -> abrirGaleria());
        configurarMiniaturas();

        ChipsCategoria.montar(chipsCategoria, categoria, escolhida -> {
            categoria = escolhida;
            aplicarCorDaCategoria(true);
            atualizarTextos();
        });

        edtNome.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                atualizarTextos();
            }
        });

        btnSalvar.setOnClickListener(v -> salvarProdutos());

        corAtual = EstiloCategoria.cor(this, categoria);
        aplicarCorDaCategoria(false);
        atualizarTextos();
    }

    private void configurarMiniaturas() {
        RecyclerView recyclerFotos = findViewById(R.id.recyclerFotos);
        recyclerFotos.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        miniaturas = new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View item = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_miniatura, parent, false);
                // As miniaturas cobrem a área de fotos: o toque nelas também troca as fotos
                item.setOnClickListener(v -> abrirGaleria());
                return new RecyclerView.ViewHolder(item) {
                };
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                Glide.with(AddProdutoActivity.this)
                        .load(imagensSelecionadas.get(position))
                        .override(240, 240)
                        .centerCrop()
                        .into((ImageView) holder.itemView);
            }

            @Override
            public int getItemCount() {
                return imagensSelecionadas.size();
            }
        };
        recyclerFotos.setAdapter(miniaturas);
    }

    /** O cabeçalho e o botão assumem a cor da categoria: deixa claro onde os produtos vão parar. */
    private void aplicarCorDaCategoria(boolean animar) {
        int novaCor = EstiloCategoria.cor(this, categoria);
        if (animar && novaCor != corAtual) {
            ValueAnimator animacao = ValueAnimator.ofArgb(corAtual, novaCor);
            animacao.setDuration(250);
            animacao.addUpdateListener(a -> pintar((int) a.getAnimatedValue()));
            animacao.start();
        } else {
            pintar(novaCor);
        }
        corAtual = novaCor;
    }

    private void pintar(int cor) {
        cabecalho.setBackgroundColor(cor);
        // Desabilitado ("Escolha as fotos primeiro") precisa continuar legível: fundo claro e
        // texto escuro, em vez do texto apagado do Material sobre a cor da categoria
        btnSalvar.setBackgroundTintList(new ColorStateList(ESTADOS_BOTAO, new int[]{
                ContextCompat.getColor(this, R.color.superficie_variante), cor}));
        btnSalvar.setTextColor(new ColorStateList(ESTADOS_BOTAO, new int[]{
                ContextCompat.getColor(this, R.color.texto_secundario),
                ContextCompat.getColor(this, R.color.branco)}));
        progresso.setIndicatorColor(cor);
    }

    private void atualizarTextos() {
        int fotos = imagensSelecionadas.size();
        String nome = textoDoNome();

        fotosVazio.setVisibility(fotos == 0 ? View.VISIBLE : View.GONE);
        fotosEscolhidas.setVisibility(fotos == 0 ? View.GONE : View.VISIBLE);
        txtQuantidadeFotos.setText(ProdutoUtils.contagem(fotos, "foto escolhida", "fotos escolhidas"));

        // Explica antes de salvar como os nomes vão ficar
        if (nome.isEmpty()) {
            campoNome.setHelperText("Pode deixar em branco e dar nome depois");
        } else if (fotos > 1) {
            campoNome.setHelperText("Cada foto vira um produto: “" + nome + " 1”, “" + nome + " 2”…");
        } else {
            campoNome.setHelperText(null);
        }

        if (salvando) return;
        btnSalvar.setEnabled(fotos > 0);
        if (fotos == 0) {
            btnSalvar.setText("Escolha as fotos primeiro");
        } else {
            String quantos = fotos == 1 ? "Salvar produto" : "Salvar " + ProdutoUtils.numero(fotos) + " produtos";
            btnSalvar.setText(quantos + " em " + SeletorCategoria.rotulo(categoria));
        }
    }

    private String textoDoNome() {
        return edtNome.getText() == null ? "" : edtNome.getText().toString().trim();
    }

    private void abrirGaleria() {
        if (salvando) return;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT); // OPEN_DOCUMENT é melhor para persistência
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        galeriaLauncher.launch(intent);
    }

    private final ActivityResultLauncher<Intent> galeriaLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != RESULT_OK || result.getData() == null) return;

                List<Uri> escolhidas = new ArrayList<>();
                Intent dados = result.getData();
                if (dados.getClipData() != null) {
                    for (int i = 0; i < dados.getClipData().getItemCount(); i++) {
                        escolhidas.add(dados.getClipData().getItemAt(i).getUri());
                    }
                } else if (dados.getData() != null) {
                    escolhidas.add(dados.getData());
                }
                if (escolhidas.isEmpty()) return;

                for (Uri uri : escolhidas) {
                    try {
                        getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } catch (SecurityException ignorada) {
                        // Alguns apps de galeria não permitem guardar o acesso; a cópia é feita já ao salvar
                    }
                }

                imagensSelecionadas.clear();
                imagensSelecionadas.addAll(escolhidas);
                miniaturas.notifyDataSetChanged();
                atualizarTextos();
            });

    private void salvarProdutos() {
        if (salvando || imagensSelecionadas.isEmpty()) return;
        salvando = true;
        esconderTeclado();

        final String nomeDigitado = textoDoNome();
        // Congela as escolhas no momento do toque
        final Categoria categoriaEscolhida = categoria;
        final List<Uri> fotos = new ArrayList<>(imagensSelecionadas);
        final int total = fotos.size();

        travarFormulario(true);
        btnSalvar.setText("Salvando…");
        progresso.setMax(total);
        progresso.setProgressCompat(0, false);
        progresso.setVisibility(View.VISIBLE);
        txtProgresso.setVisibility(View.VISIBLE);

        new Thread(() -> {
            try {
                int salvos = 0;
                for (int i = 0; i < total; i++) {
                    final int numero = i + 1;
                    runOnUiThread(() -> txtProgresso.setText(
                            "Salvando " + ProdutoUtils.numero(numero) + " de " + ProdutoUtils.numero(total) + "…"));

                    String caminho = copiarParaInterno(fotos.get(i), numero);
                    if (caminho != null) {
                        String nome = nomeDoProduto(nomeDigitado, categoriaEscolhida, numero, total);
                        db.produtoDao().inserir(new Produto(nome, caminho, categoriaEscolhida));
                        salvos++;
                    }

                    runOnUiThread(() -> progresso.setProgressCompat(numero, true));
                }

                final int ok = salvos;
                final int falhas = total - salvos;
                runOnUiThread(() -> {
                    String mensagem = ProdutoUtils.contagem(ok, "produto adicionado", "produtos adicionados");
                    if (falhas > 0) {
                        mensagem += ". " + ProdutoUtils.contagem(falhas,
                                "foto não pôde ser lida", "fotos não puderam ser lidas");
                    }
                    Toast.makeText(this, mensagem, Toast.LENGTH_LONG).show();
                    finish();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    salvando = false;
                    travarFormulario(false);
                    progresso.setVisibility(View.GONE);
                    txtProgresso.setVisibility(View.GONE);
                    atualizarTextos();
                    Toast.makeText(this, "Não foi possível salvar. Tente de novo.", Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    /**
     * Uma foto com nome fica com o nome exato; várias recebem número. Sem nome, o
     * produto fica com nome provisório e aparece em "Nomear produtos".
     */
    private static String nomeDoProduto(String nomeDigitado, Categoria categoria, int numero, int total) {
        if (nomeDigitado.isEmpty()) return categoria.name() + " " + numero;
        if (total == 1) return nomeDigitado;
        return nomeDigitado + " " + numero;
    }

    private void travarFormulario(boolean travar) {
        areaFotos.setEnabled(!travar);
        campoNome.setEnabled(!travar);
        for (int i = 0; i < chipsCategoria.getChildCount(); i++) {
            chipsCategoria.getChildAt(i).setEnabled(!travar);
        }
        btnSalvar.setEnabled(!travar);
    }

    private void esconderTeclado() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(edtNome.getWindowToken(), 0);
    }

    private String copiarParaInterno(Uri uri, int indice) {
        Bitmap bitmap = null;
        Bitmap redimensionado = null;
        try {
            // Primeiro só as dimensões: uma foto de 12 MP inteira ocuparia 48 MB de memória
            BitmapFactory.Options opcoes = new BitmapFactory.Options();
            opcoes.inJustDecodeBounds = true;
            try (InputStream is = getContentResolver().openInputStream(uri)) {
                BitmapFactory.decodeStream(is, null, opcoes);
            }
            // Imagem corrompida ou formato não suportado
            if (opcoes.outWidth <= 0 || opcoes.outHeight <= 0) return null;

            // Lê já reduzida, mas nunca abaixo da largura final
            opcoes.inSampleSize = 1;
            while (opcoes.outWidth / (opcoes.inSampleSize * 2) >= LARGURA_FINAL) {
                opcoes.inSampleSize *= 2;
            }
            opcoes.inJustDecodeBounds = false;
            try (InputStream is = getContentResolver().openInputStream(uri)) {
                bitmap = BitmapFactory.decodeStream(is, null, opcoes);
            }
            if (bitmap == null) return null;

            float aspect = (float) bitmap.getWidth() / bitmap.getHeight();
            redimensionado = Bitmap.createScaledBitmap(bitmap, LARGURA_FINAL, Math.round(LARGURA_FINAL / aspect), true);

            String nomeArquivo = "img_" + System.currentTimeMillis() + "_" + indice + ".jpg";
            File arquivoDestino = new File(getFilesDir(), nomeArquivo);
            try (FileOutputStream os = new FileOutputStream(arquivoDestino)) {
                redimensionado.compress(Bitmap.CompressFormat.JPEG, 80, os); // 80% é mais leve para lotes grandes
            }

            return arquivoDestino.getAbsolutePath();
        } catch (Exception e) {
            return null;
        } finally {
            // Libera a memória a cada foto: é o que mantém lotes grandes sem travar
            if (redimensionado != null && redimensionado != bitmap) redimensionado.recycle();
            if (bitmap != null) bitmap.recycle();
        }
    }
}
