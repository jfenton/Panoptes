package com.jfenton.panoptes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

public class PanoptesMonitorCardService extends Service {

	public PanoptesMonitorCard monitorCard;

	private NotificationManager nm;
	private static boolean isRunning = false;

	ArrayList<Messenger> mClients = new ArrayList<Messenger>();

	static final int MSG_REGISTER_CLIENT = 1;
	static final int MSG_UNREGISTER_CLIENT = 2;
	static final int MSG_CARD_UPDATED = 3;
	static final int MSG_AUGMENT_CARD = 4;
	static final int MSG_FORCE_UNACCEPTABLE_TOGGLE = 5;
	
	final Messenger mMessenger = new Messenger(new IncomingHandler());

	@Override
	public void onCreate() {
		isRunning = true;
		Log.e("Panoptes", "PanoptesMonitorCardService onCreate");
		showNotification();
		monitorCard = new PanoptesMonitorCard(this.getApplicationContext(), this);
	}

	@Override
	public void onStart(Intent intent, int startid) {
		Log.e("Panoptes", "PanoptesMonitorCardService onStart");
		monitorCard.panoptesPhoneStateListener.onResume();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mMessenger.getBinder();
	}

	class IncomingHandler extends Handler { // Handler of incoming messages from
											// clients.
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_REGISTER_CLIENT:
				Log.e("Panoptes", "GOT REGISTER!");
				mClients.add(msg.replyTo);
				break;
			case MSG_UNREGISTER_CLIENT:
				mClients.remove(msg.replyTo);
				break;
			case MSG_AUGMENT_CARD:
				Log.e("Panoptes", "GOT AUGMENT!");
				monitorCard.panoptesPhoneStateListener.onResume();
				break;
			case MSG_FORCE_UNACCEPTABLE_TOGGLE:
				monitorCard.forceUnacceptable = !monitorCard.forceUnacceptable;
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}

	public void publishCard(PanoptesCard card) {
		for (int i = mClients.size() - 1; i >= 0; i--) {
			try {
				Bundle b = new Bundle();
				b.putString("data", card.serialiseToJSON().toString());
				Message msg = Message.obtain(null, MSG_CARD_UPDATED);
				msg.setData(b);
				mClients.get(i).send(msg);
			} catch (RemoteException e) {
				// The client is dead. Remove it from the list; we are going
				// through the list from back to front so this is safe to do
				// inside the loop.
				mClients.remove(i);
			}
		}
	}

	private void showNotification() {
		nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		// In this sample, we'll use the same text for the ticker and the
		// expanded notification
		CharSequence text = getText(R.string.service_started);
		// Set the icon, scrolling text and timestamp
		Notification notification = new Notification(R.drawable.ic_launcher, text, System.currentTimeMillis());
		// The PendingIntent to launch our activity if the user selects this
		// notification
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, PanoptesWebActivity.class), 0);
		// Set the info for the views that show in the notification panel.
		notification.setLatestEventInfo(this, getText(R.string.service_label), text, contentIntent);
		// Send the notification.
		// We use a layout id because it is a unique number. We use it later to
		// cancel.
		nm.notify(R.string.service_started, notification);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.e("Panoptes", "starting sticky!");
		return START_STICKY; // run until explicitly stopped.
	}

	public static boolean isRunning() {
		return isRunning;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		monitorCard.panoptesPhoneStateListener.onPause();
		nm.cancel(R.string.service_started); // Cancel the persistent
												// notification.
		isRunning = false;
	}
}
