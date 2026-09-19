package com.example.paiv2;

import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.ComponentActivity;
import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * No Android 15+ o app é obrigado a desenhar atrás da barra de status e da barra
 * de navegação. Estes ajudantes devolvem o espaço dessas barras às views certas.
 */
public final class Janela {

    public static final int TOPO = 1;
    public static final int BASE = 2;
    /** Soma o teclado aberto ao espaço de baixo, para nada ficar escondido atrás dele. */
    public static final int TECLADO = 4;
    /** Aplica o espaço de baixo como margem em vez de padding (botões flutuantes). */
    public static final int MARGEM_BASE = 8;

    private Janela() {
    }

    public static void telaCheia(ComponentActivity activity, boolean topoEscuro, boolean baseEscura) {
        EdgeToEdge.enable(activity,
                topoEscuro ? SystemBarStyle.dark(Color.TRANSPARENT)
                        : SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
                baseEscura ? SystemBarStyle.dark(Color.TRANSPARENT)
                        : SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT));
    }

    /** Uma view só aceita um ouvinte de recuos, por isso todos os lados vão numa chamada. */
    public static void recuos(View view, int lados) {
        final int paddingTopo = view.getPaddingTop();
        final int paddingBase = view.getPaddingBottom();
        ViewGroup.LayoutParams params = view.getLayoutParams();
        final int margemBase = params instanceof ViewGroup.MarginLayoutParams
                ? ((ViewGroup.MarginLayoutParams) params).bottomMargin : 0;

        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets barras = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            int inferior = barras.bottom;
            if ((lados & TECLADO) != 0) {
                inferior = Math.max(inferior, insets.getInsets(WindowInsetsCompat.Type.ime()).bottom);
            }

            v.setPadding(v.getPaddingLeft(),
                    (lados & TOPO) != 0 ? paddingTopo + barras.top : v.getPaddingTop(),
                    v.getPaddingRight(),
                    (lados & BASE) != 0 ? paddingBase + inferior : v.getPaddingBottom());

            if ((lados & MARGEM_BASE) != 0 && v.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                lp.bottomMargin = margemBase + inferior;
                v.setLayoutParams(lp);
            }
            return insets;
        });
    }
}
