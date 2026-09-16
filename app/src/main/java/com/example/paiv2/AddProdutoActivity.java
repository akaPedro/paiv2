package com.example.paiv2;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class AddProdutoActivity extends AppCompatActivity {

    private EditText edtNome;
    private ImageView imgProduto;
    private Button btnSalvar;
    private MaterialButton btnCategoria;

    private List<Uri> imagensSelecionadas;

    private Categoria categoria;

    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_addproduto);

        // A tela de origem sugere a categoria, mas o usuário pode trocar antes de salvar
        String categoriaStr = getIntent().getStringExtra(CategoriaActivity.EXTRA_CATEGORIA);
        categoria = categoriaStr == null ? Categoria.OUTROS : Categoria.valueOf(categoriaStr);

        edtNome = findViewById(R.id.edtNomeProduto);
        imgProduto = findViewById(R.id.imgSelecionarProduto);
        btnSalvar = findViewById(R.id.btnSalvarProduto);
        btnCategoria = findViewById(R.id.btnCategoriaProduto);

        db = AppDatabase.getInstance(this);

        btnCategoria.setText(SeletorCategoria.rotulo(categoria));
        btnCategoria.setOnClickListener(v ->
                SeletorCategoria.mostrar(this, categoria, escolhida -> {
                    categoria = escolhida;
                    btnCategoria.setText(SeletorCategoria.rotulo(escolhida));
                }));

        imgProduto.setOnClickListener(v -> abrirGaleria());
        btnSalvar.setOnClickListener(v -> salvarProduto());
    }

    private String copiarParaInterno(Uri uri, int indice) {
        Bitmap bitmap = null;
        Bitmap redimensionado = null;
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            bitmap = BitmapFactory.decodeStream(is);
            if (is != null) is.close();

            // Imagem corrompida ou formato não suportado
            if (bitmap == null) return null;

            float aspect = (float) bitmap.getWidth() / bitmap.getHeight();
            redimensionado = Bitmap.createScaledBitmap(bitmap, 800, (int)(800/aspect), true);

            String nomeArquivo = "img_" + System.currentTimeMillis() + "_" + indice + ".jpg";
            File arquivoDestino = new File(getFilesDir(), nomeArquivo);
            FileOutputStream os = new FileOutputStream(arquivoDestino);

            redimensionado.compress(Bitmap.CompressFormat.JPEG, 80, os); // 80% é mais leve para lotes grandes

            os.close();

            // --- ISSO AQUI É O SEGREDO PARA GRANDES QUANTIDADES ---
            bitmap.recycle();
            redimensionado.recycle();
            // -------------------------------------------------------

            return arquivoDestino.getAbsolutePath();
        } catch (Exception e) {
            // Se der erro, tenta liberar a memória mesmo assim
            if (bitmap != null) bitmap.recycle();
            if (redimensionado != null) redimensionado.recycle();
            return null;
        }
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

                            imagensSelecionadas = new ArrayList<>();

                            // MÚLTIPLAS IMAGENS
                            if (result.getData().getClipData() != null) {
                                int count = result.getData().getClipData().getItemCount();

                                for (int i = 0; i < count; i++) {
                                    Uri uri = result.getData().getClipData().getItemAt(i).getUri();
                                    imagensSelecionadas.add(uri);

                                    getContentResolver().takePersistableUriPermission(
                                            uri,
                                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    );
                                }

                                // Mostra a primeira imagem só como preview
                                imgProduto.setImageURI(imagensSelecionadas.get(0));

                            } else if (result.getData().getData() != null) {
                                // Caso o usuário selecione só uma
                                Uri uri = result.getData().getData();
                                imagensSelecionadas.add(uri);

                                getContentResolver().takePersistableUriPermission(
                                        uri,
                                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                                );

                                imgProduto.setImageURI(uri);
                            }
                        }
                    }
            );



    private void salvarProduto() {
        // 1. Pega o nome. Se estiver vazio, usaremos um padrão.
        final String nomeDigitado = edtNome.getText().toString().trim();

        if (imagensSelecionadas == null || imagensSelecionadas.isEmpty()) {
            Toast.makeText(this, "Selecione as imagens primeiro", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSalvar.setEnabled(false);
        btnSalvar.setText("Processando lote...");
        btnCategoria.setEnabled(false);

        // Congela a categoria escolhida no momento do clique
        final Categoria categoriaEscolhida = categoria;

        new Thread(() -> {
            try {
                int contador = 1;

                // Definimos o prefixo: Se o usuário não digitou nada, usamos o nome da categoria
                String prefixo = nomeDigitado.isEmpty() ? categoriaEscolhida.name() : nomeDigitado;

                for (Uri uri : imagensSelecionadas) {
                    // Copia e redimensiona
                    String caminhoSeguro = copiarParaInterno(uri, contador);

                    if (caminhoSeguro != null) {
                        // Nome automático: "Cerveja 1", "Cerveja 2", etc.
                        String nomeFinal = prefixo + " " + contador;

                        Produto produto = new Produto(
                                nomeFinal,
                                caminhoSeguro,
                                categoriaEscolhida
                        );
                        db.produtoDao().inserir(produto);
                    }

                    // Feedback visual no log ou progresso (opcional)
                    contador++;
                }

                int finalContador = contador;
                runOnUiThread(() -> {
                    Toast.makeText(this, (finalContador - 1) + " itens adicionados!", Toast.LENGTH_SHORT).show();
                    finish();
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    btnSalvar.setEnabled(true);
                    btnSalvar.setText("SALVAR PRODUTO");
                    btnCategoria.setEnabled(true);
                    Toast.makeText(this, "Erro ao processar lote grande", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

}

