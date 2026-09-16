package com.example.paiv2;

import android.content.Context;

import androidx.appcompat.app.AlertDialog;

import com.example.paiv2.entity.Categoria;

/**
 * Escolha de categoria em lista, usada tanto ao criar quanto ao editar um produto.
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

    public static void mostrar(Context context, Categoria atual, AoEscolher aoEscolher) {
        String[] rotulos = new String[OPCOES.length];
        int selecionada = 0;
        for (int i = 0; i < OPCOES.length; i++) {
            rotulos[i] = rotulo(OPCOES[i]);
            if (OPCOES[i] == atual) selecionada = i;
        }

        new AlertDialog.Builder(context)
                .setTitle("Categoria do produto")
                .setSingleChoiceItems(rotulos, selecionada, (dialog, qual) -> {
                    dialog.dismiss();
                    aoEscolher.escolhida(OPCOES[qual]);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
