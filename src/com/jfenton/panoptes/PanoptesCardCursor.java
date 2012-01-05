package com.jfenton.panoptes;

import android.database.CursorWindow;
import android.database.sqlite.SQLiteCursor;
import android.database.sqlite.SQLiteCursorDriver;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQuery;

public class PanoptesCardCursor extends SQLiteCursor {
	public PanoptesCardCursor(SQLiteDatabase db, SQLiteCursorDriver driver, String editTable, SQLiteQuery query) {
		super(db, driver, editTable, query);
	}

	public PanoptesCardCursor(SQLiteCursorDriver driver, String editTable, SQLiteQuery query) {
		super(driver, editTable, query);
	}

	public int getCount() {
		return super.getCount();
	}
}
