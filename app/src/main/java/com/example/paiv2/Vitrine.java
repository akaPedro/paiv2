package com.example.paiv2;

import android.content.Context;
import android.content.Intent;
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
 * As duas vitrines da tela inicial: promoção e destaques. Cada uma guarda a sua
 * lista de ids nas preferências do app (o banco não é alterado). A ordem
 * importa: os primeiros são os que aparecem na tela inicial.
 */
public enum Vitrine {

    PROMOCAO("promocao_ids", "Promoção", "em promoção", "na promoção", "da promoção",
            R.color.vermelho_oferta, R.color.vermelho_oferta_claro, R.drawable.ic_promocao),

    DESTAQUE("destaque_ids", "Destaques", "em destaque", "nos destaques", "dos destaques",
            R.color.dourado_destaque, R.color.dourado_destaque_claro, R.drawable.ic_estrela);

    /** Quantos produtos de cada vitrine aparecem na tela inicial. */
    public static final int VAGAS = 3;

    private static final String PREFS = "paiv2_prefs";
    private static final String EXTRA = "vitrine";

    public final String titulo;
    /** Pedaços de frase: "em promoção", "na promoção", "da promoção". */
    public final String onde;
    public final String paraOnde;
    public final String deOnde;
    public final int cor;
    public final int corClara;
    public final int icone;

    private final String chave;

    Vitrine(String chave, String titulo, String onde, String paraOnde, String deOnde,
            int cor, int corClara, int icone) {
        this.chave = chave;
        this.titulo = titulo;
        this.onde = onde;
        this.paraOnde = paraOnde;
        this.deOnde = deOnde;
        this.cor = cor;
        this.corClara = corClara;
        this.icone = icone;
    }

    /** Diz à próxima tela em qual vitrine ela está mexendo. */
    public void guardar(Intent intent) {
        intent.putExtra(EXTRA, name());
    }

    public static Vitrine recebida(Intent intent) {
        String nome = intent == null ? null : intent.getStringExtra(EXTRA);
        if (nome == null) return PROMOCAO;
        try {
            return valueOf(nome);
        } catch (IllegalArgumentException ignorada) {
            return PROMOCAO;
        }
    }

    public List<Integer> ids(Context context) {
        String guardado = prefs(context).getString(chave, "");
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

    public boolean contem(Context context, int id) {
        return ids(context).contains(id);
    }

    public void adicionar(Context context, Collection<Integer> novos) {
        // LinkedHashSet mantém a ordem e evita repetir quem já está na vitrine
        Set<Integer> atuais = new LinkedHashSet<>(ids(context));
        atuais.addAll(novos);
        salvar(context, new ArrayList<>(atuais));
    }

    public void remover(Context context, Collection<Integer> saindo) {
        List<Integer> atuais = ids(context);
        atuais.removeAll(saindo);
        salvar(context, atuais);
    }

    /** Produto excluído não pode continuar em vitrine nenhuma. */
    public static void removerDeTodas(Context context, Collection<Integer> saindo) {
        for (Vitrine vitrine : values()) vitrine.remover(context, saindo);
    }

    /** Devolve o produto à posição em que estava, para desfazer uma remoção. */
    public void inserir(Context context, int id, int posicao) {
        List<Integer> atuais = ids(context);
        atuais.remove((Integer) id);
        atuais.add(Math.max(0, Math.min(posicao, atuais.size())), id);
        salvar(context, atuais);
    }

    /** Move o produto para o começo, colocando-o nas vagas da tela inicial. */
    public void porNaFrente(Context context, int id) {
        List<Integer> atuais = ids(context);
        if (!atuais.remove((Integer) id)) return;
        atuais.add(0, id);
        salvar(context, atuais);
    }

    /** Tira o produto das vagas sem tirá-lo da lista: manda para o fim da fila. */
    public void mandarParaOFim(Context context, int id) {
        List<Integer> atuais = ids(context);
        if (!atuais.remove((Integer) id)) return;
        atuais.add(id);
        salvar(context, atuais);
    }

    /**
     * Produtos da vitrine que ainda existem, na ordem escolhida. Produtos
     * excluídos em outras telas saem da lista guardada.
     */
    public List<Produto> produtos(Context context, List<Produto> todos) {
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

    private void salvar(Context context, List<Integer> ids) {
        StringBuilder texto = new StringBuilder();
        for (Integer id : ids) {
            if (texto.length() > 0) texto.append(',');
            texto.append(id);
        }
        prefs(context).edit().putString(chave, texto.toString()).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
