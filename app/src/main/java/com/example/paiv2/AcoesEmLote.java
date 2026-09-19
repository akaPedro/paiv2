package com.example.paiv2;

import android.view.View;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.paiv2.entity.Categoria;
import com.example.paiv2.entity.Produto;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

/**
 * Barras do modo de seleção: no topo, quantos estão marcados e "Todos";
 * embaixo, editar (um só), mover e excluir. A tela precisa incluir os layouts
 * barra_selecao_topo e barra_selecao_acoes.
 */
public class AcoesEmLote {

    private final AppCompatActivity activity;
    private final ProdutoAdapter adapter;
    private final View barraTopo;
    private final View barraAcoes;
    private final TextView txtQuantidade;
    private final MaterialButton btnTodos;
    private final MaterialButton btnEditar;
    private final MaterialButton btnPromocao;
    private final MaterialButton btnMover;
    private final MaterialButton btnExcluir;
    // Somem durante a seleção (o botão "Adicionar"): tocar neles sairia da tela
    // e perderia o que já foi marcado
    private final View[] esconderDuranteSelecao;
    // A barra de cima cobre este cabeçalho inteiro, para nada dele aparecer por baixo
    private final View cabecalho;
    private final OnBackPressedCallback voltar;
    private View ancoraAvisos;

    private AcoesEmLote(AppCompatActivity activity, ProdutoAdapter adapter, View cabecalho,
                        View[] esconderDuranteSelecao) {
        this.activity = activity;
        this.adapter = adapter;
        this.cabecalho = cabecalho;
        this.esconderDuranteSelecao = esconderDuranteSelecao;

        barraTopo = activity.findViewById(R.id.barraSelecaoTopo);
        barraAcoes = activity.findViewById(R.id.barraSelecaoAcoes);
        txtQuantidade = activity.findViewById(R.id.txtQuantidadeSelecionada);
        btnTodos = activity.findViewById(R.id.btnSelecionarTodos);
        btnEditar = activity.findViewById(R.id.btnSelEditar);
        btnPromocao = activity.findViewById(R.id.btnSelPromocao);
        btnMover = activity.findViewById(R.id.btnSelMover);
        btnExcluir = activity.findViewById(R.id.btnSelExcluir);

        Janela.recuos(barraTopo, Janela.TOPO);
        Janela.recuos(barraAcoes, Janela.BASE);

        activity.findViewById(R.id.btnSairSelecao).setOnClickListener(v -> adapter.sairModoSelecao());
        btnTodos.setOnClickListener(v -> adapter.selecionarTodos());
        btnEditar.setOnClickListener(v -> {
            adapter.editarUnicoSelecionado();
            adapter.sairModoSelecao();
        });
        btnPromocao.setOnClickListener(v -> colocarEmPromocao());
        btnMover.setOnClickListener(v -> escolherDestino());
        btnExcluir.setOnClickListener(v -> confirmarExclusao());

        // O botão voltar sai da seleção em vez de sair da tela
        voltar = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                adapter.sairModoSelecao();
            }
        };
        activity.getOnBackPressedDispatcher().addCallback(activity, voltar);

        adapter.setAoMudarSelecao(this::atualizar);
    }

    public static AcoesEmLote instalar(AppCompatActivity activity, ProdutoAdapter adapter, View cabecalho,
                                       View... esconderDuranteSelecao) {
        return new AcoesEmLote(activity, adapter, cabecalho, esconderDuranteSelecao);
    }

    /** Os avisos ("3 produtos movidos") aparecem acima desta view, se ela estiver visível. */
    public void setAncoraAvisos(View ancora) {
        this.ancoraAvisos = ancora;
    }

    private void atualizar(boolean ativo, int quantidade) {
        voltar.setEnabled(ativo);
        // O cabeçalho da tela inicial muda de altura durante a busca: mede na hora
        if (ativo) barraTopo.setMinimumHeight(cabecalho.getHeight());
        barraTopo.setVisibility(ativo ? View.VISIBLE : View.GONE);
        barraAcoes.setVisibility(ativo ? View.VISIBLE : View.GONE);
        for (View v : esconderDuranteSelecao) {
            v.setVisibility(ativo ? View.GONE : View.VISIBLE);
        }
        if (!ativo) return;

        txtQuantidade.setText(quantidade == 0
                ? "Toque nos produtos"
                : ProdutoUtils.contagem(quantidade, "selecionado", "selecionados"));
        btnEditar.setEnabled(quantidade == 1);
        btnPromocao.setEnabled(quantidade > 0);
        btnMover.setEnabled(quantidade > 0);
        btnExcluir.setEnabled(quantidade > 0);
        btnTodos.setText(adapter.todosSelecionados() ? "Nenhum" : "Todos");
    }

    private void colocarEmPromocao() {
        final int quantos = adapter.getQuantidadeSelecionada();
        adapter.colocarSelecionadosEmPromocao();
        adapter.sairModoSelecao();
        avisar(ProdutoUtils.contagem(quantos, "produto entrou", "produtos entraram") + " na promoção");
    }

    private void escolherDestino() {
        List<Produto> marcados = adapter.getSelecionados();
        final int quantos = marcados.size();

        // Se todos vêm da mesma categoria, ela aparece marcada como "atual"
        Categoria atual = marcados.isEmpty() ? null : marcados.get(0).getCategoria();
        for (Produto p : marcados) {
            if (p.getCategoria() != atual) {
                atual = null;
                break;
            }
        }

        String titulo = quantos == 1 ? "Mover produto para" : "Mover " + quantos + " produtos para";
        SeletorCategoria.mostrar(activity, titulo, atual, destino ->
                adapter.moverSelecionados(destino, () -> {
                    adapter.sairModoSelecao();
                    avisar(ProdutoUtils.contagem(quantos, "produto movido", "produtos movidos")
                            + " para " + SeletorCategoria.rotulo(destino));
                }));
    }

    private void confirmarExclusao() {
        final int quantos = adapter.getQuantidadeSelecionada();

        AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
                .setTitle(quantos == 1 ? "Excluir produto?" : "Excluir " + quantos + " produtos?")
                .setMessage("Não dá para desfazer depois.")
                .setPositiveButton("Excluir", (d, w) -> adapter.excluirSelecionados(() -> {
                    adapter.sairModoSelecao();
                    avisar(ProdutoUtils.contagem(quantos, "produto excluído", "produtos excluídos"));
                }))
                .setNegativeButton("Cancelar", null)
                .show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(ContextCompat.getColor(activity, R.color.vermelho_oferta));
    }

    private void avisar(String texto) {
        Snackbar aviso = Snackbar.make(activity.findViewById(android.R.id.content), texto, Snackbar.LENGTH_LONG);
        if (ancoraAvisos != null && ancoraAvisos.getVisibility() == View.VISIBLE) {
            aviso.setAnchorView(ancoraAvisos);
        }
        aviso.show();
    }
}
