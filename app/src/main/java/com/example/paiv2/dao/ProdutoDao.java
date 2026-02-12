package com.example.paiv2.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.paiv2.entity.Categoria;
import com.example.paiv2.entity.Produto;

import java.util.List;

@Dao
public interface ProdutoDao {

    @Insert
    void inserir(Produto produto);

    @Query("""
    SELECT * FROM produtos
    WHERE categoria = :categoria
    ORDER BY nome COLLATE NOCASE ASC
""")
    List<Produto> listarPorCategoriaOrdenado(Categoria categoria);



    @Query("SELECT * FROM produtos WHERE categoria = :categoria ORDER BY nome COLLATE NOCASE ASC")
    List<Produto> listarPorCategoria(Categoria categoria);

    @Query("SELECT * FROM produtos ORDER BY nome COLLATE NOCASE ASC")
    List<Produto> listarTodos();
    @Delete
    void deletar(Produto produto);

    @Update
    void atualizar(Produto produto);

    @Query("SELECT COUNT(*) FROM produtos WHERE categoria = :categoria")
    int contarPorCategoria(Categoria categoria);

    @Query("SELECT * FROM produtos WHERE nome LIKE '%' || :texto || '%' ORDER BY nome ASC")
    List<Produto> buscarPorNome(String texto);

}

