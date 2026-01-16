package com.example.paiv2.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import org.jspecify.annotations.NonNull;

@Entity(tableName = "produtos")
public class Produto {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String nome;
    @NonNull
    private Categoria categoria;
    private String imageUri;

    // 🔹 CONSTRUTOR (sem id)
    public Produto(String nome, String imageUri, Categoria categoria) {
        this.nome = nome;
        this.imageUri = imageUri;
        this.categoria = categoria;
    }

    public Produto() {

    }

    // 🔹 GETTERS
    public int getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public String getImageUri() {
        return imageUri;
    }

    // 🔹 SETTERS (ESSENCIAL PARA O ROOM)
    public void setId(int id) {
        this.id = id;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }

    public void setImageUri(String imageUri) {
        this.imageUri = imageUri;
    }
}
