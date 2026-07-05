package com.example.paiv2;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.paiv2.database.AppDatabase;
import com.example.paiv2.entity.Categoria;
import com.example.paiv2.entity.Produto;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ProdutoAdapter adapter;
    private AppDatabase db;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Referências do Layout
        EditText edtPesquisar = findViewById(R.id.edtPesquisar);
        ScrollView scrollCategorias = findViewById(R.id.scrollCategorias);
        RecyclerView recycler = findViewById(R.id.recyclerPesquisa);

        // Configuração do Banco e Adapter
        adapter = new ProdutoAdapter(this, new ArrayList<>());
        db = AppDatabase.getInstance(this);

        recycler.setLayoutManager(new GridLayoutManager(this, 2));
        recycler.setAdapter(adapter);

        // IDs dos Botões conforme seu XML
        Button BAlim = findViewById(R.id.btnAlimentos);
        Button BBebi = findViewById(R.id.btnBebidas);
        Button BHili = findViewById(R.id.btnHigiene);
        Button BOut = findViewById(R.id.btnOutros);
        Button BDoc = findViewById(R.id.btnDoces);

        // Listeners dos Botões: todas as categorias abrem a mesma tela.
        // A Main não é finalizada, então o botão "voltar" retorna para cá naturalmente.
        BAlim.setOnClickListener(v -> abrirCategoria(Categoria.ALIMENTOS));
        BBebi.setOnClickListener(v -> abrirCategoria(Categoria.BEBIDAS));
        BHili.setOnClickListener(v -> abrirCategoria(Categoria.HIGIENE));
        BOut.setOnClickListener(v -> abrirCategoria(Categoria.OUTROS));
        BDoc.setOnClickListener(v -> abrirCategoria(Categoria.DOCES));

        // Lógica de Pesquisa e Visibilidade
        edtPesquisar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String texto = s.toString().trim();

                if (texto.isEmpty()) {
                    // Se estiver vazio, mostra as categorias e esconde a lista
                    recycler.setVisibility(View.GONE);
                    scrollCategorias.setVisibility(View.VISIBLE);
                } else {
                    // Se tiver texto, esconde categorias e mostra resultados
                    scrollCategorias.setVisibility(View.GONE);
                    recycler.setVisibility(View.VISIBLE);
                    buscarProdutos(texto);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Correção do EdgeToEdge: Usando scrollCategorias como referência de View
        // Para evitar o crash, o findViewById deve encontrar uma View existente.
        ViewCompat.setOnApplyWindowInsetsListener(scrollCategorias, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), systemBars.top, v.getPaddingRight(), systemBars.bottom);
            return insets;
        });
    }

    private void abrirCategoria(Categoria categoria) {
        startActivity(CategoriaActivity.novaIntent(this, categoria));
    }

    private void buscarProdutos(String texto) {
        new Thread(() -> {
            List<Produto> lista = db.produtoDao().buscarPorNome(texto);
            runOnUiThread(() -> adapter.atualizarLista(lista));
        }).start();
    }
}