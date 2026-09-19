package com.example.paiv2;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.paiv2.entity.Categoria;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * Lista de categorias com cor e ícone, usada para mover produtos.
 */
public final class SeletorCategoria {

    public interface AoEscolher {
        void escolhida(Categoria categoria);
    }

    // Ordem em que as categorias aparecem para o usuário
    private static final Categoria[] OPCOES = {
            Categoria.ALIMENTOS,
            Categoria.DOCES,
            Categoria.BEBIDAS,
            Categoria.HIGIENE,
            Categoria.OUTROS
    };

    private SeletorCategoria() {
    }

    /** Categorias na ordem em que devem aparecer para o usuário. */
    public static Categoria[] opcoes() {
        return OPCOES.clone();
    }

    /** Nome da categoria como o usuário lê: "Alimentos" em vez de "ALIMENTOS". */
    public static String rotulo(Categoria categoria) {
        if (categoria == null) return "";
        String nome = categoria.name();
        return nome.charAt(0) + nome.substring(1).toLowerCase();
    }

    public static void mostrar(Context context, String titulo, Categoria atual, AoEscolher aoEscolher) {
        BaseAdapter opcoes = new BaseAdapter() {
            @Override
            public int getCount() {
                return OPCOES.length;
            }

            @Override
            public Object getItem(int position) {
                return OPCOES[position];
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View linha = convertView != null ? convertView
                        : LayoutInflater.from(context).inflate(R.layout.item_opcao_categoria, parent, false);
                Categoria categoria = OPCOES[position];

                linha.findViewById(R.id.opcaoCirculo).setBackgroundTintList(
                        ColorStateList.valueOf(EstiloCategoria.cor(context, categoria)));
                ((ImageView) linha.findViewById(R.id.opcaoIcone)).setImageResource(EstiloCategoria.icone(categoria));
                ((TextView) linha.findViewById(R.id.opcaoNome)).setText(rotulo(categoria));
                linha.findViewById(R.id.opcaoAtual).setVisibility(categoria == atual ? View.VISIBLE : View.GONE);
                return linha;
            }
        };

        new MaterialAlertDialogBuilder(context)
                .setTitle(titulo)
                .setAdapter(opcoes, (dialog, qual) -> aoEscolher.escolhida(OPCOES[qual]))
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
