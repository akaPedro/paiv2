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
import com.bumptech.glide.Glide;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.paiv2.database.AppDatabase;
import com.example.paiv2.entity.Produto;

import java.util.ArrayList;
import java.util.List;

public class ProdutoAdapter extends RecyclerView.Adapter<ProdutoAdapter.ProdutoViewHolder> {

    private List<Produto> listaProdutos = new ArrayList<>();
    private Context context;




    public ProdutoAdapter(Context context, List<Produto> listaProdutos) {
        this.context = context;
        this.listaProdutos = listaProdutos;
    }

    @NonNull
    @Override
    public ProdutoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_produto, parent, false);
        return new ProdutoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProdutoViewHolder holder, int position) {
        Produto produto = listaProdutos.get(position);
        holder.txtNome.setText(produto.getNome());


        if (produto.getImageUri() != null && !produto.getImageUri().isEmpty()) {

            String caminhoCompleto = "file:///android_asset/" + produto.getImageUri();

            // O Glide carrega em background e não trava o scroll
            Glide.with(context)
                    .load("file:///android_asset/" + produto.getImageUri())
                    .into(holder.imgProduto);
        } else {
            holder.imgProduto.setImageResource(R.drawable.default_image);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ImagemProdutoActivity.class);
            intent.putExtra("imageUri", produto.getImageUri());
            context.startActivity(intent);
        });

//gptanca
        holder.btnMenu.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(context, holder.btnMenu);
            popup.inflate(R.menu.menu_produto);

            popup.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.action_delete) {
                    excluirProduto(produto);
                    return true;
                }

                if (item.getItemId() == R.id.action_edit) {
                    editarProduto(produto);
                    return true;
                }

                return false;
            });

            popup.show();
        });


    }

    @Override
    public int getItemCount() {
        return listaProdutos.size();
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

    private void excluirProduto(Produto produto) {
        AppDatabase db = AppDatabase.getInstance(context);

        new Thread(() -> {
            db.produtoDao().deletar(produto);

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

        builder.setPositiveButton("Salvar", (dialog, which) -> {
            String novoNome = input.getText().toString().trim();
            if (!novoNome.isEmpty()) {
                produto.setNome(novoNome);

                AppDatabase db = AppDatabase.getInstance(context);

                new Thread(() -> {
                    db.produtoDao().atualizar(produto);

                    ((Activity) context).runOnUiThread(this::notifyDataSetChanged);
                }).start();
            }
        });

        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }


}
