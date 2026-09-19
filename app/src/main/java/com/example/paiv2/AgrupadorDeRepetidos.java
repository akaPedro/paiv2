package com.example.paiv2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Junta produtos que parecem ser o mesmo item, ligados pela foto ou pelo nome.
 * Não depende do Android para poder ser testada isoladamente.
 */
public final class AgrupadorDeRepetidos {

    /** Um produto reduzido ao que interessa para comparar. */
    public static final class Chave {
        public final int id;
        /** Nome normalizado, ou null quando ainda é provisório. */
        public final String nome;
        /** Resumo do conteúdo da foto, ou null quando não deu para ler. */
        public final String foto;

        public Chave(int id, String nome, String foto) {
            this.id = id;
            this.nome = nome;
            this.foto = foto;
        }
    }

    private AgrupadorDeRepetidos() {
    }

    /**
     * Agrupa em cadeia: se A tem a foto de B e B tem o nome de C, os três ficam
     * no mesmo grupo. Grupos de um só produto não são devolvidos.
     */
    public static List<List<Chave>> agrupar(List<Chave> chaves) {
        Map<Integer, Integer> pai = new HashMap<>();
        for (Chave c : chaves) pai.put(c.id, c.id);

        Map<String, Integer> primeiroComNome = new HashMap<>();
        Map<String, Integer> primeiroComFoto = new HashMap<>();
        for (Chave c : chaves) {
            if (c.nome != null) unirPorChave(pai, primeiroComNome, c.nome, c.id);
            if (c.foto != null) unirPorChave(pai, primeiroComFoto, c.foto, c.id);
        }

        // LinkedHashMap mantém a ordem de leitura dos produtos
        Map<Integer, List<Chave>> porRaiz = new LinkedHashMap<>();
        for (Chave c : chaves) {
            porRaiz.computeIfAbsent(raiz(pai, c.id), r -> new ArrayList<>()).add(c);
        }

        List<List<Chave>> grupos = new ArrayList<>();
        for (List<Chave> grupo : porRaiz.values()) {
            if (grupo.size() > 1) grupos.add(grupo);
        }
        return grupos;
    }

    /** true se pelo menos dois do grupo compartilham a mesma foto. */
    public static boolean mesmaFoto(List<Chave> grupo) {
        return repete(grupo, true);
    }

    /** true se pelo menos dois do grupo compartilham o mesmo nome. */
    public static boolean mesmoNome(List<Chave> grupo) {
        return repete(grupo, false);
    }

    private static boolean repete(List<Chave> grupo, boolean porFoto) {
        Set<String> vistos = new HashSet<>();
        for (Chave c : grupo) {
            String valor = porFoto ? c.foto : c.nome;
            if (valor != null && !vistos.add(valor)) return true;
        }
        return false;
    }

    private static void unirPorChave(Map<Integer, Integer> pai, Map<String, Integer> primeiros,
                                     String chave, int id) {
        Integer anterior = primeiros.putIfAbsent(chave, id);
        if (anterior != null) unir(pai, anterior, id);
    }

    private static int raiz(Map<Integer, Integer> pai, int id) {
        int atual = id;
        while (pai.get(atual) != atual) atual = pai.get(atual);
        // Encurta o caminho para as próximas consultas
        int caminho = id;
        while (pai.get(caminho) != atual) {
            int proximo = pai.get(caminho);
            pai.put(caminho, atual);
            caminho = proximo;
        }
        return atual;
    }

    private static void unir(Map<Integer, Integer> pai, int a, int b) {
        int raizA = raiz(pai, a);
        int raizB = raiz(pai, b);
        if (raizA != raizB) pai.put(raizB, raizA);
    }
}
