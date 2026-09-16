package com.example.paiv2;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

/**
 * Imagem com zoom: pinça para aproximar, arrastar para mover e dois toques
 * para alternar entre a tela cheia e a aproximação.
 */
public class ImagemZoomView extends AppCompatImageView {

    private static final float ZOOM_MAXIMO = 5f;
    private static final float ZOOM_DOIS_TOQUES = 2.5f;

    private final Matrix matriz = new Matrix();
    private final RectF areaImagem = new RectF();
    private final float[] valores = new float[9];

    // Escala em que a imagem inteira cabe na tela; é o "tamanho normal"
    private float escalaAjuste = 1f;
    private boolean pronta = false;

    private ScaleGestureDetector detectorPinca;
    private GestureDetector detectorGestos;
    private Runnable aoTocarSemZoom;

    public ImagemZoomView(Context context) {
        super(context);
        iniciar(context);
    }

    public ImagemZoomView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        iniciar(context);
    }

    public ImagemZoomView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        iniciar(context);
    }

    private void iniciar(Context context) {
        setScaleType(ScaleType.MATRIX);
        detectorPinca = new ScaleGestureDetector(context, new Pinca());
        detectorGestos = new GestureDetector(context, new Gestos());
    }

    /** Ação para um toque simples com a imagem no tamanho normal (fechar a tela). */
    public void setAoTocarSemZoom(Runnable acao) {
        this.aoTocarSemZoom = acao;
    }

    @Override
    public void setImageDrawable(@Nullable Drawable drawable) {
        super.setImageDrawable(drawable);
        ajustarParaCaber();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        ajustarParaCaber();
    }

    /** Volta a imagem para o tamanho em que ela cabe inteira na tela. */
    private void ajustarParaCaber() {
        Drawable desenho = getDrawable();
        if (desenho == null) {
            pronta = false;
            return;
        }

        float larguraView = getWidth() - getPaddingLeft() - getPaddingRight();
        float alturaView = getHeight() - getPaddingTop() - getPaddingBottom();
        float larguraImg = desenho.getIntrinsicWidth();
        float alturaImg = desenho.getIntrinsicHeight();

        if (larguraView <= 0 || alturaView <= 0 || larguraImg <= 0 || alturaImg <= 0) {
            pronta = false;
            return;
        }

        escalaAjuste = Math.min(larguraView / larguraImg, alturaView / alturaImg);

        matriz.reset();
        matriz.setScale(escalaAjuste, escalaAjuste);
        matriz.postTranslate(
                getPaddingLeft() + (larguraView - larguraImg * escalaAjuste) / 2f,
                getPaddingTop() + (alturaView - alturaImg * escalaAjuste) / 2f);
        setImageMatrix(matriz);
        pronta = true;
    }

    private float escalaAtual() {
        matriz.getValues(valores);
        return valores[Matrix.MSCALE_X];
    }

    public boolean estaAmpliada() {
        return pronta && escalaAtual() > escalaAjuste * 1.01f;
    }

    /** Impede que a imagem seja arrastada para fora da tela. */
    private void aplicarLimites() {
        Drawable desenho = getDrawable();
        if (desenho == null) return;

        areaImagem.set(0, 0, desenho.getIntrinsicWidth(), desenho.getIntrinsicHeight());
        matriz.mapRect(areaImagem);

        float esquerda = getPaddingLeft();
        float topo = getPaddingTop();
        float direita = getWidth() - getPaddingRight();
        float base = getHeight() - getPaddingBottom();

        float deslocX = 0;
        if (areaImagem.width() <= direita - esquerda) {
            // Menor que a tela: fica centralizada
            deslocX = esquerda + (direita - esquerda - areaImagem.width()) / 2f - areaImagem.left;
        } else if (areaImagem.left > esquerda) {
            deslocX = esquerda - areaImagem.left;
        } else if (areaImagem.right < direita) {
            deslocX = direita - areaImagem.right;
        }

        float deslocY = 0;
        if (areaImagem.height() <= base - topo) {
            deslocY = topo + (base - topo - areaImagem.height()) / 2f - areaImagem.top;
        } else if (areaImagem.top > topo) {
            deslocY = topo - areaImagem.top;
        } else if (areaImagem.bottom < base) {
            deslocY = base - areaImagem.bottom;
        }

        matriz.postTranslate(deslocX, deslocY);
        setImageMatrix(matriz);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!pronta) return super.onTouchEvent(event);
        detectorPinca.onTouchEvent(event);
        detectorGestos.onTouchEvent(event);
        return true;
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    private class Pinca extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(@NonNull ScaleGestureDetector detector) {
            float atual = escalaAtual();
            // Nunca menor que a tela cheia nem maior que o limite
            float alvo = Math.max(escalaAjuste,
                    Math.min(atual * detector.getScaleFactor(), escalaAjuste * ZOOM_MAXIMO));
            float fator = alvo / atual;

            matriz.postScale(fator, fator, detector.getFocusX(), detector.getFocusY());
            aplicarLimites();
            return true;
        }
    }

    private class Gestos extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onScroll(MotionEvent inicio, @NonNull MotionEvent atual,
                                float distanciaX, float distanciaY) {
            if (!estaAmpliada()) return false;
            matriz.postTranslate(-distanciaX, -distanciaY);
            aplicarLimites();
            return true;
        }

        @Override
        public boolean onDoubleTap(@NonNull MotionEvent e) {
            if (estaAmpliada()) {
                ajustarParaCaber();
            } else {
                matriz.postScale(ZOOM_DOIS_TOQUES, ZOOM_DOIS_TOQUES, e.getX(), e.getY());
                aplicarLimites();
            }
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(@NonNull MotionEvent e) {
            // Com zoom, o toque volta ao tamanho normal em vez de fechar: assim
            // ninguém fica preso na imagem ampliada sem saber como sair
            if (estaAmpliada()) {
                ajustarParaCaber();
            } else if (aoTocarSemZoom != null) {
                performClick();
                aoTocarSemZoom.run();
            }
            return true;
        }
    }
}
