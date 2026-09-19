package com.example.paiv2;

import com.example.paiv2.entity.Categoria;
import com.example.paiv2.entity.Produto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Busca de produtos por nome, usada na tela inicial e na escolha de produtos
 * da promoção.
 */
public final class BuscaProdutos {

    // Abaixo disso, tolerar erro de digitação traria resultados demais sem relação
    private static final int MIN_LETRAS_TOLERANCIA = 4;

    /** Produto com o nome já normalizado, para filtrar sem depender de acentos. */
    public static final class Item {
        final Produto produto;
        final String nomeNormalizado;
        final String[] palavras;

        Item(Produto produto) {
            this.produto = produto;
            this.nomeNormalizado = ProdutoUtils.normalizar(produto.getNome());
            this.palavras = this.nomeNormalizado.split("\\s+");
        }
    }

    private BuscaProdutos() {
    }

    public static List<Item> preparar(List<Produto> produtos) {
        List<Item> itens = new ArrayList<>(produtos.size());
        for (Produto p : produtos) itens.add(new Item(p));
        return itens;
    }

    /**
     * Procura ignorando acentos e maiúsculas. Cada palavra digitada precisa aparecer
     * no nome ("leite po" encontra "Leite em Pó"). Se nada for encontrado, tenta de
     * novo perdoando um erro de digitação por palavra. O resultado vem agrupado por
     * categoria, para o usuário saber onde cada produto está.
     */
    public static List<Produto> buscar(List<Item> itens, String texto) {
        String[] palavras = ProdutoUtils.normalizar(texto).split("\\s+");

        List<Item> encontrados = casar(itens, palavras, false);
        if (encontrados.isEmpty()) {
            encontrados = casar(itens, palavras, true);
        }

        ordenarPorRelevancia(encontrados, palavras[0]);

        List<Produto> produtos = new ArrayList<>(encontrados.size());
        for (Item item : encontrados) produtos.add(item.produto);
        return agrupar(produtos);
    }

    /** Separa os produtos por categoria, com um cabeçalho antes de cada grupo. */
    public static List<Produto> agrupar(List<Produto> produtos) {
        // LinkedHashMap pré-carregado mantém a ordem em que as categorias são exibidas
        Map<Categoria, List<Produto>> grupos = new LinkedHashMap<>();
        for (Categoria categoria : SeletorCategoria.opcoes()) {
            grupos.put(categoria, new ArrayList<>());
        }

        for (Produto produto : produtos) {
            List<Produto> grupo = grupos.get(produto.getCategoria());
            // Categoria desconhecida não some da lista: cai em Outros
            if (grupo == null) grupo = grupos.get(Categoria.OUTROS);
            grupo.add(produto);
        }

        List<Produto> resultado = new ArrayList<>();
        for (Map.Entry<Categoria, List<Produto>> grupo : grupos.entrySet()) {
            if (grupo.getValue().isEmpty()) continue;
            resultado.add(ProdutoUtils.criarDivisor(SeletorCategoria.rotulo(grupo.getKey()), grupo.getKey()));
            resultado.addAll(grupo.getValue());
        }
        return resultado;
    }

    private static List<Item> casar(List<Item> itens, String[] palavras, boolean tolerante) {
        List<Item> encontrados = new ArrayList<>();
        for (Item item : itens) {
            boolean contemTodas = true;
            for (String palavra : palavras) {
                if (!combina(item, palavra, tolerante)) {
                    contemTodas = false;
                    break;
                }
            }
            if (contemTodas) encontrados.add(item);
        }
        return encontrados;
    }

    private static boolean combina(Item item, String palavra, boolean tolerante) {
        if (item.nomeNormalizado.contains(palavra)) return true;
        if (!tolerante || palavra.length() < MIN_LETRAS_TOLERANCIA) return false;

        // Compara com cada palavra do nome aceitando um erro: "arros" acha "Arroz"
        for (String parte : item.palavras) {
            if (ProdutoUtils.ateUmaEdicao(parte, palavra)) return true;
        }
        return false;
    }

    /** Quem tem o termo mais no começo do nome aparece primeiro. */
    private static void ordenarPorRelevancia(List<Item> encontrados, String primeiraPalavra) {
        encontrados.sort((a, b) -> {
            int posA = posicao(a.nomeNormalizado, primeiraPalavra);
            int posB = posicao(b.nomeNormalizado, primeiraPalavra);
            if (posA != posB) return Integer.compare(posA, posB);
            return a.nomeNormalizado.compareTo(b.nomeNormalizado);
        });
    }

    private static int posicao(String nome, String termo) {
        int pos = nome.indexOf(termo);
        // Achado só por semelhança: vai para o fim da lista
        return pos < 0 ? Integer.MAX_VALUE : pos;
    }
}
