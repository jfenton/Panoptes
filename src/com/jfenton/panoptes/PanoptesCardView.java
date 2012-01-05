package com.jfenton.panoptes;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteCursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class PanoptesCardView extends ListView {

	public static Context context;
	public static PanoptesMonitorCard monitorCard;
	public static PanoptesMonitorCard getMonitorCard() {
//		if(monitorCard == null)
//			monitorCard = new PanoptesMonitorCard();
		return monitorCard;
	}
	public List<PanoptesCard> cards;
	public static long nextCardIndex = 0;
	private static final String fields[] = { "cardId", "fromPeriod", "toPeriod", "lastSynchronised" };

	public PanoptesCardView(Context ctx) {
		super(ctx);
		PanoptesCardView.context = ctx;
		setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
	}

	public PanoptesCardView(Context ctx, AttributeSet attrs) {
		super(ctx, attrs);
		PanoptesCardView.context = ctx;
		setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
	}

	public PanoptesCardView(Context ctx, AttributeSet attrs, int defStyle) {
		super(ctx, attrs, defStyle);
		PanoptesCardView.context = ctx;
		setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
	}

	public void onFinishInflate() {
		super.onFinishInflate();
		
		PanoptesCardCursorFactory cf = new PanoptesCardCursorFactory();
		PanoptesDatabaseHelper dbHelper = new PanoptesDatabaseHelper(this.context, cf);
		SQLiteDatabase db = dbHelper.getWritableDatabase();

		SQLiteCursor cursor = (SQLiteCursor)db.query("cards", PanoptesDatabaseHelper.cardFields, null, null, null, null, "_id ASC");
		// TODO: startManagingCursor(cursor);

		PanoptesCardCursorWindow window = new PanoptesCardCursorWindow(false);
		cursor.setWindow(window);
		
		PanoptesCardAdapter adapter = new PanoptesCardAdapter(this.context, cursor);
		// CardAdapter adapter = new CardAdapter(this, cards);
		setAdapter(adapter);
	}

	public class CardAdapter extends BaseAdapter {
		public PanoptesCardView cardView;
		private List<PanoptesCard> cards;

		public CardAdapter(PanoptesCardView cardView, List<PanoptesCard> cards) {
			this.cards = cards;
			this.cardView = cardView;
		}

		public int getCount() {
			return cards.size();
		}

		public PanoptesCard getItem(int position) {
			return cards.get(cards.size() - 1 - position);
		}

		public long getItemId(int position) {
			if(position == 0) return PanoptesCardView.monitorCard.getId();
			return cards.get(cards.size() - 1 - position).getId();
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			// if(convertView == null) {
			if(position == 0) {
				PanoptesMonitorCard card = PanoptesCardView.monitorCard;
				card.controller = this.cardView;
				card.view = (LinearLayout) LayoutInflater.from(PanoptesCardView.context).inflate(R.layout.monitor, parent, false);
				card.dataView = (PanoptesMonitorCardDataView) card.view.findViewById(R.id.card_datums);
				return card.view;
			} else {
				PanoptesCard card = cards.get(cards.size() - 1 - position);
				card.controller = this.cardView;
				card.view = (LinearLayout) LayoutInflater.from(PanoptesCardView.context).inflate(R.layout.card, parent, false);
				card.dataView = (PanoptesCardDataView) card.view.findViewById(R.id.card_datums);
				TextView tf = (TextView) card.view.findViewById(R.id.cardTitle);
				tf.setText("Operator Report Card #" + card.getId());
				return card.view;
			}
			// } else {
			// return convertView;
			// }
		}
	}
}
