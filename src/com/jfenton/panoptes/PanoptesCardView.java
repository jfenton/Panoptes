package com.jfenton.panoptes;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
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
	public List<PanoptesCard> cards;
	public static long nextCardIndex = 1;
	private static final String fields[] = { "cardId", "fromPeriod", "toPeriod", "lastSynchronised" };

	private PanoptesDatabaseHelper dbHelper;
	private SQLiteDatabase db;

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

		dbHelper = new PanoptesDatabaseHelper(context);
		db = dbHelper.getWritableDatabase();

		Cursor data = db.query("cards", fields, null, null, null, null, null);
//		dataSource = new SimpleCursorAdapter(this, R.layout.row, data, fields, new int[] { R.id.first, R.id.last });

		cards = new ArrayList<PanoptesCard>();
		PanoptesCardView.monitorCard = new PanoptesMonitorCard();
		cards.add(PanoptesCardView.monitorCard);
		
		PanoptesCard c = new PanoptesCard();
		cards.add(c);

		c = new PanoptesCard();
		cards.add(c);

		c = new PanoptesCard();
		cards.add(c);

		CardAdapter adapter = new CardAdapter(this, cards);
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
