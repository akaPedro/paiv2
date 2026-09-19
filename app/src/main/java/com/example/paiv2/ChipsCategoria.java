package com.example.paiv2;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.example.paiv2.entity.Categoria;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

/**
 * Escolha de categoria direto na tela, com um chip colorido por categoria:
 * mais rápido que abrir uma lista e deixa a escolha sempre visível.
 */
public final class ChipsCategoria {

    private static final int[][] ESTADOS = {{android.R.attr.state_checked}, {}};

    private ChipsCategoria() {
    }

    public static void montar(ChipGroup grupo, Categoria inicial, SeletorCategoria.AoEscolher aoEscolher) {
        Context context = grupo.getContext();
        grupo.setOnCheckedStateChangeListener(null);
        grupo.removeAllViews();
        grupo.setSingleSelection(true);
        grupo.setSelectionRequired(true);

        int branco = ContextCompat.getColor(context, R.color.branco);
        int superficie = ContextCompat.getColor(context, R.color.superficie);
        int texto = ContextCompat.getColor(context, R.color.texto_primario);
        int contorno = ContextCompat.getColor(context, R.color.contorno);

        for (Categoria categoria : SeletorCategoria.opcoes()) {
            Chip chip = (Chip) LayoutInflater.from(context).inflate(R.layout.item_chip_categoria, grupo, false);
            int cor = EstiloCategoria.cor(context, categoria);

            chip.setId(View.generateViewId());
            chip.setTag(categoria);
            chip.setText(SeletorCategoria.rotulo(categoria));
            chip.setChipIconResource(EstiloCategoria.icone(categoria));
            chip.setChipBackgroundColor(new ColorStateList(ESTADOS, new int[]{cor, superficie}));
            chip.setTextColor(new ColorStateList(ESTADOS, new int[]{branco, texto}));
            chip.setChipIconTint(new ColorStateList(ESTADOS, new int[]{branco, cor}));
            chip.setChipStrokeColor(new ColorStateList(ESTADOS, new int[]{cor, contorno}));

            grupo.addView(chip);
            if (categoria == inicial) chip.setChecked(true);
        }

        grupo.setOnCheckedStateChangeListener((g, ids) -> {
            if (ids.isEmpty()) return;
            Chip marcado = g.findViewById(ids.get(0));
            if (marcado != null) aoEscolher.escolhida((Categoria) marcado.getTag());
        });
    }
}
