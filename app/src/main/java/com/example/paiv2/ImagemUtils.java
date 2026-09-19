package com.example.paiv2;

import android.net.Uri;

import java.io.File;

/**
 * Converte o caminho guardado no banco no formato que o Glide entende.
 */
public final class ImagemUtils {

    private ImagemUtils() {
    }

    public static Object modelo(String uri) {
        if (uri == null || uri.isEmpty()) return null;
        // Cópia interna feita ao adicionar pela galeria
        if (uri.startsWith("/")) return new File(uri);
        // URI de galeria (produtos antigos)
        if (uri.startsWith("content://") || uri.startsWith("file://")) return Uri.parse(uri);
        // Imagem que veio dentro do app, em assets/
        return "file:///android_asset/" + uri;
    }
}
