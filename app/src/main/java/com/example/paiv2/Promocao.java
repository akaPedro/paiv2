package com.example.paiv2;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.paiv2.entity.Produto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Produtos marcados como promoção, guardados nas preferências do app (o banco
 * não é alterado). A ordem importa: os primeiros são os que aparecem na vitrine
 * da tela inicial.
 */
public final class Promocao {

    private static final String PREFS = "paiv2_prefs";
    private static final String CHAVE = "promocao_ids";

    /** Quantos produtos aparecem na vitrine da tela inicial. */
    public static final int DESTAQUES = 3;

    private Promocao() {
    }

    public static List<Integer> ids(Context context) {
        String guardado = prefs(context).getString(CHAVE, "");
        List<Integer> ids = new ArrayList<>();
        if (guardado == null || guardado.isEmpty()) return ids;

        for (String parte : guardado.split(",")) {
            try {
                ids.add(Integer.parseInt(parte.trim()));
            } catch (NumberFormatException ignorada) {
                // Preferência corrompida: o id inválido é simplesmente ignorado
            }
        }
        return ids;
    }

    public static boolean contem(Context context, int id) {
        return ids(context).contains(id);
    }

    public static void adicionar(Context context, Collection<Integer> novos) {
        // LinkedHashSet mantém a ordem e evita repetir quem já está na promoção
        Set<Integer> atuais = new LinkedHashSet<>(ids(context));
        atuais.addAll(novos);
        salvar(context, new ArrayList<>(atuais));
    }

    public static void remover(Context context, Collection<Integer> saindo) {
        List<Integer> atuais = ids(context);
        atuais.removeAll(saindo);
        salvar(context, atuais);
    }

    /** Devolve o produto à posição em que estava, para desfazer uma remoção. */
    public static void inserir(Context context, int id, int posicao) {
        List<Integer> atuais = ids(context);
        atuais.remove((Integer) id);
        atuais.add(Math.max(0, Math.min(posicao, atuais.size())), id);
        salvar(context, atuais);
    }

    /** Move o produto para o começo, colocando-o na vitrine. */
    public static void destacar(Context context, int id) {
        List<Integer> atuais = ids(context);
        if (!atuais.remove((Integer) id)) return;
        atuais.add(0, id);
        salvar(context, atuais);
    }

    /** Tira o produto da vitrine sem tirá-lo da promoção: manda para o fim da fila. */
    public static void tirarDoDestaque(Context context, int id) {
        List<Integer> atuais = ids(context);
        if (!atuais.remove((Integer) id)) return;
        atuais.add(id);
        salvar(context, atuais);
    }

    /**
     * Produtos da promoção que ainda existem, na ordem escolhida. Produtos
     * excluídos em outras telas saem da lista guardada.
     */
    public static List<Produto> produtos(Context context, List<Produto> todos) {
        Map<Integer, Produto> porId = new HashMap<>();
        for (Produto p : todos) porId.put(p.getId(), p);

        List<Integer> ids = ids(context);
        List<Produto> encontrados = new ArrayList<>();
        List<Integer> validos = new ArrayList<>();
        for (Integer id : ids) {
            Produto p = porId.get(id);
            if (p != null) {
                encontrados.add(p);
                validos.add(id);
            }
        }
        if (validos.size() != ids.size()) salvar(context, validos);
        return encontrados;
    }

    public static void salvar(Context context, List<Integer> ids) {
        StringBuilder texto = new StringBuilder();
        for (Integer id : ids) {
            if (texto.length() > 0) texto.append(',');
            texto.append(id);
        }
        prefs(context).edit().putString(CHAVE, texto.toString()).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
