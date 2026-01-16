package com.example.paiv2.database;

import androidx.room.TypeConverter;

import com.example.paiv2.entity.Categoria;

public class Converters {

    @TypeConverter
    public static String fromCategoria(Categoria categoria) {
        return categoria == null ? null : categoria.name();
    }

    @TypeConverter
    public static Categoria toCategoria(String value) {
        return value == null ? null : Categoria.valueOf(value);
    }
}
