package com.example.paiv2;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.paiv2.database.AppDatabase;
import com.example.paiv2.entity.Categoria;
import com.example.paiv2.entity.Produto;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ProdutoAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TIPO_PRODUTO = 0;
    private static final int TIPO_DIVISOR = 1;

    /** Avisa a tela que os produtos mudaram e a lista precisa ser recarregada. */
    public interface AoAlterarProdutos {
        void aoAlterar();
    }

    private final List<Produto> listaProdutos;
    private final Context context;
    private final Handler main = new Handler(Looper.getMainLooper());
    private AoAlterarProdutos ouvinte;

    public ProdutoAdapter(Context context, List<Produto> listaProdutos) {
        this.context = context;
        this.listaProdutos = new ArrayList<>(listaProdutos);
    }

    public void setAoAlterarProdutos(AoAlterarProdutos ouvinte) {
        this.ouvinte = ouvinte;
    }

    @Override
    public int getItemViewType(int position) {
        return ProdutoUtils.isDivisor(listaProdutos.get(position)) ? TIPO_DIVISOR : TIPO_PRODUTO;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        if (viewType == TIPO_DIVISOR) {
            return new DivisorViewHolder(inflater.inflate(R.layout.item_divisor, parent, false));
        }
        return new ProdutoViewHolder(inflater.inflate(R.layout.item_produto, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Produto produto = listaProdutos.get(position);

        if (holder instanceof DivisorViewHolder) {
            ligarDivisor((DivisorViewHolder) holder, produto);
        } else {
            ligarProduto((ProdutoViewHolder) holder, produto);
        }
    }

    private void ligarDivisor(DivisorViewHolder holder, Produto divisor) {
        String titulo = divisor.getNome();
        // Bebida alcoólica continua em vermelho como aviso; os demais títulos em azul
        int cor = ContextCompat.getColor(context,
                "ALCOÓLICAS".equals(titulo) ? R.color.vermelho_oferta : R.color.azul_logo);

        holder.txtTitulo.setText(titulo);
        holder.txtTitulo.setTextColor(cor);
        holder.linha.setBackgroundColor(cor);
    }

    private void ligarProduto(ProdutoViewHolder holder, Produto produto) {
        holder.txtNome.setText(produto.getNome());
        holder.txtNome.setTextColor(ContextCompat.getColor(context,
                ProdutoUtils.isAlcoolica(produto) ? R.color.vermelho_oferta : R.color.azul_logo
        ));

        carregarImagem(holder.imgProduto, produto.getImageUri());

        // Toque abre a imagem grande
        holder.itemView.setOnClickListener(v -> {
            String path = produto.getImageUri();
            if (path == null || path.isEmpty()) return;

            Intent intent = new Intent(context, ImagemProdutoActivity.class);
            intent.putExtra("imageUri", path);
            context.startActivity(intent);
        });

        // Apertar e segurar abre o menu de editar/excluir
        holder.itemView.setOnLongClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            mostrarMenu(v, produto);
            return true;
        });
    }

    private void carregarImagem(ImageView alvo, String uriString) {
        Glide.with(context).clear(alvo);

        if (uriString == null || uriString.isEmpty()) {
            alvo.setImageResource(R.drawable.default_image);
            return;
        }

        Object model;
        if (uriString.startsWith("/")) {
            // Caminho Interno (Cópia da Galeria) - PRIORIDADE
            model = new File(uriString);
        } else if (uriString.startsWith("content://") || uriString.startsWith("file://")) {
            // URI Direta da Galeria (Legado)
            model = Uri.parse(uriString);
        } else {
            // Assets (Caminho relativo)
            model = "file:///android_asset/" + uriString;
        }

        Glide.with(context)
                .load(model)
                .override(320, 320)
                .fitCenter()
                .thumbnail(0.1f)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.default_image)
                .error(R.drawable.default_image)
                .into(alvo);
    }

    @Override
    public int getItemCount() {
        return listaProdutos.size();
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);
        if (holder instanceof ProdutoViewHolder) {
            Glide.with(context).clear(((ProdutoViewHolder) holder).imgProduto);
        }
    }

    public void atualizarLista(List<Produto> novaLista) {
        listaProdutos.clear();
        listaProdutos.addAll(novaLista);
        notifyDataSetChanged();
    }

    public boolean isDivisor(int position) {
        if (position >= 0 && position < listaProdutos.size()) {
            return ProdutoUtils.isDivisor(listaProdutos.get(position));
        }
        return false;
    }

    static class ProdutoViewHolder extends RecyclerView.ViewHolder {
        final ImageView imgProduto;
        final TextView txtNome;

        ProdutoViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProduto = itemView.findViewById(R.id.imgProduto);
            txtNome = itemView.findViewById(R.id.txtNomeProduto);
        }
    }

    static class DivisorViewHolder extends RecyclerView.ViewHolder {
        final TextView txtTitulo;
        final View linha;

        DivisorViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTitulo = itemView.findViewById(R.id.txtTituloSecao);
            linha = itemView.findViewById(R.id.linhaSecao);
        }
    }

    // --- MENU E EDIÇÃO ---

    private void mostrarMenu(View anchor, Produto produto) {
        PopupMenu popup = new PopupMenu(context, anchor);
        popup.inflate(R.menu.menu_produto);
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_delete) {
                confirmarExclusao(produto);
                return true;
            } else if (id == R.id.action_edit) {
                editarProduto(produto);
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void confirmarExclusao(Produto produto) {
        new AlertDialog.Builder(context)
                .setTitle("Excluir produto")
                .setMessage("Excluir \"" + produto.getNome() + "\"?")
                .setPositiveButton("Excluir", (d, w) -> excluirProduto(produto))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void excluirProduto(Produto produto) {
        AppDatabase db = AppDatabase.getInstance(context);
        new Thread(() -> {
            db.produtoDao().deletar(produto);
            // Se for um arquivo interno, deleta o arquivo físico também para não lotar o celular
            if (produto.getImageUri() != null && produto.getImageUri().startsWith("/")) {
                new File(produto.getImageUri()).delete();
            }
            main.post(() -> avisarExclusao(produto));
        }).start();
    }

    private void editarProduto(Produto produto) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_editar_produto, null);
        TextInputEditText edtNome = view.findViewById(R.id.edtNomeEditar);
        MaterialButton btnCategoria = view.findViewById(R.id.btnCategoriaEditar);

        edtNome.setText(produto.getNome());
        edtNome.setSelection(edtNome.getText() == null ? 0 : edtNome.getText().length());

        // Guarda a escolha até o usuário salvar, para "Cancelar" não mudar nada
        final Categoria[] categoriaEscolhida = {produto.getCategoria()};
        btnCategoria.setText(SeletorCategoria.rotulo(categoriaEscolhida[0]));
        btnCategoria.setOnClickListener(v ->
                SeletorCategoria.mostrar(context, categoriaEscolhida[0], escolhida -> {
                    categoriaEscolhida[0] = escolhida;
                    btnCategoria.setText(SeletorCategoria.rotulo(escolhida));
                }));

        new AlertDialog.Builder(context)
                .setTitle("Editar produto")
                .setView(view)
                .setPositiveButton("Salvar", (dialog, which) -> {
                    String novoNome = edtNome.getText() == null ? "" : edtNome.getText().toString().trim();
                    if (novoNome.isEmpty()) return;

                    produto.setNome(novoNome);
                    produto.setCategoria(categoriaEscolhida[0]);
                    new Thread(() -> {
                        AppDatabase.getInstance(context).produtoDao().atualizar(produto);
                        main.post(this::avisarEdicao);
                    }).start();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // A tela recarrega a lista do banco. Os tratamentos locais abaixo só entram
    // em ação se nenhuma tela tiver registrado um ouvinte.

    private void avisarExclusao(Produto produto) {
        if (ouvinte != null) {
            ouvinte.aoAlterar();
            return;
        }
        int pos = listaProdutos.indexOf(produto);
        if (pos >= 0) {
            listaProdutos.remove(pos);
            notifyItemRemoved(pos);
        }
    }

    private void avisarEdicao() {
        if (ouvinte != null) {
            ouvinte.aoAlterar();
        } else {
            notifyDataSetChanged();
        }
    }
}
