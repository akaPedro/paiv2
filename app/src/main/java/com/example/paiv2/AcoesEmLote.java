package com.example.paiv2;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;

import com.example.paiv2.entity.Categoria;
import com.example.paiv2.entity.Produto;

import java.util.List;

/**
 * Barra que aparece no topo quando há produtos marcados, com as ações de
 * editar (um só), mover de categoria e excluir em lote.
 */
public class AcoesEmLote implements ActionMode.Callback {

    private final AppCompatActivity activity;
    private final ProdutoAdapter adapter;
    // Fica escondida durante a seleção (o botão "+"): tocá-la sairia da tela
    // e perderia o que já foi marcado
    private final View esconderDuranteSelecao;
    private ActionMode modo;

    private AcoesEmLote(AppCompatActivity activity, ProdutoAdapter adapter, View esconderDuranteSelecao) {
        this.activity = activity;
        this.adapter = adapter;
        this.esconderDuranteSelecao = esconderDuranteSelecao;
    }

    public static void instalar(AppCompatActivity activity, ProdutoAdapter adapter) {
        instalar(activity, adapter, null);
    }

    public static void instalar(AppCompatActivity activity, ProdutoAdapter adapter, View esconderDuranteSelecao) {
        AcoesEmLote acoes = new AcoesEmLote(activity, adapter, esconderDuranteSelecao);
        adapter.setAoMudarSelecao(acoes::aoMudarSelecao);
    }

    private void aoMudarSelecao(int quantidade) {
        if (quantidade == 0) {
            fechar();
            return;
        }
        if (modo == null) modo = activity.startSupportActionMode(this);
        if (modo == null) return;

        if (esconderDuranteSelecao != null) esconderDuranteSelecao.setVisibility(View.GONE);

        modo.setTitle(quantidade + (quantidade == 1 ? " selecionado" : " selecionados"));
        // Mostra ou esconde o "Editar" conforme a quantidade
        modo.invalidate();
    }

    private void fechar() {
        if (modo == null) return;
        ActionMode aFechar = modo;
        // Zera antes de finalizar para não voltar aqui pelo onDestroyActionMode
        modo = null;
        aFechar.finish();
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mode.getMenuInflater().inflate(R.menu.menu_selecao, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        // Editar só faz sentido com um produto marcado
        menu.findItem(R.id.acao_editar).setVisible(adapter.getQuantidadeSelecionada() == 1);
        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.acao_editar) {
            adapter.editarUnicoSelecionado();
            fechar();
            return true;
        }
        if (id == R.id.acao_mover) {
            escolherDestino();
            return true;
        }
        if (id == R.id.acao_excluir) {
            confirmarExclusao();
            return true;
        }
        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        modo = null;
        if (esconderDuranteSelecao != null) esconderDuranteSelecao.setVisibility(View.VISIBLE);
        adapter.sairModoSelecao();
    }

    private void escolherDestino() {
        final int quantos = adapter.getQuantidadeSelecionada();

        // Com um só marcado, a lista já vem na categoria atual dele
        List<Produto> marcados = adapter.getSelecionados();
        Categoria atual = marcados.size() == 1 ? marcados.get(0).getCategoria() : null;

        SeletorCategoria.mostrar(activity, atual, destino ->
                adapter.moverSelecionados(destino, () -> {
                    Toast.makeText(activity,
                            quantos + (quantos == 1 ? " produto movido para " : " produtos movidos para ")
                                    + SeletorCategoria.rotulo(destino),
                            Toast.LENGTH_SHORT).show();
                    fechar();
                }));
    }

    private void confirmarExclusao() {
        final int quantos = adapter.getQuantidadeSelecionada();

        new AlertDialog.Builder(activity)
                .setTitle("Excluir produtos")
                .setMessage(quantos == 1
                        ? "Excluir o produto selecionado?"
                        : "Excluir os " + quantos + " produtos selecionados?")
                .setPositiveButton("Excluir", (d, w) -> adapter.excluirSelecionados(() -> {
                    Toast.makeText(activity,
                            quantos + (quantos == 1 ? " produto excluído" : " produtos excluídos"),
                            Toast.LENGTH_SHORT).show();
                    fechar();
                }))
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
