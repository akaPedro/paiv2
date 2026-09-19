package com.example.paiv2;

import android.content.Context;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

import com.example.paiv2.entity.Categoria;

/**
 * Cor e ícone de cada categoria. A mesma cor aparece no início, no cabeçalho da
 * categoria e nos chips de escolha, para a pessoa reconhecer a seção pela cor.
 */
public final class EstiloCategoria {

    private EstiloCategoria() {
    }

    @ColorRes
    public static int corRes(Categoria categoria) {
        if (categoria == null) return R.color.azul_logo;
        switch (categoria) {
            case ALIMENTOS:
                return R.color.cat_alimentos;
            case DOCES:
                return R.color.cat_doces;
            case BEBIDAS:
                return R.color.cat_bebidas;
            case HIGIENE:
                return R.color.cat_higiene;
            default:
                return R.color.cat_outros;
        }
    }

    @ColorInt
    public static int cor(Context context, Categoria categoria) {
        return ContextCompat.getColor(context, corRes(categoria));
    }

    /** A cor da categoria bem clarinha, para fundos de destaque. */
    @ColorInt
    public static int corClara(Context context, Categoria categoria) {
        return ColorUtils.compositeColors(
                ColorUtils.setAlphaComponent(cor(context, categoria), 0x2E),
                ContextCompat.getColor(context, R.color.superficie));
    }

    @DrawableRes
    public static int icone(Categoria categoria) {
        if (categoria == null) return R.drawable.ic_cat_outros;
        switch (categoria) {
            case ALIMENTOS:
                return R.drawable.ic_cat_alimentos;
            case DOCES:
                return R.drawable.ic_cat_doces;
            case BEBIDAS:
                return R.drawable.ic_cat_bebidas;
            case HIGIENE:
                return R.drawable.ic_cat_higiene;
            default:
                return R.drawable.ic_cat_outros;
        }
    }
}
