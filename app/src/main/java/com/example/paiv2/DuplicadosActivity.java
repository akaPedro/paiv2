package com.example.paiv2;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.paiv2.database.AppDatabase;
import com.example.paiv2.entity.Produto;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

/**
 * Mostra os produtos repetidos em grupos e deixa a pessoa decidir: escolher qual
 * cópia fica, excluir as outras, ou manter tudo como está.
 */
public class DuplicadosActivity extends AppCompatActivity {

    /** Um grupo de repetidos e qual das cópias está marcada para ficar. */
    private static final class GrupoUi {
        final DetectorDuplicados.Grupo grupo;
        int manter = 0;

        GrupoUi(DetectorDuplicados.Grupo grupo) {
            this.grupo = grupo;
        }
    }

    private AppDatabase db;
    private final List<GrupoUi> grupos = new ArrayList<>();
    private GruposAdapter adapter;

    private MaterialToolbar toolbar;
    private RecyclerView recycler;
    private View carregando;
    private View vazio;
    private View rodape;
    private MaterialButton btnExcluirTodos;

    public static void abrir(Context context) {
        context.startActivity(new Intent(context, DuplicadosActivity.class));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Janela.telaCheia(this, true, false);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_duplicados);

        db = AppDatabase.getInstance(this);

        Janela.recuos(findViewById(R.id.cabecalho), Janela.TOPO);
        Janela.recuos(findViewById(R.id.rodapeDuplicados), Janela.BASE);

        toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        recycler = findViewById(R.id.recyclerDuplicados);
        Janela.recuos(recycler, Janela.BASE);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new GruposAdapter();
        recycler.setAdapter(adapter);

        carregando = findViewById(R.id.carregandoDuplicados);
        vazio = findViewById(R.id.vazioDuplicados);
        rodape = findViewById(R.id.rodapeDuplicados);
        btnExcluirTodos = findViewById(R.id.btnExcluirTodos);
        btnExcluirTodos.setOnClickListener(v -> confirmarExclusaoGeral());

