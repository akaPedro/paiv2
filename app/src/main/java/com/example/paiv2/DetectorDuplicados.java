package com.example.paiv2;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.paiv2.database.AppDatabase;
import com.example.paiv2.entity.Produto;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Encontra produtos repetidos comparando o conteúdo das fotos e, quando já têm
 * nome, também o nome. Nome sozinho não serve: os produtos recém-importados se
 * chamam todos "produto_001", "produto_002"... em todas as categorias.
 */
public final class DetectorDuplicados {

    private static final String TAG = "DetectorDuplicados";

    public interface AoTerminar {
        void encontrou(List<Grupo> grupos);
    }

    /** Produtos que parecem ser o mesmo item. */
    public static final class Grupo {
        public final List<Produto> produtos;
        public final boolean mesmaFoto;
        public final boolean mesmoNome;

        Grupo(List<Produto> produtos, boolean mesmaFoto, boolean mesmoNome) {
            this.produtos = produtos;
            this.mesmaFoto = mesmaFoto;
            this.mesmoNome = mesmoNome;
        }

        public String motivo() {
            if (mesmaFoto && mesmoNome) return "Mesma foto e mesmo nome";
            return mesmaFoto ? "Mesma foto" : "Mesmo nome";
        }
    }

    private DetectorDuplicados() {
    }

    public static void procurar(Context context, AppDatabase db, AoTerminar aoTerminar) {
        Context appContext = context.getApplicationContext();
        Handler main = new Handler(Looper.getMainLooper());

        new Thread(() -> {
            List<Grupo> grupos = new ArrayList<>();
            try {
                grupos = procurarAgora(appContext, db);
            } catch (Exception e) {
                Log.w(TAG, "Não foi possível procurar repetidos", e);
            }
            List<Grupo> resultado = grupos;
            main.post(() -> aoTerminar.encontrou(resultado));
        }).start();
    }

    private static List<Grupo> procurarAgora(Context context, AppDatabase db) {
        List<Produto> todos = db.produtoDao().listarTodos();

        // 1. O tamanho do arquivo é barato de ler e já separa quase todas as fotos
        Map<Long, List<Produto>> porTamanho = new HashMap<>();
        for (Produto p : todos) {
            long tamanho = tamanhoDaFoto(context, p.getImageUri());
            if (tamanho > 0) {
                porTamanho.computeIfAbsent(tamanho, t -> new ArrayList<>()).add(p);
            }
        }

        // 2. Só vale ler o arquivo inteiro onde o tamanho se repete
        Map<Integer, String> fotoPorId = new HashMap<>();
        for (Map.Entry<Long, List<Produto>> entrada : porTamanho.entrySet()) {
            if (entrada.getValue().size() < 2) continue;
            for (Produto p : entrada.getValue()) {
                String resumo = resumoDaFoto(context, p.getImageUri());
                if (resumo != null) fotoPorId.put(p.getId(), entrada.getKey() + ":" + resumo);
            }
        }

        List<AgrupadorDeRepetidos.Chave> chaves = new ArrayList<>(todos.size());
        Map<Integer, Produto> porId = new HashMap<>();
        for (Produto p : todos) {
            porId.put(p.getId(), p);
            // Nome provisório não conta: "produto_001" existe em toda categoria
            String nome = ProdutoUtils.semNome(p) ? null : ProdutoUtils.normalizar(p.getNome());
            chaves.add(new AgrupadorDeRepetidos.Chave(p.getId(), nome, fotoPorId.get(p.getId())));
        }

        List<Grupo> grupos = new ArrayList<>();
        for (List<AgrupadorDeRepetidos.Chave> grupo : AgrupadorDeRepetidos.agrupar(chaves)) {
            List<Produto> produtos = new ArrayList<>(grupo.size());
            for (AgrupadorDeRepetidos.Chave c : grupo) produtos.add(porId.get(c.id));
            grupos.add(new Grupo(produtos,
                    AgrupadorDeRepetidos.mesmaFoto(grupo),
                    AgrupadorDeRepetidos.mesmoNome(grupo)));
        }
        return grupos;
    }

    private static long tamanhoDaFoto(Context context, String uri) {
        if (uri == null || uri.isEmpty()) return -1;
        try {
            // Cópia guardada dentro do app
            if (uri.startsWith("/")) return new File(uri).length();
            // Imagem que veio junto com o app
            if (!uri.startsWith("content://") && !uri.startsWith("file://")) {
                try (AssetFileDescriptor descritor = context.getAssets().openFd(uri)) {
                    return descritor.getLength();
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Não foi possível medir " + uri, e);
        }
        return -1;
    }

    /** Resumo do conteúdo do arquivo, para comparar fotos byte a byte. */
    private static String resumoDaFoto(Context context, String uri) {
        try (InputStream entrada = abrir(context, uri)) {
            if (entrada == null) return null;
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] buffer = new byte[64 * 1024];
            int lidos;
            while ((lidos = entrada.read(buffer)) > 0) {
                digest.update(buffer, 0, lidos);
            }
            StringBuilder hex = new StringBuilder();
            for (byte b : digest.digest()) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            Log.w(TAG, "Não foi possível ler " + uri, e);
            return null;
        }
    }

    private static InputStream abrir(Context context, String uri) throws Exception {
        if (uri.startsWith("/")) return new FileInputStream(uri);
        if (uri.startsWith("content://") || uri.startsWith("file://")) return null;
        return context.getAssets().open(uri);
    }

    /** Quantos produtos sobram de repetidos, sem contar o que será mantido em cada grupo. */
    public static int totalRepetidos(List<Grupo> grupos) {
        int total = 0;
        for (Grupo g : grupos) total += g.produtos.size() - 1;
        return total;
    }
}
