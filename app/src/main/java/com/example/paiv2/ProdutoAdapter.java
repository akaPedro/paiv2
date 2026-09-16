package com.example.paiv2;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.paiv2.database.AppDatabase;
import com.example.paiv2.entity.Categoria;
import com.example.paiv2.entity.Produto;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ProdutoAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TIPO_PRODUTO = 0;
    private static final int TIPO_DIVISOR = 1;

    // Marca uma atualização que mexe só na seleção, não no conteúdo do card
    private static final Object PAYLOAD_SELECAO = new Object();

    /** Avisa a tela que os produtos mudaram e a lista precisa ser recarregada. */
    public interface AoAlterarProdutos {
        void aoAlterar();
    }

    /** Avisa a tela quantos produtos estão marcados (0 = sair do modo de seleção). */
    public interface AoMudarSelecao {
        void mudou(int quantidade);
    }

    private final List<Produto> listaProdutos;
    private final Context context;
    private final Handler main = new Handler(Looper.getMainLooper());
    private AoAlterarProdutos ouvinte;
    private AoMudarSelecao ouvinteSelecao;

    // Guardamos ids, não posições: a lista é recarregada o tempo todo
    private final Set<Integer> selecionados = new LinkedHashSet<>();
    private boolean modoSelecao = false;

    public ProdutoAdapter(Context context, List<Produto> listaProdutos) {
        this.context = context;
        this.listaProdutos = new ArrayList<>(listaProdutos);
    }

    public void setAoAlterarProdutos(AoAlterarProdutos ouvinte) {
        this.ouvinte = ouvinte;
    }

    public void setAoMudarSelecao(AoMudarSelecao ouvinteSelecao) {
        this.ouvinteSelecao = ouvinteSelecao;
    }

    @Override
    public int getItemViewType(int position) {
        return ProdutoUtils.isDivisor(listaProdutos.get(position)) ? TIPO_DIVISOR : TIPO_PRODUTO;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        if (viewType == TIPO_DIVISOR) {
            return new DivisorViewHolder(inflater.inflate(R.layout.item_divisor, parent, false));
        }
        return new ProdutoViewHolder(inflater.inflate(R.layout.item_produto, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Produto produto = listaProdutos.get(position);

        if (holder instanceof DivisorViewHolder) {
            ligarDivisor((DivisorViewHolder) holder, produto);
        } else {
            ligarProduto((ProdutoViewHolder) holder, produto);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position,
                                 @NonNull List<Object> payloads) {
        // Só mudou a marcação: repinta o selo sem recarregar a imagem, que piscaria
        if (!payloads.isEmpty() && holder instanceof ProdutoViewHolder) {
            marcarSelecao((ProdutoViewHolder) holder, listaProdutos.get(position));
            return;
        }
        super.onBindViewHolder(holder, position, payloads);
    }

    private void marcarSelecao(ProdutoViewHolder holder, Produto produto) {
        boolean marcado = selecionados.contains(produto.getId());
        holder.selecaoFundo.setVisibility(marcado ? View.VISIBLE : View.GONE);
        holder.selecaoSelo.setVisibility(marcado ? View.VISIBLE : View.GONE);
    }

    private void ligarDivisor(DivisorViewHolder holder, Produto divisor) {
        String titulo = divisor.getNome();
        // Bebida alcoólica continua em vermelho como aviso; os demais títulos em azul
        int cor = ContextCompat.getColor(context,
                "ALCOÓLICAS".equals(titulo) ? R.color.vermelho_oferta : R.color.azul_logo);

        holder.txtTitulo.setText(titulo);
        holder.txtTitulo.setTextColor(cor);
        holder.linha.setBackgroundColor(cor);
    }

    private void ligarProduto(ProdutoViewHolder holder, Produto produto) {
        holder.txtNome.setText(produto.getNome());
        holder.txtNome.setTextColor(ContextCompat.getColor(context,
                ProdutoUtils.isAlcoolica(produto) ? R.color.vermelho_oferta : R.color.azul_logo
        ));

        carregarImagem(holder.imgProduto, produto.getImageUri());

        marcarSelecao(holder, produto);

        // Fora da seleção, o toque abre a imagem grande; dentro dela, marca e desmarca
        holder.itemView.setOnClickListener(v -> {
            if (modoSelecao) {
                alternarSelecao(produto);
                return;
            }
            String path = produto.getImageUri();
            if (path == null || path.isEmpty()) return;

            Intent intent = new Intent(context, ImagemProdutoActivity.class);
            intent.putExtra("imageUri", path);
            context.startActivity(intent);
        });

        // Apertar e segurar começa a seleção
        holder.itemView.setOnLongClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            modoSelecao = true;
            alternarSelecao(produto);
            return true;
        });
    }

    private void carregarImagem(ImageView alvo, String uriString) {
        Glide.with(context).clear(alvo);

        if (uriString == null || uriString.isEmpty()) {
            alvo.setImageResource(R.drawable.default_image);
            return;
        }

        Object model;
        if (uriString.startsWith("/")) {
            // Caminho Interno (Cópia da Galeria) - PRIORIDADE
            model = new File(uriString);
        } else if (uriString.startsWith("content://") || uriString.startsWith("file://")) {
            // URI Direta da Galeria (Legado)
            model = Uri.parse(uriString);
        } else {
            // Assets (Caminho relativo)
            model = "file:///android_asset/" + uriString;
        }

        Glide.with(context)
                .load(model)
                .override(320, 320)
                .fitCenter()
                .thumbnail(0.1f)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.default_image)
                .error(R.drawable.default_image)
                .into(alvo);
    }

    @Override
    public int getItemCount() {
        return listaProdutos.size();
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);
        if (holder instanceof ProdutoViewHolder) {
            Glide.with(context).clear(((ProdutoViewHolder) holder).imgProduto);
        }
    }

    public void atualizarLista(List<Produto> novaLista) {
        listaProdutos.clear();
        listaProdutos.addAll(novaLista);

        // Produtos excluídos ou que saíram desta tela não podem continuar marcados
        if (!selecionados.isEmpty()) {
            Set<Integer> presentes = new LinkedHashSet<>();
            for (Produto p : listaProdutos) presentes.add(p.getId());
            boolean mudou = selecionados.retainAll(presentes);
            if (mudou) avisarSelecao();
        }

        notifyDataSetChanged();
    }

    public boolean isDivisor(int position) {
        if (position >= 0 && position < listaProdutos.size()) {
            return ProdutoUtils.isDivisor(listaProdutos.get(position));
        }
        return false;
    }

    static class ProdutoViewHolder extends RecyclerView.ViewHolder {
        final ImageView imgProduto;
        final TextView txtNome;
        final View selecaoFundo;
        final ImageView selecaoSelo;

        ProdutoViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProduto = itemView.findViewById(R.id.imgProduto);
            txtNome = itemView.findViewById(R.id.txtNomeProduto);
            selecaoFundo = itemView.findViewById(R.id.selecaoFundo);
            selecaoSelo = itemView.findViewById(R.id.selecaoSelo);
        }
    }

    static class DivisorViewHolder extends RecyclerView.ViewHolder {
        final TextView txtTitulo;
        final View linha;

        DivisorViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTitulo = itemView.findViewById(R.id.txtTituloSecao);
            linha = itemView.findViewById(R.id.linhaSecao);
        }
    }

    // --- SELEÇÃO ---

    private void alternarSelecao(Produto produto) {
        Integer id = produto.getId();
        if (!selecionados.remove(id)) {
            selecionados.add(id);
        }
        int pos = listaProdutos.indexOf(produto);
        if (pos >= 0) notifyItemChanged(pos, PAYLOAD_SELECAO);
        avisarSelecao();
    }

    private void avisarSelecao() {
        if (ouvinteSelecao != null) ouvinteSelecao.mudou(selecionados.size());
    }

    public int getQuantidadeSelecionada() {
        return selecionados.size();
    }

    public List<Produto> getSelecionados() {
        List<Produto> marcados = new ArrayList<>();
        for (Produto p : listaProdutos) {
            if (!ProdutoUtils.isDivisor(p) && selecionados.contains(p.getId())) {
                marcados.add(p);
            }
        }
        return marcados;
    }

    /** Sai do modo de seleção sem avisar a tela: quem chama já sabe que acabou. */
    public void sairModoSelecao() {
        modoSelecao = false;
        if (selecionados.isEmpty()) return;

        // Repinta só os cards que estavam marcados, para nenhuma imagem recarregar
        List<Integer> posicoes = new ArrayList<>();
        for (int i = 0; i < listaProdutos.size(); i++) {
            if (selecionados.contains(listaProdutos.get(i).getId())) posicoes.add(i);
        }
        selecionados.clear();
        for (int pos : posicoes) notifyItemChanged(pos, PAYLOAD_SELECAO);
    }

    /** Move os produtos marcados para outra categoria, tudo em uma transação. */
    public void moverSelecionados(Categoria destino, Runnable aoTerminar) {
        List<Produto> alvos = getSelecionados();
        if (alvos.isEmpty()) {
            aoTerminar.run();
            return;
        }

        AppDatabase db = AppDatabase.getInstance(context);
        new Thread(() -> {
            db.runInTransaction(() -> {
                for (Produto p : alvos) {
                    p.setCategoria(destino);
                    db.produtoDao().atualizar(p);
                }
            });
            main.post(() -> {
                aoTerminar.run();
                avisarEdicao();
            });
        }).start();
    }

    /** Exclui os produtos marcados, tudo em uma transação. */
    public void excluirSelecionados(Runnable aoTerminar) {
        List<Produto> alvos = getSelecionados();
        if (alvos.isEmpty()) {
            aoTerminar.run();
            return;
        }

        AppDatabase db = AppDatabase.getInstance(context);
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

            main.post(() -> {
                aoTerminar.run();
                avisarEdicao();
            });
        }).start();
    }

    /** Abre o diálogo de edição do único produto marcado. */
    public void editarUnicoSelecionado() {
        List<Produto> marcados = getSelecionados();
        if (marcados.size() == 1) editarProduto(marcados.get(0));
    }

    // --- MENU E EDIÇÃO ---

    private void editarProduto(Produto produto) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_editar_produto, null);
        TextInputEditText edtNome = view.findViewById(R.id.edtNomeEditar);
        MaterialButton btnCategoria = view.findViewById(R.id.btnCategoriaEditar);

        edtNome.setText(produto.getNome());
        edtNome.setSelection(edtNome.getText() == null ? 0 : edtNome.getText().length());

        // Guarda a escolha até o usuário salvar, para "Cancelar" não mudar nada
        final Categoria[] categoriaEscolhida = {produto.getCategoria()};
        btnCategoria.setText(SeletorCategoria.rotulo(categoriaEscolhida[0]));
        btnCategoria.setOnClickListener(v ->
                SeletorCategoria.mostrar(context, categoriaEscolhida[0], escolhida -> {
                    categoriaEscolhida[0] = escolhida;
                    btnCategoria.setText(SeletorCategoria.rotulo(escolhida));
                }));

        new AlertDialog.Builder(context)
                .setTitle("Editar produto")
                .setView(view)
                .setPositiveButton("Salvar", (dialog, which) -> {
                    String novoNome = edtNome.getText() == null ? "" : edtNome.getText().toString().trim();
                    if (novoNome.isEmpty()) return;

                    produto.setNome(novoNome);
                    produto.setCategoria(categoriaEscolhida[0]);
                    new Thread(() -> {
                        AppDatabase.getInstance(context).produtoDao().atualizar(produto);
                        main.post(this::avisarEdicao);
                    }).start();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // A tela recarrega a lista do banco; sem ouvinte, redesenha o que está em memória.

    private void avisarEdicao() {
        if (ouvinte != null) {
            ouvinte.aoAlterar();
        } else {
            notifyDataSetChanged();
        }
    }
}
