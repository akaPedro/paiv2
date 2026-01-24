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

        if (isAlcoolica(produto)) {
            // 🔴 alcoólicas
            holder.txtNome.setTextColor(
                    context.getResources().getColor(R.color.vermelho_oferta)
            );
        } else {
            // 🔵 não alcoólicas
            holder.txtNome.setTextColor(
                    context.getResources().getColor(R.color.azul_logo)
            );
        }


        if (produto.getId() == -1) {
            holder.txtNome.setText(produto.getNome());
            holder.txtNome.setTextColor(
                    context.getResources().getColor(produto.getNome().contains("NÃO")
                            ? android.R.color.holo_blue_dark
                            : android.R.color.holo_red_dark)
            );

            holder.imgProduto.setVisibility(View.GONE);
            holder.btnMenu.setVisibility(View.GONE);
            return;
        }

        holder.imgProduto.setVisibility(View.VISIBLE);
        holder.btnMenu.setVisibility(View.VISIBLE);

        // LIMPA imagem reciclada
        holder.imgProduto.setImageDrawable(null);

        String imageUri = produto.getImageUri();

        if (imageUri != null && !imageUri.isEmpty()) {

            if (imageUri.startsWith("content://") || imageUri.startsWith("file://")) {
                // 📷 Galeria / storage
                Glide.with(context)
                        .load(Uri.parse(imageUri))
                        .override(ViewGroup.LayoutParams.MATCH_PARENT, 120)
                        .fitCenter()                        .dontAnimate()
                        .placeholder(R.drawable.default_image)
                        .error(R.drawable.default_image)
                        .into(holder.imgProduto);

            } else {
                // 📦 Assets
                Glide.with(context)
                        .load("file:///android_asset/" + imageUri)
                        .override(ViewGroup.LayoutParams.MATCH_PARENT, 120)
                        .fitCenter()                        .dontAnimate()
                        .placeholder(R.drawable.default_image)
                        .error(R.drawable.default_image)
                        .into(holder.imgProduto);
            }

        } else {
            holder.imgProduto.setImageResource(R.drawable.default_image);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ImagemProdutoActivity.class);
            intent.putExtra("imageUri", imageUri);
            context.startActivity(intent);
        });

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

    public boolean isDivisor(int position) {
        Produto p = listaProdutos.get(position);
        return p.getId() == -1;
    }

    private boolean isAlcoolica(Produto p) {
        String nome = p.getNome().toLowerCase();

        return nome.contains("cerveja")
                || nome.contains("vinho")
                || nome.contains("vodka")
                || nome.contains("whisky")
                || nome.contains("cachaça")
                || nome.contains("rum")
                || nome.contains("caninha")
                || nome.contains("montila")
                || nome.contains("ice")
                || nome.contains("catuaba")
                || nome.contains("licor")
                || nome.contains("conhaque")
                || nome.contains("sidra")
                || nome.contains("aguardente")
                || nome.contains("raiz amarga")
                || nome.contains("corote")
                || nome.contains("gin");
    }

}
