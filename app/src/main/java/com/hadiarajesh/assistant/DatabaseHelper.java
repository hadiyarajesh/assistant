package com.hadiarajesh.assistant;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Rajesh on 12-04-2018.
 */

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME="CONTACTDB";
    private static final int DATABASE_VERSION=1;

    private static final String CREATE_QUERY_CONTACTS="CREATE TABLE IF NOT EXISTS table_contact" + "(_id INTEGER PRIMARY KEY AUTOINCREMENT,"+ "name TEXT COLLATE NOCASE,"+ "number TEXT);";

    public DatabaseHelper(Context context)
    {
        super(context,DATABASE_NAME,null,DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_QUERY_CONTACTS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS table_contact");
    }

    public void addContact(String name,String number, SQLiteDatabase database)
    {
        ContentValues contentValues=new ContentValues();
        contentValues.put("name",name);
        contentValues.put("number",number);
        database.insert("table_contact",null,contentValues);
    }
}
