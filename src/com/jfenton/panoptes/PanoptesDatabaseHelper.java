package com.jfenton.panoptes;

import android.content.Context;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.provider.BaseColumns;
import android.util.Log;

public class PanoptesDatabaseHelper extends SQLiteOpenHelper {

	public PanoptesDatabaseHelper(Context context) {
		super(context, "Panoptes", null, 1);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE IF NOT EXISTS cards (cardId INTEGER PRIMARY KEY AUTOINCREMENT, fromPeriod TEXT, toPeriod TEXT, lastSynchronised INTEGER)");
		db.execSQL("CREATE TABLE IF NOT EXISTS datums (datumId INTEGER PRIMARY KEY AUTOINCREMENT, card INTEGER, key TEXT, value TEXT, FOREIGN KEY(card) REFERENCES cards(cardId))");
		db.execSQL("INSERT INTO cards (fromPeriod, toPeriod) VALUES ('2011-11-27 01:46:32.123', '2011-11-27 01:46:45.412')");
		db.execSQL("INSERT INTO datums (card, key, value) VALUES (last_insert_rowid(), 'rssi', '6')");
		db.execSQL("INSERT INTO datums (card, key, value) VALUES (last_insert_rowid(), 'eclo', '4')");
		db.execSQL("INSERT INTO datums (card, key, value) VALUES (last_insert_rowid(), 'loc', '51.30,0.7')");
		db.execSQL("INSERT INTO cards (fromPeriod, toPeriod) VALUES ('2011-11-28 02:32:31.143', '2011-11-28 04:32:22.416')");
		db.execSQL("INSERT INTO datums (card, key, value) VALUES (last_insert_rowid(), 'rssi', '15')");
		db.execSQL("INSERT INTO datums (card, key, value) VALUES (last_insert_rowid(), 'eclo', '2')");
		db.execSQL("INSERT INTO datums (card, key, value) VALUES (last_insert_rowid(), 'loc', '41.30,0.2')");
	}

	@Override
	public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {
		Log.e("Database", "onUpgrade not implemented");
	}

}
