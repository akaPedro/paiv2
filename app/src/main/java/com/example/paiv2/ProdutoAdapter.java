package com.example.paiv2;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.paiv2.entity.Categoria;
import com.example.paiv2.entity.Produto;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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

    /** Avisa a tela se o modo de seleção está ativo e quantos produtos estão marcados. */
    public interface AoMudarSelecao {
        void mudou(boolean ativo, int quantidade);
    }

    private final List<Produto> listaProdutos;
    private final Context context;
    private final Handler main = new Handler(Looper.getMainLooper());
    private AoAlterarProdutos ouvinte;
    private AoMudarSelecao ouvinteSelecao;

    // Guardamos ids, não posições: a lista é recarregada o tempo todo
    private final Set<Integer> selecionados = new LinkedHashSet<>();
    private boolean modoSelecao = false;
    // Na escolha de produtos da promoção a lista muda a cada busca, e o que já
    // foi marcado precisa continuar marcado mesmo saindo da tela
    private boolean podarSelecaoAoAtualizar = true;

    // Posição de cada cabeçalho -> quantos produtos há na seção dele
    private final Map<Integer, Integer> quantidadePorSecao = new HashMap<>();

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

    public void setPodarSelecaoAoAtualizar(boolean podar) {
        this.podarSelecaoAoAtualizar = podar;
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
            ligarDivisor((DivisorViewHolder) holder, produto, position);
        } else {
            ligarProduto((ProdutoViewHolder) holder, produto);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position,
                                 @NonNull List<Object> payloads) {
        // Só mudou a marcação: não recarrega a imagem, que piscaria
        if (!payloads.isEmpty() && holder instanceof ProdutoViewHolder) {
            marcarSelecao((ProdutoViewHolder) holder, listaProdutos.get(position));
            return;
        }
        super.onBindViewHolder(holder, position, payloads);
    }

    private void ligarDivisor(DivisorViewHolder holder, Produto divisor, int position) {
        String titulo = divisor.getNome();
        // Alcoólicas em vermelho, como aviso; as demais seções na cor da categoria
        int cor = ProdutoUtils.TITULO_ALCOOLICAS.equals(titulo)
                ? ContextCompat.getColor(context, R.color.vermelho_oferta)
                : EstiloCategoria.cor(context, divisor.getCategoria());

        holder.txtTitulo.setText(titulo);
        holder.marcador.setBackgroundTintList(ColorStateList.valueOf(cor));

        Integer quantidade = quantidadePorSecao.get(position);
        holder.txtQuantidade.setVisibility(quantidade == null ? View.GONE : View.VISIBLE);
        if (quantidade != null) holder.txtQuantidade.setText(ProdutoUtils.numero(quantidade));
    }

    private void ligarProduto(ProdutoViewHolder holder, Produto produto) {
        boolean semNome = ProdutoUtils.semNome(produto);
        holder.txtNome.setText(produto.getNome());
        // Nome provisório fica apagado: mostra que ainda falta dar nome
        holder.txtNome.setTextColor(ContextCompat.getColor(context,
                semNome ? R.color.texto_secundario : R.color.texto_primario));
        holder.txtNome.setTypeface(null, semNome ? Typeface.NORMAL : Typeface.BOLD);
        holder.selo18.setVisibility(ProdutoUtils.isAlcoolica(produto) ? View.VISIBLE : View.GONE);

        carregarImagem(holder.imgProduto, produto.getImageUri());
        marcarSelecao(holder, produto);

        // Fora da seleção, o toque abre o produto; dentro dela, marca e desmarca
        holder.card.setOnClickListener(v -> {
            if (modoSelecao) {
                alternarSelecao(produto);
            } else {
                ImagemProdutoActivity.abrir(context, produtos(), produto);
            }
        });

        // Segurar começa a seleção; num produto já marcado, não desmarca
        holder.card.setOnLongClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            modoSelecao = true;
            if (selecionados.contains(produto.getId())) {
                avisarSelecao();
            } else {
                alternarSelecao(produto);
            }
            return true;
        });
    }

    private void marcarSelecao(ProdutoViewHolder holder, Produto produto) {
        holder.card.setChecked(selecionados.contains(produto.getId()));
    }

    private void carregarImagem(ImageView alvo, String uri) {
        Glide.with(context).clear(alvo);

        Object modelo = ImagemUtils.modelo(uri);
        if (modelo == null) {
            alvo.setImageResource(R.drawable.default_image);
            return;
        }

        Glide.with(context)
                .load(modelo)
                .override(320, 320)
                .fitCenter()
                .thumbnail(0.1f)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.placeholder_img)
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
        recontarSecoes();

        // Produtos excluídos ou que saíram desta tela não podem continuar marcados
        if (podarSelecaoAoAtualizar && !selecionados.isEmpty()) {
            Set<Integer> presentes = new HashSet<>();
            for (Produto p : listaProdutos) {
                if (!ProdutoUtils.isDivisor(p)) presentes.add(p.getId());
            }
            if (selecionados.retainAll(presentes)) avisarSelecao();
        }

        notifyDataSetChanged();
    }

    private void recontarSecoes() {
        quantidadePorSecao.clear();
        int cabecalho = -1;
        for (int i = 0; i < listaProdutos.size(); i++) {
            if (ProdutoUtils.isDivisor(listaProdutos.get(i))) {
                cabecalho = i;
                quantidadePorSecao.put(i, 0);
            } else if (cabecalho >= 0) {
                quantidadePorSecao.put(cabecalho, quantidadePorSecao.get(cabecalho) + 1);
            }
        }
    }

    /** Os produtos da lista, sem os cabeçalhos de seção. */
    public List<Produto> produtos() {
        List<Produto> produtos = new ArrayList<>();
        for (Produto p : listaProdutos) {
            if (!ProdutoUtils.isDivisor(p)) produtos.add(p);
        }
        return produtos;
    }

    public boolean isDivisor(int position) {
        if (position >= 0 && position < listaProdutos.size()) {
            return ProdutoUtils.isDivisor(listaProdutos.get(position));
        }
        return false;
    }

    static class ProdutoViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView card;
        final ImageView imgProduto;
        final TextView txtNome;
        final TextView selo18;

        ProdutoViewHolder(@NonNull View itemView) {
            super(itemView);
            card = (MaterialCardView) itemView;
            // O selo de marcado já tem as próprias cores (círculo azul, visto branco)
            card.setCheckedIconTint(null);
            imgProduto = itemView.findViewById(R.id.imgProduto);
            txtNome = itemView.findViewById(R.id.txtNomeProduto);
            selo18 = itemView.findViewById(R.id.selo18);
        }
    }

    static class DivisorViewHolder extends RecyclerView.ViewHolder {
        final View marcador;
        final TextView txtTitulo;
        final TextView txtQuantidade;

        DivisorViewHolder(@NonNull View itemView) {
            super(itemView);
            marcador = itemView.findViewById(R.id.marcadorSecao);
            txtTitulo = itemView.findViewById(R.id.txtTituloSecao);
            txtQuantidade = itemView.findViewById(R.id.txtQuantidadeSecao);
        }
    }

    // --- SELEÇÃO ---

    /** Entra na seleção sem marcar nada, pelo botão "Selecionar". */
    public void entrarModoSelecao() {
        if (modoSelecao) return;
        modoSelecao = true;
        avisarSelecao();
    }

    public void sairModoSelecao() {
        if (!modoSelecao) return;
        modoSelecao = false;

        // Repinta só os cards que estavam marcados, para nenhuma imagem recarregar
        List<Integer> posicoes = new ArrayList<>();
        for (int i = 0; i < listaProdutos.size(); i++) {
            if (selecionados.contains(listaProdutos.get(i).getId())) posicoes.add(i);
        }
        selecionados.clear();
        for (int pos : posicoes) notifyItemChanged(pos, PAYLOAD_SELECAO);

        avisarSelecao();
    }

    private void alternarSelecao(Produto produto) {
        Integer id = produto.getId();
        if (!selecionados.remove(id)) {
            selecionados.add(id);
        }
        int pos = listaProdutos.indexOf(produto);
        if (pos >= 0) notifyItemChanged(pos, PAYLOAD_SELECAO);
        avisarSelecao();
    }

    public boolean todosSelecionados() {
        int total = 0;
        for (Produto p : listaProdutos) {
            if (!ProdutoUtils.isDivisor(p)) total++;
        }
        return total > 0 && selecionados.size() == total;
    }

    /** Marca todos; se já estavam todos marcados, desmarca todos. */
    public void selecionarTodos() {
        if (todosSelecionados()) {
            selecionados.clear();
        } else {
            for (Produto p : listaProdutos) {
                if (!ProdutoUtils.isDivisor(p)) selecionados.add(p.getId());
            }
        }
        notifyItemRangeChanged(0, listaProdutos.size(), PAYLOAD_SELECAO);
        avisarSelecao();
    }

    private void avisarSelecao() {
        if (ouvinteSelecao != null) ouvinteSelecao.mudou(modoSelecao, selecionados.size());
    }

    public int getQuantidadeSelecionada() {
        return selecionados.size();
    }

    /** Ids marcados, inclusive os que a busca tirou da lista atual. */
    public List<Integer> getIdsSelecionados() {
        return new ArrayList<>(selecionados);
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

    /** Move os produtos marcados para outra categoria. */
    public void moverSelecionados(Categoria destino, Runnable aoTerminar) {
        AcoesProduto.mover(context, getSelecionados(), destino, () -> {
            aoTerminar.run();
            avisarEdicao();
        });
    }

    /** Exclui os produtos marcados. */
    public void excluirSelecionados(Runnable aoTerminar) {
        AcoesProduto.excluir(context, getSelecionados(), () -> {
            aoTerminar.run();
            avisarEdicao();
        });
    }

    /** Coloca os produtos marcados na promoção. */
    public void colocarSelecionadosEmPromocao() {
        Promocao.adicionar(context, getIdsSelecionados());
    }

    /** Abre o diálogo de edição do único produto marcado. */
    public void editarUnicoSelecionado() {
        List<Produto> marcados = getSelecionados();
        if (marcados.size() == 1) EditorProduto.abrir(context, marcados.get(0), this::avisarEdicao);
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
