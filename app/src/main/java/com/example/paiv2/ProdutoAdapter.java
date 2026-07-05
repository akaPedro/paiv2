package com.example.paiv2;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.paiv2.database.AppDatabase;
import com.example.paiv2.entity.Produto;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ProdutoAdapter extends RecyclerView.Adapter<ProdutoAdapter.ProdutoViewHolder> {

    private List<Produto> listaProdutos = new ArrayList<>();
    private final Context context;


    public ProdutoAdapter(Context context, List<Produto> listaProdutos) {
        this.context = context;
        this.listaProdutos = listaProdutos;
    }

    @NonNull
    @Override
    public ProdutoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_produto, parent, false);
        return new ProdutoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProdutoViewHolder holder, int position) {
        final Produto produto = listaProdutos.get(position);

        // 1. Tratamento de Divisores
        if (ProdutoUtils.isDivisor(produto)) {
            holder.txtNome.setText(produto.getNome());
            holder.txtNome.setTextColor(ContextCompat.getColor(context,
                    produto.getNome().contains("NÃO") ? android.R.color.holo_blue_dark : android.R.color.holo_red_dark
            ));
            holder.imgProduto.setVisibility(View.GONE);
            holder.btnMenu.setVisibility(View.GONE);
            holder.itemView.setOnClickListener(null);
            return;
        }

        // 2. Configuração Básica
        holder.imgProduto.setVisibility(View.VISIBLE);
        holder.btnMenu.setVisibility(View.VISIBLE);
        holder.txtNome.setText(produto.getNome());
        holder.txtNome.setTextColor(ContextCompat.getColor(context,
                ProdutoUtils.isAlcoolica(produto) ? R.color.vermelho_oferta : R.color.azul_logo
        ));

        // 3. Carregamento de Imagem Otimizado
        Glide.with(context).clear(holder.imgProduto);

        String uriString = produto.getImageUri();
        if (uriString != null && !uriString.isEmpty()) {
            Object model;

            // Identifica a origem do arquivo para o Glide
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
                    .into(holder.imgProduto);
        } else {
            holder.imgProduto.setImageResource(R.drawable.default_image);
        }

        // 4. Clique para abrir imagem grande
        holder.itemView.setOnClickListener(v -> {
            String path = produto.getImageUri();
            if (path == null || path.isEmpty()) return;

            Intent intent = new Intent(context, ImagemProdutoActivity.class);
            intent.putExtra("imageUri", path);
            context.startActivity(intent);
        });

        holder.btnMenu.setOnClickListener(v -> mostrarMenu(holder.btnMenu, produto));
    }

    @Override
    public int getItemCount() { return listaProdutos.size(); }

    @Override
    public void onViewRecycled(@NonNull ProdutoViewHolder holder) {
        super.onViewRecycled(holder);
        Glide.with(context).clear(holder.imgProduto);
    }

    public void atualizarLista(List<Produto> novaLista) {
        listaProdutos.clear();
        listaProdutos.addAll(novaLista);
        notifyDataSetChanged();
    }

    static class ProdutoViewHolder extends RecyclerView.ViewHolder {
        ImageView imgProduto;
        TextView txtNome;
        ImageButton btnMenu;

        public ProdutoViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProduto = itemView.findViewById(R.id.imgProduto);
            txtNome = itemView.findViewById(R.id.txtNomeProduto);
            btnMenu = itemView.findViewById(R.id.btnMenu);
        }
    }

    // --- MÉTODOS DE APOIO ---

    private void excluirProduto(Produto produto) {
        AppDatabase db = AppDatabase.getInstance(context);
        new Thread(() -> {
            db.produtoDao().deletar(produto);
            // Se for um arquivo interno, deleta o arquivo físico também para não lotar o celular
            if (produto.getImageUri() != null && produto.getImageUri().startsWith("/")) {
                new File(produto.getImageUri()).delete();
            }
            ((Activity) context).runOnUiThread(() -> {
                listaProdutos.remove(produto);
                notifyDataSetChanged();
            });
        }).start();
    }

    private void editarProduto(Produto produto) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Editar nome");
        final EditText input = new EditText(context);
        input.setText(produto.getNome());
        builder.setView(input);

        // Faz com que o texto seja selecionado assim que ganhar foco
        input.setSelectAllOnFocus(true);
        input.requestFocus();
        // Seleciona tudo explicitamente do início ao fim
        input.setSelection(0, input.getText().length());

        builder.setPositiveButton("Salvar", (dialog, which) -> {
            String novoNome = input.getText().toString().trim();
            if (!novoNome.isEmpty()) {
                produto.setNome(novoNome);
                new Thread(() -> {
                    AppDatabase.getInstance(context).produtoDao().atualizar(produto);
                    ((Activity) context).runOnUiThread(this::notifyDataSetChanged);
                }).start();
            }
        });
        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void mostrarMenu(View anchor, Produto produto) {
        PopupMenu popup = new PopupMenu(context, anchor);
        popup.inflate(R.menu.menu_produto);
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_delete) {
                excluirProduto(produto);
                return true;
            } else if (id == R.id.action_edit) {
                editarProduto(produto);
                return true;
            }
            return false;
        });
        popup.show();
    }

    public boolean isDivisor(int position) {
        if (position >= 0 && position < listaProdutos.size()) {
            return ProdutoUtils.isDivisor(listaProdutos.get(position));
        }
        return false;
    }

}