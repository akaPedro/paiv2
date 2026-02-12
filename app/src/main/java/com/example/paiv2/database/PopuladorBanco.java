package com.example.paiv2.database;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import com.example.paiv2.entity.Categoria;
import com.example.paiv2.entity.Produto;

import java.io.IOException;
import java.util.Arrays;

public class PopuladorBanco {

    private static final String TAG = "PopuladorBanco";

    public static void importarCategoria(
            Context context,
            AppDatabase db,
            String pastaAssets,
            Categoria categoria
    ) {

        new Thread(() -> {
            try {
                AssetManager assetManager = context.getAssets();
                String[] arquivos = assetManager.list(pastaAssets);

                if (arquivos == null || arquivos.length == 0) {
                    Log.w(TAG, "⚠️ Pasta vazia ou inexistente: " + pastaAssets);
                    return;
                }

                // 1. ORDENAÇÃO ESSENCIAL: Garante que a ordem de inserção
                // seja idêntica à ordem alfabética (A-Z)
                Arrays.sort(arquivos);

                // 2. Trava de segurança contra duplicação
                if (db.produtoDao().contarPorCategoria(categoria) > 0) {
                    Log.i(TAG, "⚠️ Categoria já populada: " + categoria);
                    return;
                }

                Log.d(TAG, "🚀 Iniciando importação de " + arquivos.length + " itens de: " + pastaAssets);

                for (String nomeArquivo : arquivos) {
                    // Aceita apenas extensões de imagem comuns
                    String lowerNome = nomeArquivo.toLowerCase();
                    if (!lowerNome.endsWith(".jpg") &&
                            !lowerNome.endsWith(".jpeg") &&
                            !lowerNome.endsWith(".png") &&
                            !lowerNome.endsWith(".webp")) {
                        continue;
                    }

                    Produto p = new Produto();

                    // Nome limpo para exibição (ex: "arroz_branco")
                    String nomeLimpo = nomeArquivo
                            .replace(".jpg", "")
                            .replace(".jpeg", "")
                            .replace(".png", "")
                            .replace(".webp", "");

                    p.setNome(nomeLimpo);
                    p.setCategoria(categoria);

                    // 3. CAMINHO COMPLETO: Fundamental para o Glide não se perder
                    p.setImageUri(pastaAssets + "/" + nomeArquivo);

                    db.produtoDao().inserir(p);
                }

                Log.d(TAG, "✅ Importação concluída com sucesso: " + pastaAssets);

            } catch (IOException e) {
                Log.e(TAG, "❌ Erro ao listar assets em " + pastaAssets, e);
            }
        }).start();
    }
}