        procurar();
    }

    private void procurar() {
        carregando.setVisibility(View.VISIBLE);
        recycler.setVisibility(View.GONE);
        vazio.setVisibility(View.GONE);
        rodape.setVisibility(View.GONE);
        toolbar.setSubtitle(null);

        DetectorDuplicados.procurar(this, db, encontrados -> {
            if (isFinishing() || isDestroyed()) return;
            grupos.clear();
            for (DetectorDuplicados.Grupo g : encontrados) grupos.add(new GrupoUi(g));
            adapter.notifyDataSetChanged();
            carregando.setVisibility(View.GONE);
            atualizarResumo();
        });
    }

    private void atualizarResumo() {
        boolean temGrupos = !grupos.isEmpty();
        recycler.setVisibility(temGrupos ? View.VISIBLE : View.GONE);
        vazio.setVisibility(temGrupos ? View.GONE : View.VISIBLE);
        rodape.setVisibility(temGrupos ? View.VISIBLE : View.GONE);

        if (!temGrupos) {
            toolbar.setSubtitle(null);
            return;
        }
        toolbar.setSubtitle(ProdutoUtils.contagem(grupos.size(), "grupo", "grupos")
                + " · " + ProdutoUtils.contagem(totalCopias(), "cópia a mais", "cópias a mais"));
        btnExcluirTodos.setText("Excluir as " + ProdutoUtils.numero(totalCopias()) + " cópias de uma vez");
    }

    /** Quantos produtos seriam excluídos mantendo um de cada grupo. */
    private int totalCopias() {
        int total = 0;
        for (GrupoUi g : grupos) total += g.grupo.produtos.size() - 1;
        return total;
    }

    private void excluirOutros(GrupoUi grupoUi) {
        List<Produto> excluir = new ArrayList<>();
        for (int i = 0; i < grupoUi.grupo.produtos.size(); i++) {
            if (i != grupoUi.manter) excluir.add(grupoUi.grupo.produtos.get(i));
        }
        final int quantos = excluir.size();

        AcoesProduto.excluir(this, excluir, () -> {
            if (isFinishing() || isDestroyed()) return;
            int posicao = grupos.indexOf(grupoUi);
            if (posicao >= 0) {
                grupos.remove(posicao);
                adapter.notifyItemRemoved(posicao);
            }
            atualizarResumo();
            avisar(ProdutoUtils.contagem(quantos, "cópia excluída", "cópias excluídas"));
        });
    }

    private void confirmarExclusaoGeral() {
        final int quantos = totalCopias();
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Excluir " + ProdutoUtils.numero(quantos) + " cópias?")
                .setMessage("De cada grupo fica o produto que está marcado. Não dá para desfazer depois.")
                .setPositiveButton("Excluir", (d, w) -> {
                    List<Produto> excluir = new ArrayList<>();
                    for (GrupoUi g : grupos) {
                        for (int i = 0; i < g.grupo.produtos.size(); i++) {
                            if (i != g.manter) excluir.add(g.grupo.produtos.get(i));
                        }
                    }
                    AcoesProduto.excluir(this, excluir, () -> {
                        if (isFinishing() || isDestroyed()) return;
                        grupos.clear();
                        adapter.notifyDataSetChanged();
                        atualizarResumo();
                        avisar(ProdutoUtils.contagem(quantos, "cópia excluída", "cópias excluídas"));
                    });
                })
                .setNegativeButton("Cancelar", null)
                .show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(ContextCompat.getColor(this, R.color.vermelho_oferta));
    }

    private void manterTodos(GrupoUi grupoUi) {
        int posicao = grupos.indexOf(grupoUi);
        if (posicao < 0) return;
        grupos.remove(posicao);
        adapter.notifyItemRemoved(posicao);
        atualizarResumo();
    }

    private void avisar(String texto) {
        Snackbar.make(findViewById(android.R.id.content), texto, Snackbar.LENGTH_LONG).show();
    }

    private class GruposAdapter extends RecyclerView.Adapter<GruposAdapter.Holder> {

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_grupo_duplicado, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            GrupoUi grupoUi = grupos.get(position);
            List<Produto> produtos = grupoUi.grupo.produtos;

            holder.motivo.setText(grupoUi.grupo.motivo());
            holder.quantidade.setText(ProdutoUtils.contagem(produtos.size(), "cópia", "cópias"));

            holder.itens.removeAllViews();
            for (int i = 0; i < produtos.size(); i++) {
                holder.itens.addView(criarCopia(holder, grupoUi, produtos.get(i), i));
            }

            holder.btnExcluirOutros.setText("Excluir "
                    + ProdutoUtils.contagem(produtos.size() - 1, "cópia", "cópias"));
            holder.btnExcluirOutros.setOnClickListener(v -> excluirOutros(grupoUi));
            holder.btnManterTodos.setOnClickListener(v -> manterTodos(grupoUi));
        }

        private View criarCopia(Holder holder, GrupoUi grupoUi, Produto produto, int indice) {
            View copia = LayoutInflater.from(DuplicadosActivity.this)
                    .inflate(R.layout.item_duplicado, holder.itens, false);

            int largura = Math.round(104 * getResources().getDisplayMetrics().density);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(largura,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMarginEnd(Math.round(8 * getResources().getDisplayMetrics().density));
            copia.setLayoutParams(params);

            MaterialCardView card = copia.findViewById(R.id.dupCard);
            card.setCheckedIconTint(null);
            card.setChecked(indice == grupoUi.manter);

            Glide.with(DuplicadosActivity.this)
                    .load(ImagemUtils.modelo(produto.getImageUri()))
                    .override(260, 260)
                    .fitCenter()
                    .error(R.drawable.default_image)
                    .into((ImageView) copia.findViewById(R.id.dupFoto));

            ((TextView) copia.findViewById(R.id.dupNome)).setText(produto.getNome());
            TextView categoria = copia.findViewById(R.id.dupCategoria);
            categoria.setText(SeletorCategoria.rotulo(produto.getCategoria()));
            categoria.setTextColor(EstiloCategoria.cor(DuplicadosActivity.this, produto.getCategoria()));
            copia.findViewById(R.id.dupManter).setVisibility(
                    indice == grupoUi.manter ? View.VISIBLE : View.INVISIBLE);

            card.setOnClickListener(v -> {
                grupoUi.manter = indice;
                int posicao = grupos.indexOf(grupoUi);
                if (posicao >= 0) notifyItemChanged(posicao);
            });
            return copia;
        }

        @Override
        public int getItemCount() {
            return grupos.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            final TextView motivo;
            final TextView quantidade;
            final LinearLayout itens;
            final MaterialButton btnExcluirOutros;
            final MaterialButton btnManterTodos;

            Holder(@NonNull View itemView) {
                super(itemView);
                motivo = itemView.findViewById(R.id.grupoMotivo);
                quantidade = itemView.findViewById(R.id.grupoQuantidade);
                itens = itemView.findViewById(R.id.grupoItens);
                btnExcluirOutros = itemView.findViewById(R.id.btnExcluirOutros);
                btnManterTodos = itemView.findViewById(R.id.btnManterTodos);
            }
        }
    }
}
