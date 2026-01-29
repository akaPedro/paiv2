package com.example.paiv2.database;

import android.content.Context;
import android.content.res.AssetManager;

import com.example.paiv2.entity.Categoria;
import com.example.paiv2.entity.Produto;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class
PopuladorBanco {

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
                    System.out.println("⚠️ Pasta vazia: " + pastaAssets);
                    return;
                }

                // 🔒 Evita duplicação
                if (db.produtoDao().contarPorCategoria(categoria) > 0) {
                    System.out.println("⚠️ Categoria já populada: " + categoria);
                    return;
                }

                for (String nomeArquivo : arquivos) {

                    // Aceita apenas imagens
                    if (!nomeArquivo.endsWith(".jpg")
                            && !nomeArquivo.endsWith(".jpeg")
                            && !nomeArquivo.endsWith(".png")) {
                        continue;
                    }

                    Produto p = new Produto();

                    // Nome sem extensão
                    String nomeLimpo = nomeArquivo
                            .replace(".jpg", "")
                            .replace(".jpeg", "")
                            .replace(".png", "");

                    p.setNome(nomeLimpo);
                    p.setCategoria(categoria);

                    // Caminho do asset
                    p.setImageUri(pastaAssets + "/" + nomeArquivo);

                    db.produtoDao().inserir(p);
                }

                System.out.println("✅ Importação concluída: " + pastaAssets);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
