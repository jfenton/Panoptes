package com.jfenton.panoptes;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.TimerTask;
import java.util.logging.Logger;

import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.jfenton.panoptes.receivers.LocationChangedReceiver;
import com.jfenton.panoptes.receivers.PassiveLocationChangedReceiver;
import com.jfenton.panoptes.utils.PlatformSpecificImplementationFactory;
import com.jfenton.panoptes.utils.base.ILastLocationFinder;
import com.jfenton.panoptes.utils.base.IStrictMode;
import com.jfenton.panoptes.utils.base.LocationUpdateRequester;
import com.jfenton.panoptes.utils.base.SharedPreferenceSaver;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Picture;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PictureDrawable;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.telephony.NeighboringCellInfo;
import android.telephony.TelephonyManager;
import android.util.EventLog;
import android.util.Log;
import android.util.EventLog.Event;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.LinearLayout;

public class PanoptesWebActivity extends Activity {

	LinearLayout linearLayout;
	MapView mapView;
	List<Overlay> mapOverlays;
	Drawable drawable;
	PanoptesItemizedOverlay itemizedOverlay;
	MapController mapController;

	protected static String TAG = "PlaceActivity";

	// TODO (RETO) Add "refreshing" icons when stuff is blank or refreshing.

	protected PackageManager packageManager;
	protected NotificationManager notificationManager;
	protected LocationManager locationManager;

	// protected boolean followLocationChanges = true;
	protected SharedPreferences prefs;
	protected Editor prefsEditor;
	protected SharedPreferenceSaver sharedPreferenceSaver;

	protected Criteria criteria;
	protected ILastLocationFinder lastLocationFinder;
	protected LocationUpdateRequester locationUpdateRequester;
	protected PendingIntent locationListenerPendingIntent;
	protected PendingIntent locationListenerPassivePendingIntent;

	protected IntentFilter newCheckinFilter;
	protected ComponentName newCheckinReceiverName;

	private ArrayList<OverlayItem> mOverlays = new ArrayList<OverlayItem>();

	protected TelephonyManager mainGSM;
	protected GSMReceiver receiverGSM;
	protected List<NeighboringCellInfo> gsmList;

	public static PanoptesDatabaseHelper dbHelper;
	public static SQLiteDatabase db;

	WebView mWebView;

