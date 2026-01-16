package com.example.paiv2.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.example.paiv2.dao.ProdutoDao;
import com.example.paiv2.entity.Produto;

@Database(entities = {Produto.class}, version = 1)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {
    public abstract ProdutoDao produtoDao();

    private static AppDatabase INSTANCE;

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = Room.databaseBuilder(
                    context.getApplicationContext(),
                    AppDatabase.class,
                    "paiv2_db"
            ).allowMainThreadQueries().build();
        }
        return INSTANCE;
    }
}
