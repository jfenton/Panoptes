package com.jfenton.panoptes;

import java.util.HashMap;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteCursor;
import android.database.sqlite.SQLiteCursorDriver;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQuery;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.LinearLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class PanoptesCardAdapter extends CursorAdapter {

	private Cursor c;
	private Context context;
	public static HashMap<Integer, Integer> positionToCardId;

	public PanoptesCardAdapter(Context context, Cursor c) {
		super(context, c);
		this.c = c;
		this.context = context;
		PanoptesCardAdapter.positionToCardId = new HashMap<Integer, Integer>();
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		// if(convertView == null) {

		// card.dataView = (PanoptesCardDataView)
		// card.view.findViewById(R.id.card_datums);

		if (cursor.getPosition() == 0) {
			PanoptesMonitorCard card = PanoptesCardView.getMonitorCard();
//			card.controller = this.cardView;
			card.view = (LinearLayout) LayoutInflater.from(context).inflate(R.layout.monitor, parent, false);
			card.view.setTag(cursor.getString(cursor.getColumnIndex("_id")));
			card.dataView = (PanoptesMonitorCardDataView) card.view.findViewById(R.id.card_datums);
			return card.view;
		} else {
			PanoptesCardAdapter.positionToCardId.put(cursor.getPosition(), cursor.getInt(cursor.getColumnIndex("_id")));
			View v = (LinearLayout) LayoutInflater.from(context).inflate(R.layout.card, parent, false);
			bindView(v, context, cursor);
			return v;
			// PanoptesCard card = cards.get(cards.size() - 1 - position);
			// card.controller = this.cardView;
			// card.view = (LinearLayout)
			// LayoutInflater.from(PanoptesCardView.context).inflate(R.layout.card,
			// parent, false);
			// card.dataView = (PanoptesCardDataView)
			// card.view.findViewById(R.id.card_datums);
			// TextView tf = (TextView) card.view.findViewById(R.id.cardTitle);
			// tf.setText("Operator Report Card #" + card.getId());

		}
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		if (cursor.getPosition() == 0) {
			PanoptesMonitorCard card = PanoptesCardView.monitorCard;
//			TextView tf = (TextView) view.findViewById(R.id.timestamp);
//			tf.setText(card.lastUpdated.toString());
		} else {
			TextView tf = (TextView) view.findViewById(R.id.cardTitle);
			tf.setText("Operator Report Card #" + cursor.getString(cursor.getColumnIndex("_id")));
		}
	}

}
