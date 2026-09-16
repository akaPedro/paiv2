package com.example.paiv2.database;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.paiv2.entity.Categoria;
import com.example.paiv2.entity.Produto;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cadastra no banco as imagens que vêm empacotadas em assets/.
 * Roda apenas na primeira abertura depois da instalação.
 */
public class PopuladorBanco {

    private static final String TAG = "PopuladorBanco";

    private static final String PREFS = "paiv2_prefs";
    private static final String KEY_ASSETS_IMPORTADOS = "assets_importados";

    // Pasta dentro de assets/ -> categoria correspondente
    private static final Map<String, Categoria> PASTAS = new LinkedHashMap<>();

    static {
        PASTAS.put("alim", Categoria.ALIMENTOS);
        PASTAS.put("bebes", Categoria.BEBIDAS);
        PASTAS.put("doces", Categoria.DOCES);
        PASTAS.put("high", Categoria.HIGIENE);
        PASTAS.put("oto", Categoria.OUTROS);
    }

    // Impede que uma segunda importação comece enquanto a primeira roda (ex.: tela
    // girada no meio do processo), o que duplicaria os produtos: as duas veriam a
    // contagem zerada antes de qualquer transação ser gravada.
    private static boolean emAndamento = false;
    private static final List<Runnable> PENDENTES = new ArrayList<>();

    private PopuladorBanco() {
    }

    /** true quando a importação inicial já foi concluída neste aparelho. */
    public static boolean jaImportado(Context context) {
        return prefs(context).getBoolean(KEY_ASSETS_IMPORTADOS, false);
    }

    /**
     * Importa todas as categorias em segundo plano e chama {@code aoConcluir}
     * na thread principal ao terminar.
     *
     * <p>É seguro chamar sempre: em aparelho que já tem produtos, cada categoria
     * é pulada pela trava de contagem e nada é duplicado.
     */
    public static void importarSeNecessario(Context context, AppDatabase db, Runnable aoConcluir) {
        Context appContext = context.getApplicationContext();
        Handler main = new Handler(Looper.getMainLooper());

        synchronized (PopuladorBanco.class) {
            if (aoConcluir != null) PENDENTES.add(aoConcluir);
            // Já tem uma importação rodando: este callback será avisado junto
            // com os outros quando ela terminar.
            if (emAndamento) return;
            emAndamento = true;
        }

        new Thread(() -> {
            try {
                for (Map.Entry<String, Categoria> pasta : PASTAS.entrySet()) {
                    importarCategoria(appContext, db, pasta.getKey(), pasta.getValue());
                }
                // Só marca como pronto depois que TODAS terminaram: se o app for
                // fechado no meio, a importação recomeça na próxima abertura.
                prefs(appContext).edit().putBoolean(KEY_ASSETS_IMPORTADOS, true).apply();
                Log.d(TAG, "✅ Importação inicial concluída");
            } catch (Exception e) {
                Log.e(TAG, "❌ Falha na importação inicial", e);
            } finally {
                List<Runnable> avisar;
                synchronized (PopuladorBanco.class) {
                    emAndamento = false;
                    avisar = new ArrayList<>(PENDENTES);
                    PENDENTES.clear();
                }
                for (Runnable r : avisar) main.post(r);
            }
        }).start();
    }

    private static void importarCategoria(
            Context context,
            AppDatabase db,
            String pastaAssets,
            Categoria categoria
    ) {
        // Trava de segurança contra duplicação: categoria que já tem produtos é pulada
        if (db.produtoDao().contarPorCategoria(categoria) > 0) {
            Log.i(TAG, "⚠️ Categoria já populada: " + categoria);
            return;
        }

        String[] arquivos;
        try {
            AssetManager assetManager = context.getAssets();
            arquivos = assetManager.list(pastaAssets);
        } catch (IOException e) {
            Log.e(TAG, "❌ Erro ao listar assets em " + pastaAssets, e);
            return;
        }

        if (arquivos == null || arquivos.length == 0) {
            Log.w(TAG, "⚠️ Pasta vazia ou inexistente: " + pastaAssets);
            return;
        }

        // Garante que a ordem de inserção seja a alfabética (A-Z)
        Arrays.sort(arquivos);

        List<Produto> produtos = new ArrayList<>();
        for (String nomeArquivo : arquivos) {
            if (!ehImagem(nomeArquivo)) continue;

            Produto p = new Produto();
            p.setNome(semExtensao(nomeArquivo));
            p.setCategoria(categoria);
            // Caminho completo: fundamental para o Glide achar o arquivo
            p.setImageUri(pastaAssets + "/" + nomeArquivo);
            produtos.add(p);
        }

        Log.d(TAG, "🚀 Importando " + produtos.size() + " itens de: " + pastaAssets);

        // Uma transação só: inserir centenas de itens um a um seria lento demais
        db.runInTransaction(() -> {
            for (Produto p : produtos) {
                db.produtoDao().inserir(p);
            }
        });
    }

    private static boolean ehImagem(String nomeArquivo) {
        String nome = nomeArquivo.toLowerCase();
        return nome.endsWith(".jpg")
                || nome.endsWith(".jpeg")
                || nome.endsWith(".png")
                || nome.endsWith(".webp");
    }

    private static String semExtensao(String nomeArquivo) {
        int ponto = nomeArquivo.lastIndexOf('.');
        return ponto > 0 ? nomeArquivo.substring(0, ponto) : nomeArquivo;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