	Messenger mService = null;
	boolean mIsBound;
	private final Messenger mMessenger = new Messenger(new PanoptesWebActivityIncomingHandler());

	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			mService = new Messenger(service);
			try {
				Log.e("Panoptes", "Sending REGISTER");
				Message msg = Message.obtain(null, PanoptesMonitorCardService.MSG_REGISTER_CLIENT);
				msg.replyTo = mMessenger;
				mService.send(msg);
			} catch (RemoteException e) {
				// In this case the service has crashed before we could even do
				// anything with it
			}
		}

		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected - process crashed.
			mService = null;
		}
	};

	public void onCreate(Bundle savedInstanceState) {

		if (PanoptesConstants.DEVELOPER_MODE) {
			IStrictMode strictMode = PlatformSpecificImplementationFactory.getStrictMode();
			if (strictMode != null)
				strictMode.enableStrictMode();
		}

		super.onCreate(savedInstanceState);
		setContentView(R.layout.web);

		Intent newinIntent = new Intent(this.getApplicationContext(), PanoptesMonitorCardService.class);
		this.getApplicationContext().startService(newinIntent);

		mWebView = (WebView) findViewById(R.id.webview);
		mWebView.getSettings().setJavaScriptEnabled(true);
		mWebView.setVerticalScrollbarOverlay(true);
		mWebView.setHorizontalScrollbarOverlay(true);
		mWebView.setBackgroundColor(Color.BLACK);
		mWebView.setScrollBarStyle(WebView.SCROLLBARS_INSIDE_OVERLAY);
		mWebView.getSettings().setLightTouchEnabled(true);
		mWebView.addJavascriptInterface(new PanoptesWebBridge(this), "Panoptes");
		mWebView.setWebChromeClient(new WebChromeClient() {
			public void onConsoleMessage(String message, int lineNumber, String sourceID) {
				Log.e("Panoptes", message + " -- From line " + lineNumber + " of " + sourceID);
			}
		});
		Log.e("Panoptes", "Launching!");
		mWebView.loadUrl("file:///android_asset/index.html");

		PanoptesCardCursorFactory cf = new PanoptesCardCursorFactory();
		dbHelper = new PanoptesDatabaseHelper(this.getApplicationContext(), cf);
		db = dbHelper.getWritableDatabase();

		// ArrayList<EventLog.Event> logs = new ArrayList<EventLog.Event>();
		// try {
		// int[] tags = new int[] { 50100, 50101, 50102, 50103, 50104, 50105,
		// 50106, 50107, 50108, 50109, 50110, 50111, 50112, 50113, 50114, 50115,
		// 50116 };
		//
		// EventLog.readEvents(tags, logs);
		// Iterator<Event> logsIterator = logs.iterator();
		// Event event = null;
		// while (logsIterator.hasNext()) {
		// event = (Event) logsIterator.next();
		// Object o[] = (Object[]) event.getData();
		// String logline = "";
		// for (int i = 0; i < o.length; i++) {
		// logline = logline + o[i].toString() + ",";
		// }
		// Log.v("Panoptes", "Got log entry with tag " + event.getTag() + ": " +
		// logline);
		//
		// }
		// } catch (IOException e) {
		// e.printStackTrace();
		// }

		Log.v("Panoptes", "GSMReciever instantiation");
		mainGSM = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		receiverGSM = new GSMReceiver();
		registerReceiver(receiverGSM, new IntentFilter());

		Log.v("Panoptes", "Neighbours:");
		List<NeighboringCellInfo> n = mainGSM.getNeighboringCellInfo();
		StringBuilder sb2 = new StringBuilder();
		for (NeighboringCellInfo nci : n) {
			int ncid = nci.getCid();
			int nrss = nci.getRssi();
			if (ncid > 0) {
				String ns = "  neighbors: " + ncid + ", " + nrss + "\n";
				Log.v("Panoptes", ns);
			}
		}

		// Get references to the managers
		packageManager = getPackageManager();
		notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		// Get a reference to the Shared Preferences and a Shared Preference
		// Editor.
		prefs = getSharedPreferences(PanoptesConstants.SHARED_PREFERENCE_FILE, Context.MODE_PRIVATE);
		prefsEditor = prefs.edit();

		// Instantiate a SharedPreferenceSaver class based on the available
		// platform version.
		// This will be used to save shared preferences
		sharedPreferenceSaver = PlatformSpecificImplementationFactory.getSharedPreferenceSaver(this);

		// Save that we've been run once.
		prefsEditor.putBoolean(PanoptesConstants.SP_KEY_RUN_ONCE, true);
		sharedPreferenceSaver.savePreferences(prefsEditor, false);

		// Specify the Criteria to use when requesting location updates while
		// the application is Active
		criteria = new Criteria();
		if (PanoptesConstants.USE_GPS_WHEN_ACTIVITY_VISIBLE)
			criteria.setAccuracy(Criteria.ACCURACY_FINE);
		else
			criteria.setPowerRequirement(Criteria.POWER_LOW);

		// Setup the location update Pending Intents
		Intent activeIntent = new Intent(this, LocationChangedReceiver.class);
		locationListenerPendingIntent = PendingIntent.getBroadcast(this, 0, activeIntent, PendingIntent.FLAG_UPDATE_CURRENT);

		Intent passiveIntent = new Intent(this, PassiveLocationChangedReceiver.class);
		locationListenerPassivePendingIntent = PendingIntent.getBroadcast(this, 0, passiveIntent, PendingIntent.FLAG_UPDATE_CURRENT);

		// Instantiate a LastLocationFinder class.
		// This will be used to find the last known location when the
		// application starts.
		lastLocationFinder = PlatformSpecificImplementationFactory.getLastLocationFinder(this);
		lastLocationFinder.setChangedLocationListener(oneShotLastLocationUpdateListener);

		// Instantiate a Location Update Requester class based on the available
		// platform version.
		// This will be used to request location updates.
		locationUpdateRequester = PlatformSpecificImplementationFactory.getLocationUpdateRequester(locationManager);

		getLocationAndUpdatePlaces(true);

		CheckIfServiceIsRunning();
	}

	private void CheckIfServiceIsRunning() {
		// If the service is running when the activity starts, we want to
		// automatically bind to it.
		// if (PanoptesMonitorCardService.isRunning()) {
		doBindService();
		// }
	}

	private void sendMessageToService(int intvaluetosend) {
		// if (mIsBound) {
		// if (mService != null) {
		// try {
		// Message msg = Message.obtain(null,
		// PanoptesMonitorCardService.MSG_SET_INT_VALUE, intvaluetosend, 0);
		// msg.replyTo = mMessenger;
		// mService.send(msg);
		// } catch (RemoteException e) {
		// }
		// }
		// }
	}

	void doBindService() {
		Log.e("Panoptes", "doBindService!");
		bindService(new Intent(this, PanoptesMonitorCardService.class), mConnection, Context.BIND_AUTO_CREATE);
		mIsBound = true;
	}

	void doUnbindService() {
		if (mIsBound) {
			// If we have received the service, and hence registered with it,
			// then now is the time to unregister.
			if (mService != null) {
				try {
					Message msg = Message.obtain(null, PanoptesMonitorCardService.MSG_UNREGISTER_CLIENT);
					msg.replyTo = mMessenger;
					mService.send(msg);
				} catch (RemoteException e) {
					// There is nothing special we need to do if the service has
					// crashed.
				}
			}
			// Detach our existing connection.
			unbindService(mConnection);
			mIsBound = false;
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (PanoptesCardView.monitorCard != null && PanoptesCardView.monitorCard.panoptesPhoneStateListener != null)
			PanoptesCardView.monitorCard.panoptesPhoneStateListener.onPause();
		// Tel.listen(Panoptes.panoptesPhoneStateListener,
		// PhoneStateListener.LISTEN_NONE);
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (PanoptesCardView.monitorCard != null && PanoptesCardView.monitorCard.panoptesPhoneStateListener != null)
			PanoptesCardView.monitorCard.panoptesPhoneStateListener.onResume();
		// Tel.listen(panoptesPhoneStateListener,
		// PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
	}

	/**
	 * Find the last known location (using a {@link LastLocationFinder}) and
	 * updates the place list accordingly.
	 * 
	 * @param updateWhenLocationChanges
	 *            Request location updates
	 */
	protected void getLocationAndUpdatePlaces(boolean updateWhenLocationChanges) {
		// This isn't directly affecting the UI, so put it on a worker thread.
		AsyncTask<Void, Void, Void> findLastLocationTask = new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				// Find the last known location, specifying a required accuracy
				// of within the min distance between updates
				// and a required latency of the minimum time required between
				// updates.
				Location lastKnownLocation = lastLocationFinder.getLastBestLocation(PanoptesConstants.MAX_DISTANCE, System.currentTimeMillis() - PanoptesConstants.MAX_TIME);

				// Update the place list based on the last known location within
				// a defined radius.
				// Note that this is *not* a forced update. The Place List
				// Service has settings to
				// determine how frequently the underlying web service should be
				// pinged. This function
				// is called everytime the Activity becomes active, so we don't
				// want to flood the server
				// unless the location has changed or a minimum latency or
				// distance has been covered.
				// TODO Modify the search radius based on user settings?
				// updatePlacdes(lastKnownLocation,
				// PanoptesConstants.DEFAULT_RADIUS, false);
				return null;
			}
		};
		findLastLocationTask.execute();

		// If we have requested location updates, turn them on here.
		toggleUpdatesWhenLocationChanges(updateWhenLocationChanges);
	}

	/**
	 * Choose if we should receive location updates.
	 * 
	 * @param updateWhenLocationChanges
	 *            Request location updates
	 */
	protected void toggleUpdatesWhenLocationChanges(boolean updateWhenLocationChanges) {
		// Save the location update status in shared preferences
		prefsEditor.putBoolean(PanoptesConstants.SP_KEY_FOLLOW_LOCATION_CHANGES, updateWhenLocationChanges);
		sharedPreferenceSaver.savePreferences(prefsEditor, true);

		// Start or stop listening for location changes
		if (updateWhenLocationChanges)
			requestLocationUpdates();
		else
			disableLocationUpdates();
	}

	/**
	 * Start listening for location updates.
	 */
	protected void requestLocationUpdates() {
		// Normal updates while activity is visible.
		locationUpdateRequester.requestLocationUpdates(PanoptesConstants.MAX_TIME, PanoptesConstants.MAX_DISTANCE, criteria, locationListenerPendingIntent);

		// Passive location updates from 3rd party apps when the Activity isn't
		// visible.
		locationUpdateRequester.requestPassiveLocationUpdates(PanoptesConstants.PASSIVE_MAX_TIME, PanoptesConstants.PASSIVE_MAX_DISTANCE, locationListenerPassivePendingIntent);

		// Register a receiver that listens for when the provider I'm using has
		// been disabled.
		IntentFilter intentFilter = new IntentFilter(PanoptesConstants.ACTIVE_LOCATION_UPDATE_PROVIDER_DISABLED);
		registerReceiver(locProviderDisabledReceiver, intentFilter);

		// Register a receiver that listens for when a better provider than I'm
		// using becomes available.
		String bestProvider = locationManager.getBestProvider(criteria, false);
		String bestAvailableProvider = locationManager.getBestProvider(criteria, true);
		if (bestProvider != null && !bestProvider.equals(bestAvailableProvider)) {
			locationManager.requestLocationUpdates(bestProvider, 0, 0, bestInactiveLocationProviderListener, getMainLooper());
		}
	}

	/**
	 * Stop listening for location updates
	 */
	protected void disableLocationUpdates() {
		unregisterReceiver(locProviderDisabledReceiver);
		locationManager.removeUpdates(locationListenerPendingIntent);
		locationManager.removeUpdates(bestInactiveLocationProviderListener);
		if (isFinishing())
			lastLocationFinder.cancel();
		if (PanoptesConstants.DISABLE_PASSIVE_LOCATION_WHEN_USER_EXIT && isFinishing())
			locationManager.removeUpdates(locationListenerPassivePendingIntent);
	}

	/**
	 * One-off location listener that receives updates from the
	 * {@link LastLocationFinder}. This is triggered where the last known
	 * location is outside the bounds of our maximum distance and latency.
	 */
	protected LocationListener oneShotLastLocationUpdateListener = new LocationListener() {
		public void onLocationChanged(Location l) {
			// updatePlaces(l, PanoptesConstants.DEFAULT_RADIUS, true);
		}

		public void onProviderDisabled(String provider) {
		}

		public void onStatusChanged(String provider, int status, Bundle extras) {
		}

		public void onProviderEnabled(String provider) {
		}
	};

	/**
	 * If the best Location Provider (usually GPS) is not available when we
	 * request location updates, this listener will be notified if / when it
	 * becomes available. It calls requestLocationUpdates to re-register the
	 * location listeners using the better Location Provider.
	 */
	protected LocationListener bestInactiveLocationProviderListener = new LocationListener() {
		public void onLocationChanged(Location l) {
		}

		public void onProviderDisabled(String provider) {
		}

		public void onStatusChanged(String provider, int status, Bundle extras) {
		}

		public void onProviderEnabled(String provider) {
			// Re-register the location listeners using the better Location
			// Provider.
			requestLocationUpdates();
		}
	};

	/**
	 * If the Location Provider we're using to receive location updates is
	 * disabled while the app is running, this Receiver will be notified,
	 * allowing us to re-register our Location Receivers using the best
	 * available Location Provider is still available.
	 */
	protected BroadcastReceiver locProviderDisabledReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			boolean providerDisabled = !intent.getBooleanExtra(LocationManager.KEY_PROVIDER_ENABLED, false);
			// Re-register the location listeners using the best available
			// Location Provider.
			if (providerDisabled)
				requestLocationUpdates();
		}
	};

	private class GSMReceiver extends BroadcastReceiver {
		StringBuilder sb;

		public void onReceive(Context c, Intent intent) {
			sb = new StringBuilder();
			gsmList = mainGSM.getNeighboringCellInfo();
			for (int i = 0; i < gsmList.size(); i++) {
				sb.append(new Integer(i + 1).toString() + ".");
				sb.append((gsmList.get(i)).toString());
				sb.append("\n");
			}
			Log.v("Panoptes", sb.toString());
		}
	}

	public class PanoptesWebBridge {
		PanoptesWebActivity mPanoptesWebActivity;
		
		PanoptesWebBridge(PanoptesWebActivity pwa) {
			this.mPanoptesWebActivity = pwa;
		}

		public String getMonitorCardState() {
			return "";
		}
	
		public void toggleUnacceptable() {
			Message msg = Message.obtain(null, PanoptesMonitorCardService.MSG_FORCE_UNACCEPTABLE_TOGGLE);
			msg.replyTo = mMessenger;
			try {
				this.mPanoptesWebActivity.mService.send(msg);
			} catch (RemoteException e) {
				e.printStackTrace();
			}			
		}
		
		public void sendFeedback() {
			Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);  
			String aEmailList[] = { "na.nu@na.nu" };  			  
			emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, aEmailList);  
			
			SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
			String now = sdf.format(new Date());
			
			emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Panoptes Bug Report " + now);  
			emailIntent.setType("plain/text");
			// emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, "");  

			String screenshotFilename = Environment.getExternalStorageDirectory().getAbsolutePath() + "/PanoptesScreenshot.png";
			
			Picture screenshot = mWebView.capturePicture();
			try {
				PictureDrawable pictureDrawable = new PictureDrawable(screenshot);
				Bitmap bmp = Bitmap.createBitmap(pictureDrawable.getIntrinsicWidth(),pictureDrawable.getIntrinsicHeight(), Config.ARGB_8888);
		        Canvas canvas = new Canvas(bmp);
		        canvas.drawPicture(pictureDrawable.getPicture());
				FileOutputStream out = new FileOutputStream(screenshotFilename);
				bmp.compress(Bitmap.CompressFormat.PNG, 95, out); 
				out.close();
				emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(Environment.getExternalStorageDirectory(), "/PanoptesScreenshot.png")));
				emailIntent.setType("image/png");				
			} catch(Exception e) {
				e.printStackTrace();
			}
			
			startActivity(emailIntent);
		}
	}

	class PanoptesWebActivityIncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			String data, url;
			switch (msg.what) {
			case PanoptesMonitorCardService.MSG_CARD_UPDATED:
				data = msg.getData().getString("data");
				Log.i("Panoptes", "got data: " + data);
				url = "javascript:PanoptesCardUpdated('" + data + "')";
				mWebView.loadUrl(url);
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}

	public void updateMonitorCard() {

	}
}
