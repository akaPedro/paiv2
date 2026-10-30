package com.example.paiv2;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.paiv2.database.AppDatabase;
import com.example.paiv2.entity.Produto;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Lista de uma vitrine (promoção ou destaques): a ordem manda, e os três
 * primeiros aparecem na tela inicial.
 */
public class VitrineActivity extends AppCompatActivity {

    private static final int COLUNAS = 3;

    private Vitrine vitrine;
    private AppDatabase db;
    private final List<Produto> escolhidos = new ArrayList<>();
    private VitrineAdapter adapter;

    private MaterialToolbar toolbar;
    private RecyclerView recycler;
    private View vazio;
    private ExtendedFloatingActionButton btnAdicionar;

    private final ActivityResultLauncher<Intent> escolherProdutos =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    resultado -> carregar());

    public static void abrir(Context contexto, Vitrine vitrine) {
        Intent intent = new Intent(contexto, VitrineActivity.class);
        vitrine.guardar(intent);
        contexto.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Janela.telaCheia(this, true, false);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vitrine);

        vitrine = Vitrine.recebida(getIntent());
        db = AppDatabase.getInstance(this);
        pintar();

        Janela.recuos(findViewById(R.id.cabecalho), Janela.TOPO);
        toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        recycler = findViewById(R.id.recyclerVitrine);
        Janela.recuos(recycler, Janela.BASE);
        recycler.setLayoutManager(new GridLayoutManager(this, COLUNAS));
        adapter = new VitrineAdapter();
        recycler.setAdapter(adapter);

        vazio = findViewById(R.id.vazioVitrine);

        btnAdicionar = findViewById(R.id.btnAdicionar);
        Janela.recuos(btnAdicionar, Janela.MARGEM_BASE);
        btnAdicionar.setOnClickListener(v -> {
            Intent intent = new Intent(this, EscolherProdutosActivity.class);
            vitrine.guardar(intent);
            escolherProdutos.launch(intent);
        });
    }

    /** Título, cor e textos da vitrine que esta tela está mostrando. */
    private void pintar() {
        int cor = ContextCompat.getColor(this, vitrine.cor);
        findViewById(R.id.cabecalho).setBackgroundColor(cor);
        ((MaterialToolbar) findViewById(R.id.toolbar)).setTitle(vitrine.titulo);

        ExtendedFloatingActionButton fab = findViewById(R.id.btnAdicionar);
        fab.setBackgroundTintList(ColorStateList.valueOf(cor));
        fab.setContentDescription("Adicionar produtos " + vitrine.onde);

        findViewById(R.id.vazioCirculo).setBackgroundTintList(
                ColorStateList.valueOf(ContextCompat.getColor(this, vitrine.corClara)));
        ImageView icone = findViewById(R.id.vazioIcone);
        icone.setImageResource(vitrine.icone);
        icone.setImageTintList(ColorStateList.valueOf(cor));
        ((TextView) findViewById(R.id.vazioTitulo)).setText("Nenhum produto " + vitrine.onde);
        ((TextView) findViewById(R.id.vazioDica)).setText(
                "Toque em Adicionar produtos e marque o que está " + vitrine.onde + ".");
    }

    @Override
    protected void onResume() {
        super.onResume();
        carregar();
    }

    private void carregar() {
        new Thread(() -> {
            List<Produto> todos = db.produtoDao().listarTodos();
            List<Produto> daVitrine = vitrine.produtos(this, todos);

            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                escolhidos.clear();
                escolhidos.addAll(daVitrine);
                adapter.notifyDataSetChanged();

                toolbar.setSubtitle(ProdutoUtils.contagem(escolhidos.size(), "produto", "produtos"));
                vazio.setVisibility(escolhidos.isEmpty() ? View.VISIBLE : View.GONE);
                recycler.setVisibility(escolhidos.isEmpty() ? View.GONE : View.VISIBLE);
            });
        }).start();
    }

    private void remover(Produto produto, int posicao) {
        vitrine.remover(this, Collections.singletonList(produto.getId()));
        carregar();

        Snackbar aviso = Snackbar.make(findViewById(android.R.id.content),
                "Tirado " + vitrine.deOnde, Snackbar.LENGTH_LONG);
        aviso.setAnchorView(btnAdicionar);
        aviso.setAction("Desfazer", v -> {
            vitrine.inserir(this, produto.getId(), posicao);
            carregar();
        });
        aviso.show();
    }

    private class VitrineAdapter extends RecyclerView.Adapter<VitrineAdapter.Holder> {

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_vitrine, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            Produto produto = escolhidos.get(position);
            boolean naVitrine = position < Vitrine.VAGAS;

            Glide.with(VitrineActivity.this)
                    .load(ImagemUtils.modelo(produto.getImageUri()))
                    .override(320, 320)
                    .fitCenter()
                    .error(R.drawable.default_image)
                    .into(holder.foto);

            boolean semNome = ProdutoUtils.semNome(produto);
            holder.nome.setText(produto.getNome());
            // Nome provisório fica apagado, igual ao card da categoria
            holder.nome.setTextColor(ContextCompat.getColor(VitrineActivity.this,
                    semNome ? R.color.texto_secundario : R.color.texto_primario));
            holder.nome.setTypeface(null, semNome ? Typeface.NORMAL : Typeface.BOLD);
            holder.selo18.setVisibility(
                    ProdutoUtils.isAlcoolica(produto) ? View.VISIBLE : View.GONE);

            holder.numero.setVisibility(naVitrine ? View.VISIBLE : View.GONE);
            holder.numero.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(VitrineActivity.this, vitrine.cor)));
            holder.numero.setText(String.valueOf(position + 1));

            holder.estrela.setImageResource(naVitrine ? R.drawable.ic_estrela : R.drawable.ic_estrela_vazia);
            holder.estrela.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(
                    VitrineActivity.this, naVitrine ? vitrine.cor : R.color.texto_secundario)));
            holder.estrela.setContentDescription(naVitrine ? "Tirar da vitrine" : "Colocar na vitrine");
            holder.estrela.setOnClickListener(v -> {
                if (naVitrine) {
                    vitrine.mandarParaOFim(VitrineActivity.this, produto.getId());
                } else {
                    vitrine.porNaFrente(VitrineActivity.this, produto.getId());
                }
                carregar();
            });

            holder.remover.setContentDescription("Tirar " + vitrine.deOnde);
            holder.remover.setOnClickListener(v -> remover(produto, position));
            holder.itemView.setOnClickListener(v ->
                    ImagemProdutoActivity.abrir(VitrineActivity.this, new ArrayList<>(escolhidos), produto));
        }

        @Override
        public int getItemCount() {
            return escolhidos.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            final ImageView foto;
            final TextView nome;
            final TextView selo18;
            final TextView numero;
            final ImageButton estrela;
            final ImageButton remover;

            Holder(@NonNull View itemView) {
                super(itemView);
                foto = itemView.findViewById(R.id.imgProduto);
                nome = itemView.findViewById(R.id.txtNomeProduto);
                selo18 = itemView.findViewById(R.id.selo18);
                numero = itemView.findViewById(R.id.vitrineNumero);
                estrela = itemView.findViewById(R.id.vitrineEstrela);
                remover = itemView.findViewById(R.id.vitrineRemover);
            }
        }
    }
}
