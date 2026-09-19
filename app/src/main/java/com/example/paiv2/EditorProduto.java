package com.example.paiv2;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

import androidx.appcompat.app.AlertDialog;

import com.example.paiv2.database.AppDatabase;
import com.example.paiv2.entity.Categoria;
import com.example.paiv2.entity.Produto;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Diálogo de editar nome e categoria, usado na grade e no visualizador.
 */
public final class EditorProduto {

    private EditorProduto() {
    }

    public static void abrir(Context context, Produto produto, Runnable aoSalvar) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_editar_produto, null);
        TextInputLayout campo = view.findViewById(R.id.inputLayoutNomeEditar);
        TextInputEditText edtNome = view.findViewById(R.id.edtNomeEditar);
        ChipGroup chips = view.findViewById(R.id.chipsCategoriaEditar);

        edtNome.setText(produto.getNome());
        if (ProdutoUtils.semNome(produto)) {
            // Nome provisório: já vem selecionado, digitar substitui
            edtNome.setSelectAllOnFocus(true);
        } else {
            edtNome.setSelection(edtNome.length());
        }

        // Guarda a escolha até salvar, para "Cancelar" não mudar nada
        final Categoria[] escolhida = {produto.getCategoria()};
        ChipsCategoria.montar(chips, escolhida[0], categoria -> escolhida[0] = categoria);

        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle("Editar produto")
                .setView(view)
                .setPositiveButton("Salvar", null)
                .setNegativeButton("Cancelar", null)
                .create();

        dialog.setOnShowListener(d -> {
            // Botão tratado aqui para o diálogo não fechar quando o nome estiver vazio
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String nome = edtNome.getText() == null ? "" : edtNome.getText().toString().trim();
                if (nome.isEmpty()) {
                    campo.setError("Digite um nome");
                    return;
                }
                produto.setNome(nome);
                produto.setCategoria(escolhida[0]);

                Handler main = new Handler(Looper.getMainLooper());
                new Thread(() -> {
                    AppDatabase.getInstance(context).produtoDao().atualizar(produto);
                    main.post(aoSalvar);
                }).start();
                dialog.dismiss();
            });
            edtNome.requestFocus();
        });

        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
        dialog.show();
    }
}
