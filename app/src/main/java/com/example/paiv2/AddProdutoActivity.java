package com.example.paiv2;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.paiv2.database.AppDatabase;
import com.example.paiv2.entity.Categoria;
import com.example.paiv2.entity.Produto;

import java.util.Calendar;
import java.util.List;

public class AddProdutoActivity extends AppCompatActivity {

    private EditText edtNome;
    private ImageView imgProduto;
    private Button btnSalvar;

    private Uri imagemSelecionada;
    private Categoria categoria;

    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_addproduto);

        String categoriaStr = getIntent().getStringExtra("categoria");
        if (categoriaStr != null) {
            categoria = Categoria.valueOf(categoriaStr);
        }

        Toast.makeText(this,
                "Categoria recebida: " + categoria,
                Toast.LENGTH_LONG).show();

        edtNome = findViewById(R.id.edtNomeProduto);
        imgProduto = findViewById(R.id.imgSelecionarProduto);
        btnSalvar = findViewById(R.id.btnSalvarProduto);

        db = AppDatabase.getInstance(this);

        imgProduto.setOnClickListener(v -> abrirGaleria());
        btnSalvar.setOnClickListener(v -> salvarProduto());
    }

    private void abrirGaleria() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT); // OPEN_DOCUMENT é melhor para persistência
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        galeriaLauncher.launch(intent);
    }

    private final ActivityResultLauncher<Intent> galeriaLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            imagemSelecionada = result.getData().getData();

                            // ESSENCIAL: Garante que o app poderá ler essa imagem no futuro
                            final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
                            getContentResolver().takePersistableUriPermission(imagemSelecionada, takeFlags);

                            imgProduto.setImageURI(imagemSelecionada);
                        }
                    }
            );


    private void salvarProduto() {
        String nome = edtNome.getText().toString().trim();

        // Validações básicas
        if (nome.isEmpty()) {
            edtNome.setError("Digite o nome");
            return;
        }
        if (imagemSelecionada == null) {
            Toast.makeText(this, "Selecione uma imagem", Toast.LENGTH_SHORT).show();
            return;
        }

        // Criar o objeto
        Produto produto = new Produto(nome, imagemSelecionada.toString(), categoria);

        // USANDO UMA THREAD PARA NÃO TRAVAR O APP
        new Thread(() -> {
            try {
                db.produtoDao().inserir(produto);

                // Após salvar, volta para a Thread principal para fechar a tela
                runOnUiThread(() -> {
                    Toast.makeText(AddProdutoActivity.this, "Salvo com sucesso!", Toast.LENGTH_SHORT).show();
                    finish(); // Fecha a tela de cadastro
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(AddProdutoActivity.this, "Erro ao salvar", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}

