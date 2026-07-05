package com.example.paiv2;

import com.example.paiv2.entity.Produto;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Regras compartilhadas sobre produtos, usadas pelas telas e pelo adapter.
 */
public final class ProdutoUtils {

    // Convenção: um Produto com esse id é um divisor de seção, não um item real
    public static final int ID_DIVISOR = -1;

    private ProdutoUtils() {
    }

    public static boolean isDivisor(Produto p) {
        return p.getId() == ID_DIVISOR;
    }

    /**
     * Remove acentos e coloca em minúsculas, para a busca encontrar
     * "Açúcar" mesmo digitando "acucar".
     */
    public static String normalizar(String texto) {
        if (texto == null) return "";
        String semAcentos = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return semAcentos.toLowerCase(Locale.ROOT).trim();
    }

    public static boolean isAlcoolica(Produto p) {
        if (p.getNome() == null) return false;
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
                || nome.contains("gin")
                || nome.contains("caipirinha");
    }
}
