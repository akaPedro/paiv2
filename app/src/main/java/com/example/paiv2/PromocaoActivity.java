package com.example.paiv2;

import android.content.Intent;
import android.content.res.ColorStateList;
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
import androidx.recyclerview.widget.LinearLayoutManager;
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
 * Produtos em promoção: a ordem manda, e os três primeiros aparecem na vitrine
 * da tela inicial.
 */
public class PromocaoActivity extends AppCompatActivity {

    private AppDatabase db;
    private final List<Produto> promocao = new ArrayList<>();
    private PromoAdapter adapter;

    private MaterialToolbar toolbar;
    private RecyclerView recycler;
    private View vazio;
    private ExtendedFloatingActionButton btnAdicionar;

    private final ActivityResultLauncher<Intent> escolherProdutos =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    resultado -> carregar());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Janela.telaCheia(this, true, false);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_promocao);

        db = AppDatabase.getInstance(this);

        Janela.recuos(findViewById(R.id.cabecalho), Janela.TOPO);
        toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        recycler = findViewById(R.id.recyclerPromocao);
        Janela.recuos(recycler, Janela.BASE);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PromoAdapter();
        recycler.setAdapter(adapter);

        vazio = findViewById(R.id.vazioPromocao);

        btnAdicionar = findViewById(R.id.btnAdicionarPromocao);
        Janela.recuos(btnAdicionar, Janela.MARGEM_BASE);
        btnAdicionar.setOnClickListener(v ->
                escolherProdutos.launch(new Intent(this, EscolherProdutosActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        carregar();
    }

    private void carregar() {
        new Thread(() -> {
            List<Produto> todos = db.produtoDao().listarTodos();
            List<Produto> escolhidos = Promocao.produtos(this, todos);

            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                promocao.clear();
                promocao.addAll(escolhidos);
                adapter.notifyDataSetChanged();

                toolbar.setSubtitle(ProdutoUtils.contagem(promocao.size(), "produto", "produtos"));
                vazio.setVisibility(promocao.isEmpty() ? View.VISIBLE : View.GONE);
                recycler.setVisibility(promocao.isEmpty() ? View.GONE : View.VISIBLE);
            });
        }).start();
    }

    private void remover(Produto produto, int posicao) {
        Promocao.remover(this, Collections.singletonList(produto.getId()));
        carregar();

        Snackbar aviso = Snackbar.make(findViewById(android.R.id.content),
                "Tirado da promoção", Snackbar.LENGTH_LONG);
        aviso.setAnchorView(btnAdicionar);
        aviso.setAction("Desfazer", v -> {
            Promocao.inserir(this, produto.getId(), posicao);
            carregar();
        });
        aviso.show();
    }

    private class PromoAdapter extends RecyclerView.Adapter<PromoAdapter.Holder> {

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_promocao, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            Produto produto = promocao.get(position);
            boolean naVitrine = position < Promocao.DESTAQUES;

            Glide.with(PromocaoActivity.this)
                    .load(ImagemUtils.modelo(produto.getImageUri()))
                    .override(200, 200)
                    .fitCenter()
                    .error(R.drawable.default_image)
                    .into(holder.foto);

            holder.nome.setText(produto.getNome());
            holder.categoria.setText(SeletorCategoria.rotulo(produto.getCategoria()));
            holder.categoria.setBackgroundTintList(ColorStateList.valueOf(
                    EstiloCategoria.cor(PromocaoActivity.this, produto.getCategoria())));

            holder.destaque.setVisibility(naVitrine ? View.VISIBLE : View.GONE);
            holder.destaque.setText("Na vitrine · " + (position + 1));

            holder.estrela.setImageResource(naVitrine ? R.drawable.ic_estrela : R.drawable.ic_estrela_vazia);
            holder.estrela.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(
                    PromocaoActivity.this, naVitrine ? R.color.vermelho_oferta : R.color.texto_secundario)));
            holder.estrela.setContentDescription(naVitrine ? "Tirar da vitrine" : "Colocar na vitrine");
            holder.estrela.setOnClickListener(v -> {
                if (naVitrine) {
                    Promocao.tirarDoDestaque(PromocaoActivity.this, produto.getId());
                } else {
                    Promocao.destacar(PromocaoActivity.this, produto.getId());
                }
                carregar();
            });

            holder.remover.setOnClickListener(v -> remover(produto, position));
            holder.itemView.setOnClickListener(v ->
                    ImagemProdutoActivity.abrir(PromocaoActivity.this, new ArrayList<>(promocao), produto));
        }

        @Override
        public int getItemCount() {
            return promocao.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            final ImageView foto;
            final TextView nome;
            final TextView categoria;
            final TextView destaque;
            final ImageButton estrela;
            final ImageButton remover;

            Holder(@NonNull View itemView) {
                super(itemView);
                foto = itemView.findViewById(R.id.promoFoto);
                nome = itemView.findViewById(R.id.promoNome);
                categoria = itemView.findViewById(R.id.promoCategoria);
                destaque = itemView.findViewById(R.id.promoDestaque);
                estrela = itemView.findViewById(R.id.promoEstrela);
                remover = itemView.findViewById(R.id.promoRemover);
            }
        }
    }
}
