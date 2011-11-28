package com.jfenton.panoptes;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.jfenton.panoptes.utils.googlemaps.PolylineEncoder;
import com.jfenton.panoptes.utils.googlemaps.Track;
import com.jfenton.panoptes.utils.googlemaps.Trackpoint;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class PanoptesCardDataView extends ListView {

	List<CardData> cds;
	CardDataAdapter cda;
	Context context;
	private PanoptesDatabaseHelper dbHelper;
	private SQLiteDatabase db;
	protected Boolean initialised = false;
	
	public PanoptesCardDataView(Context context) {
		super(context);
		this.context = context;
	}

	public PanoptesCardDataView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;
	}

	public PanoptesCardDataView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		this.context = context;
	}

	public void onFinishInflate() {
		super.onFinishInflate();
		if(!initialised) {
			cds = new ArrayList<CardData>();
	
			Random randomGenerator = new Random();
	
			CardData cd = new CardData();
			cd.title = "Signal Stength";
			cd.subtitle = "RSSI (Received Signal Strength Indicator)";
			int randomInt = randomGenerator.nextInt(31);
			cd.value = randomInt + " dBm";
			cd.rawValue = randomInt;
			cds.add(cd);
	
			cd = new CardData();
			cd.title = "Signal : Noise Ratio";
			cd.subtitle = "Ec/lo (Rx energy vs. Interference level)";
			randomInt = randomGenerator.nextInt(5);
			cd.value = randomInt + " db*10";
			cd.rawValue = randomInt;
			cds.add(cd);
	
			cd = new CardDataLocation();
			cd.title = "Location";
			randomInt = randomGenerator.nextInt(8);
			cd.subtitle = "Latitude, Longitude accurate to within " + randomInt + " meters";
			cd.value = "51.30N 0.7W";
			cd.rawValue = -1;
			cds.add(cd);
	
			cda = new CardDataAdapter(cds, context);
			ListView lv = (ListView) findViewById(R.id.card_datums);
			setAdapter(cda);
			
			initialised = true;
		}
	}

	public class CardDataAdapter extends BaseAdapter {

		private List<CardData> cds;
		private Context context;

		public CardDataAdapter(List<CardData> cds, Context context) {
			this.cds = cds;
			this.context = context;
		}

		public int getCount() {
			return cds.size();
		}

		public CardData getItem(int position) {
			return cds.get(position);
		}

		public long getItemId(int position) {
			return cds.get(position).getId();
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			// if(convertView == null) {
			CardData cd = cds.get(position);

			int resource = R.layout.card_data;
			if (cd.type == CardDataType.LOCATION) {
				resource = R.layout.card_data_location;
			}

			LinearLayout ll = (LinearLayout) LayoutInflater.from(context).inflate(resource, parent, false);

			if (cd.type == CardDataType.LOCATION) {
				CardDataLocation cdl = (CardDataLocation) cd;
				cdl.addViews(ll);
			}

			TextView tv = (TextView) ll.findViewById(R.id.title);
			tv.setText(cd.title);
			tv = (TextView) ll.findViewById(R.id.subtitle);
			tv.setText(cd.subtitle);
			tv = (TextView) ll.findViewById(R.id.value);
			tv.setText(cd.value);

			if (cd.rawValue != -1 && cd.rawValue <= 10) {
				tv.setTextColor(Color.RED);
			}

			return ll;
			// } else {
			// return convertView;
			// }
		}
	}

	public enum CardDataType {
		GENERIC, LOCATION
	};

	public class CardData {
		public String title;
		public String subtitle;
		public String value;
		public Integer rawValue;
		public LinearLayout view;
		public CardDataType type;

		public CardData() {
			this.type = CardDataType.GENERIC;
		}

		public long getId() {
			return 0;
		}
	}

	public class CardDataLocation extends CardData {
		public CardDataLocation() {
			this.type = CardDataType.LOCATION;
		}

		public void addViews(LinearLayout ll) {
			ImageView iv = (ImageView) ll.findViewById(R.id.mapImageView);
			iv.setTag("http://maps.googleapis.com/maps/api/staticmap?center=40.714728,-73.998672&zoom=16&size=" + 450 + "x" + 250 + "&sensor=false&markers=color:red%7Clabel:!%7C40.714728,-73.998672");
			DownloadImageTask dit = new DownloadImageTask();
			dit.execute(iv);
		}
	}

	public class DownloadImageTask extends AsyncTask<ImageView, Void, Bitmap> {

		ImageView imageView = null;

		@Override
		protected Bitmap doInBackground(ImageView... imageViews) {
			this.imageView = imageViews[0];
			return download_Image((String) imageView.getTag());
		}

		@Override
		protected void onPostExecute(Bitmap result) {
			imageView.setImageBitmap(result);
		}

		private Bitmap download_Image(String url) {
			Bitmap bm = null;
			try {
				URL aURL = new URL(url);
				URLConnection conn = aURL.openConnection();
				conn.connect();
				InputStream is = conn.getInputStream();
				BufferedInputStream bis = new BufferedInputStream(is);
				bm = BitmapFactory.decodeStream(bis);
				bis.close();
				is.close();
			} catch (IOException e) {
				Log.e("Hub", "Error getting the image from server : " + e.getMessage().toString());
			}
			return bm;
		}
	}
}
