package com.example.paiv2;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.example.paiv2.database.AppDatabase;
import com.example.paiv2.entity.Categoria;
import com.example.paiv2.entity.Produto;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Alterações em vários produtos de uma vez, usadas pela seleção em lote e pela
 * tela de produtos repetidos.
 */
public final class AcoesProduto {

    private AcoesProduto() {
    }

    /** Exclui os produtos e apaga as fotos que estavam guardadas dentro do app. */
    public static void excluir(Context context, List<Produto> alvos, Runnable aoTerminar) {
        if (alvos.isEmpty()) {
            aoTerminar.run();
            return;
        }

        AppDatabase db = AppDatabase.getInstance(context);
        Handler main = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            db.runInTransaction(() -> {
                for (Produto p : alvos) db.produtoDao().deletar(p);
            });

            // As fotos só somem depois do banco confirmar: se a transação falhar,
            // nenhum produto fica no banco apontando para arquivo apagado
            for (Produto p : alvos) {
                String uri = p.getImageUri();
                if (uri != null && uri.startsWith("/")) new File(uri).delete();
            }

            // Produto excluído não pode continuar na promoção
            List<Integer> ids = new ArrayList<>();
            for (Produto p : alvos) ids.add(p.getId());
            Promocao.remover(context, ids);

            main.post(aoTerminar);
        }).start();
    }

    /** Move os produtos para outra categoria, tudo em uma transação. */
    public static void mover(Context context, List<Produto> alvos, Categoria destino, Runnable aoTerminar) {
        if (alvos.isEmpty()) {
            aoTerminar.run();
            return;
        }

        AppDatabase db = AppDatabase.getInstance(context);
        Handler main = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            db.runInTransaction(() -> {
                for (Produto p : alvos) {
                    p.setCategoria(destino);
                    db.produtoDao().atualizar(p);
                }
            });
            main.post(aoTerminar);
        }).start();
    }
}
