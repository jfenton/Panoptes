package com.jfenton.panoptes;

import android.database.Cursor;
import android.database.sqlite.SQLiteCursorDriver;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteQuery;

public class PanoptesCardCursorFactory implements CursorFactory {
	public Cursor newCursor(SQLiteDatabase db, SQLiteCursorDriver masterQuery, String editTable, SQLiteQuery query) {
		return new PanoptesCardCursor(db, masterQuery, editTable, query);
	}
}
