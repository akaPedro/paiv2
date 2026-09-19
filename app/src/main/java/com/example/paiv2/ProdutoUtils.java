package com.example.paiv2;

import com.example.paiv2.entity.Categoria;
import com.example.paiv2.entity.Produto;

import java.text.NumberFormat;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Regras compartilhadas sobre produtos, usadas pelas telas e pelo adapter.
 */
public final class ProdutoUtils {

    // Convenção: um Produto com esse id é um divisor de seção, não um item real
    public static final int ID_DIVISOR = -1;

    public static final String TITULO_NAO_ALCOOLICAS = "Não alcoólicas";
    public static final String TITULO_ALCOOLICAS = "Alcoólicas";

    // Nomes gerados automaticamente: "produto_001" (importação) e "ALIMENTOS 3" (adicionar sem nome)
    private static final Pattern NOME_PROVISORIO =
            Pattern.compile("^(produto_\\d+|(alimentos|bebidas|higiene|doces|outros) \\d+)$");

    // Palavras curtas só valem inteiras ("gin" não pode marcar "Ginger Ale");
    // as longas valem como começo de palavra ("cervejas", "vinhos")
    private static final String[] BEBIDA_PALAVRA_INTEIRA = {"gin", "rum", "ice", "chope", "chopp"};
    private static final String[] BEBIDA_INICIO_DE_PALAVRA = {
            "cerveja", "vinho", "vodka", "whisky", "whiskey", "cachaca", "caninha", "montila",
            "catuaba", "licor", "conhaque", "sidra", "aguardente", "corote", "caipirinha",
            "espumante", "tequila"
    };

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private ProdutoUtils() {
    }

    public static boolean isDivisor(Produto p) {
        return p.getId() == ID_DIVISOR;
    }

    /** Cria o cabeçalho de seção que aparece ocupando a linha inteira da grade. */
    public static Produto criarDivisor(String titulo, Categoria categoria) {
        Produto divisor = new Produto();
        divisor.setId(ID_DIVISOR);
        divisor.setNome(titulo);
        divisor.setCategoria(categoria);
        return divisor;
    }

    /** true enquanto o produto ainda tem o nome gerado automaticamente. */
    public static boolean semNome(Produto p) {
        String nome = p.getNome();
        if (nome == null || nome.trim().isEmpty()) return true;
        return NOME_PROVISORIO.matcher(nome.trim().toLowerCase(Locale.ROOT)).matches();
    }

    /** 1635 vira "1.635". */
    public static String numero(int n) {
        return NumberFormat.getIntegerInstance(PT_BR).format(n);
    }

    /** "1 produto", "331 produtos". */
    public static String contagem(int n, String singular, String plural) {
        return n == 1 ? "1 " + singular : numero(n) + " " + plural;
    }

    /**
     * true se as duas palavras diferem por no máximo uma edição: uma letra
     * trocada, faltando ou sobrando. Serve para a busca perdoar erros de digitação.
     */
    public static boolean ateUmaEdicao(String a, String b) {
        int tamA = a.length();
        int tamB = b.length();
        if (Math.abs(tamA - tamB) > 1) return false;

        int i = 0, j = 0, diferencas = 0;
        while (i < tamA && j < tamB) {
            if (a.charAt(i) == b.charAt(j)) {
                i++;
                j++;
                continue;
            }
            if (++diferencas > 1) return false;
            // Pula a letra sobrando na palavra mais longa (ou as duas, se do mesmo tamanho)
            if (tamA > tamB) i++;
            else if (tamB > tamA) j++;
            else {
                i++;
                j++;
            }
        }
        // Sobrou uma letra no fim de uma das palavras
        if (i < tamA || j < tamB) diferencas++;
        return diferencas <= 1;
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

    /** Só bebidas podem ser alcoólicas: evita marcar "Biscoito Original" por causa de "gin". */
    public static boolean isAlcoolica(Produto p) {
        if (p.getNome() == null) return false;
        if (p.getCategoria() != null && p.getCategoria() != Categoria.BEBIDAS) return false;

        String nome = normalizar(p.getNome());
        if (nome.contains("raiz amarga")) return true;

        for (String palavra : nome.split("[^a-z0-9]+")) {
            for (String chave : BEBIDA_PALAVRA_INTEIRA) {
                if (palavra.equals(chave)) return true;
            }
            for (String chave : BEBIDA_INICIO_DE_PALAVRA) {
                if (palavra.startsWith(chave)) return true;
            }
        }
        return false;
    }
}
