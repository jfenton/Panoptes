package com.jfenton.panoptes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONObject;

import com.jfenton.panoptes.PanoptesWebActivity.PanoptesWebActivityIncomingHandler;
import com.jfenton.panoptes.services.PlaceCheckinService;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class PanoptesMonitorCard extends PanoptesCard {

	Context context;
	PanoptesPhoneStateListener panoptesPhoneStateListener;
	PanoptesCard activeReportCard;
	PanoptesMonitorCardService pmcs;
	private Timer timer;
	LocationListener locationListener;
	LocationManager lm;
	
	PanoptesMonitorCard(Context aContext, PanoptesMonitorCardService aPmcs) {
		super();
		activeReportCard = null;
		context = aContext;
		pmcs = aPmcs;

		forceUnacceptable = false;
		
		panoptesPhoneStateListener = new PanoptesPhoneStateListener(this);
		timer = new Timer();
		timer.schedule(new PanoptesPhoneStateTimerTask(), 1000, 1000);
		
		locationListener = new PanoptesMonitorCardLocationListener(this);
		lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

		PanoptesLogcatTailerThread pltt = new PanoptesLogcatTailerThread(this);
		pltt.start();

		PanoptesRILThread prt = new PanoptesRILThread(this);
		prt.start();
	}

	@Override
	public void put(String key, String value) {
		super.put(key, value);
	}

	private class PanoptesLogcatTailerThread extends Thread {

		// D/GSM ( 388): [GSMConn] onDisconnect: cause=OUT_OF_SERVICE
		private static final String logcatCallDisconectReasonRegex = "onDisconnect: cause=([A-Z_]+)";

		// D/RILC ( 130): [0830]> DIAL (num=333,clir=0)
		private static final String logcatCalledNumberRegex = "DIAL \\(num=(\\d+),clir=\\d+\\)";

		private final Pattern logcatCallDisconectReasonPattern = Pattern.compile(logcatCallDisconectReasonRegex);
		private final Pattern logcatCalledNumberPattern = Pattern.compile(logcatCalledNumberRegex);

		boolean closed = false;
		private String filename;
		Process logcatProcess;
		InputStream logcatStdoutStream;
		BufferedReader logcatOutputBufferedReader;

		PanoptesMonitorCard pmc;

		Messenger mService = null;
		boolean mIsBound;

		private ServiceConnection mConnection = new ServiceConnection() {
			public void onServiceConnected(ComponentName className, IBinder service) {
				mService = new Messenger(service);
			}

			public void onServiceDisconnected(ComponentName className) {
				mService = null;
			}
		};

		public PanoptesLogcatTailerThread(PanoptesMonitorCard aPmc) {

			this.pmc = aPmc;

			pmc.context.bindService(new Intent(pmc.context, PanoptesMonitorCardService.class), mConnection, Context.BIND_AUTO_CREATE);
			mIsBound = true;

			ProcessBuilder processBuilder = new ProcessBuilder().directory(Environment.getExternalStorageDirectory()).command("logcat", "-b", "radio");
			Log.e("PanoptesLogcatTailerThread", "startLogging: " + processBuilder.command());

			Process logcatProcess;
			try {
				logcatProcess = processBuilder.start();
				logcatStdoutStream = logcatProcess.getInputStream();
				logcatOutputBufferedReader = new BufferedReader(new InputStreamReader(logcatStdoutStream));
			} catch (IOException ioe) {
				Log.e("PanoptesLogcatTailerThread", "Problem initialising logcat process: " + ioe.toString());
			}

			Log.e("PanoptesLogcatTailerThread", "logcat ran!");
		}

		public void close() throws IOException {
			closed = true;
		}

		public void run() {
			String logcatLine;
			String lastCalledNumber = "";
			Boolean spooledToCurrent = false;
			Date receivedLastLineAt = new Date();
			try {
				while (!closed && (logcatLine = logcatOutputBufferedReader.readLine()) != null) {
					if (spooledToCurrent) {
						Matcher matcher = logcatCallDisconectReasonPattern.matcher(logcatLine);
						if (matcher.find()) {
							// TODO: BUGFIX: Deal with historical log lines
							// causing firing
							String disconnectCause = matcher.group(1);
							Log.e("PanoptesLogcatTailerThread", "Got disconnectCause " + disconnectCause);
							if (!disconnectCause.equals("LOCAL")) {
								Log.e("PanoptesLogcatTailerThread", "Creating report card due to " + disconnectCause);
								activeReportCard = new PanoptesCard();
								activeReportCard.put("type", "dropped_call");
								activeReportCard.put("period_from", new Date());
								activeReportCard.put("period_to", new Date());
								// Populate Report Card
								try {
									activeReportCard.put("called_number", lastCalledNumber);
								} catch (Exception e) {
								}
								try {
									activeReportCard.put("disconnect_cause", disconnectCause);
								} catch (Exception e) {
								}

								// lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
								// 500, 10, pmc.locationListener);
								// Log.e("Panoptes", "Identifying location...");

								pmc.pmcs.publishCard(activeReportCard);

								Log.e("Panoptes", "Sending AUGMENT");
								Message msg = Message.obtain(null, PanoptesMonitorCardService.MSG_AUGMENT_CARD);
								msg.replyTo = null;
								try {
									mService.send(msg);
								} catch (RemoteException e) {
									e.printStackTrace();
								}

							}
						}

						matcher = logcatCalledNumberPattern.matcher(logcatLine);
						if (matcher.find()) {
							lastCalledNumber = matcher.group(1);
							Log.e("PanoptesLogcatTailerThread", "Got called number " + lastCalledNumber);
						} else {
							if (logcatLine.startsWith("D/GSM")) {
								Log.e("PanoptesLogcatTailerThread", logcatLine);
							}
						}
					} else {
						if (receivedLastLineAt != null) {
							Date receivedLineAt = new Date();
							if (receivedLineAt.getTime() - receivedLastLineAt.getTime() > 500) {
								// More than 0.5 second since last line
								// received, start active tailing
								spooledToCurrent = true;
							}
							receivedLastLineAt = receivedLineAt;
						} else {
							receivedLastLineAt = new Date();
						}
					}
				}
			} catch (IOException ioe) {
				Log.e("Panoptes", ioe.toString());
				try {
					logcatStdoutStream.close();
					logcatProcess.waitFor();
				} catch (IOException ioe2) {
					Log.e("Panoptes", ioe2.toString());
				} catch (InterruptedException ie) {
					Log.e("Panoptes", ie.toString());
				}
			}
			Log.e("Panoptes", "PanoptesLogcatTailerThread exited");
		}
	}

	public class PanoptesPhoneStateListener extends PhoneStateListener {

		Context context;
		TelephonyManager telephonyManager;
		PanoptesMonitorCard pmc;

		PanoptesPhoneStateListener(PanoptesMonitorCard aPanoptesMonitorCard) {
			pmc = aPanoptesMonitorCard;
			context = aPanoptesMonitorCard.context;
			telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
			telephonyManager.listen(this, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
			telephonyManager.listen(this, PhoneStateListener.LISTEN_CALL_STATE);
		}

		protected void onPause() {
			telephonyManager.listen(this, PhoneStateListener.LISTEN_NONE);
		}

		protected void onResume() {
			telephonyManager.listen(this, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
			telephonyManager.listen(this, PhoneStateListener.LISTEN_CALL_STATE);
		}

		@Override
		public void onSignalStrengthsChanged(SignalStrength signalStrength) {
			super.onSignalStrengthsChanged(signalStrength);
			if (signalStrength.isGsm()) {
				try {
					pmc.put("gsm_rssi", String.valueOf(signalStrength.getGsmSignalStrength())); // 0-31,
				} catch (Exception e) {
				}

				try {
					pmc.put("gsm_ber", String.valueOf(signalStrength.getGsmBitErrorRate())); // 0-7,99
				} catch (Exception e) {
				}

				GsmCellLocation loc = (GsmCellLocation) telephonyManager.getCellLocation();
				try {
					pmc.put("cell_gsm_cid", String.valueOf(loc.getCid()));
				} catch (Exception e) {
				}
				try {
					pmc.put("cell_gsm_lac", String.valueOf(loc.getLac()));
				} catch (Exception e) {
				}
				try {
					pmc.put("cell_gsm_psc", String.valueOf(loc.getPsc()));
				} catch (Exception e) {
				}
			}

			if (telephonyManager.getNetworkType() == TelephonyManager.NETWORK_TYPE_CDMA) {
				try {
					pmc.put("cdma_dbm", String.valueOf(signalStrength.getCdmaDbm()));
				} catch (Exception e) {
				}
				try {
					pmc.put("cdma_ecio", String.valueOf(signalStrength.getCdmaEcio()));
				} catch (Exception e) {
				}

				CdmaCellLocation loc = (CdmaCellLocation) telephonyManager.getCellLocation();
				try {
					pmc.put("cell_cdma_bsid", String.valueOf(loc.getBaseStationId()));
				} catch (Exception e) {
				}
				try {
					pmc.put("cell_cdma_loc", String.valueOf(loc.getBaseStationLatitude() + "," + String.valueOf(loc.getBaseStationLongitude())));
				} catch (Exception e) {
				}
				try {
					pmc.put("cell_cdma_nid", String.valueOf(loc.getNetworkId()));
				} catch (Exception e) {
				}
				try {
					pmc.put("cell_cdma_sid", String.valueOf(loc.getSystemId()));
				} catch (Exception e) {
				}
			}

			if (telephonyManager.getNetworkType() == TelephonyManager.NETWORK_TYPE_EVDO_0 || telephonyManager.getNetworkType() == TelephonyManager.NETWORK_TYPE_EVDO_A || telephonyManager.getNetworkType() == TelephonyManager.NETWORK_TYPE_EVDO_B) {
				try {
					pmc.put("evdo_dbm", String.valueOf(signalStrength.getEvdoDbm()));
				} catch (Exception e) {
				}
				try {
					pmc.put("evdo_ecio", String.valueOf(signalStrength.getEvdoEcio()));
				} catch (Exception e) {
				}
				try {
					pmc.put("evdo_snr", String.valueOf(signalStrength.getEvdoSnr())); // 0-8
				} catch (Exception e) {
				}
			}

			if (activeReportCard != null) {
				try {
					int phoneType = telephonyManager.getPhoneType();
					String phoneTypeStr = null;
					switch (phoneType) {
					case TelephonyManager.PHONE_TYPE_CDMA:
						phoneTypeStr = "CDMA";
						break;
					case TelephonyManager.PHONE_TYPE_GSM:
						phoneTypeStr = "GSM";
						break;
					case TelephonyManager.PHONE_TYPE_NONE:
						break;
					case TelephonyManager.PHONE_TYPE_SIP:
						phoneTypeStr = "SIP";
						break;
					}
					if (phoneTypeStr != null)
						pmc.put("pt", phoneTypeStr);
				} catch (Exception e) {
				}

				try {
					int networkType = telephonyManager.getNetworkType();
					String networkTypeStr = null;
					switch (networkType) {
					case TelephonyManager.NETWORK_TYPE_UNKNOWN:
						break;
					case TelephonyManager.NETWORK_TYPE_GPRS:
						networkTypeStr = "GPRS";
						break;
					case TelephonyManager.NETWORK_TYPE_EDGE:
						networkTypeStr = "EDGE";
						break;
					case TelephonyManager.NETWORK_TYPE_UMTS:
						networkTypeStr = "UMTS";
						break;
					case TelephonyManager.NETWORK_TYPE_CDMA:
						networkTypeStr = "CDMA";
						break;
					case TelephonyManager.NETWORK_TYPE_EVDO_0:
						networkTypeStr = "EVDO0";
						break;
					case TelephonyManager.NETWORK_TYPE_EVDO_A:
						networkTypeStr = "EVDOA";
						break;
					case TelephonyManager.NETWORK_TYPE_1xRTT:
						networkTypeStr = "1xRTT";
						break;
					case TelephonyManager.NETWORK_TYPE_HSDPA:
						networkTypeStr = "HSDPA";
						break;
					case TelephonyManager.NETWORK_TYPE_HSUPA:
						networkTypeStr = "HSUPA";
						break;
					case TelephonyManager.NETWORK_TYPE_HSPA:
						networkTypeStr = "HSPA";
						break;
					case TelephonyManager.NETWORK_TYPE_IDEN:
						networkTypeStr = "IDEN";
						break;
					case TelephonyManager.NETWORK_TYPE_EVDO_B:
						networkTypeStr = "EVDOB";
						break;
					case TelephonyManager.NETWORK_TYPE_LTE:
						networkTypeStr = "LTE";
						break;
					case TelephonyManager.NETWORK_TYPE_EHRPD:
						networkTypeStr = "EHRPD";
						break;
					case TelephonyManager.NETWORK_TYPE_HSPAP:
						networkTypeStr = "HSPAP";
						break;
					}
					if (networkTypeStr != null)
						pmc.put("nt", networkTypeStr);
				} catch (Exception e) {
				}
			}

			if (!pmc.isAcceptable() && activeReportCard == null) {
				Log.e("Panoptes", "Creating new report card");
				activeReportCard = new PanoptesCard();
				activeReportCard.put("type", "signal");
				activeReportCard.put("period_from", new Date());

				if (signalStrength.isGsm()) {
					pmc.put("imei", telephonyManager.getDeviceId());
				}

				if (telephonyManager.getNetworkType() == TelephonyManager.NETWORK_TYPE_CDMA) {
					pmc.put("cdma_meidesn", telephonyManager.getDeviceId());
				}

				if (telephonyManager.getNetworkType() == TelephonyManager.NETWORK_TYPE_EVDO_0 || telephonyManager.getNetworkType() == TelephonyManager.NETWORK_TYPE_EVDO_A || telephonyManager.getNetworkType() == TelephonyManager.NETWORK_TYPE_EVDO_B) {
				}

				if (telephonyManager.getSimSerialNumber() != null)
					try {
						pmc.put("spn", telephonyManager.getSimSerialNumber());
					} catch (Exception e) {
					}
				try {
					pmc.put("son", telephonyManager.getSimOperatorName());
				} catch (Exception e) {
				}
				try {
					pmc.put("so", telephonyManager.getSimOperator());
				} catch (Exception e) {
				}
				try {
					pmc.put("sciso", telephonyManager.getSimCountryIso());
				} catch (Exception e) {
				}
				try {
					pmc.put("no", telephonyManager.getNetworkOperator());
				} catch (Exception e) {
				}
				try {
					pmc.put("nciso", telephonyManager.getNetworkCountryIso());
				} catch (Exception e) {
				}
				try {
					pmc.put("msisdn", telephonyManager.getLine1Number());
				} catch (Exception e) {
				}
				try {
					pmc.put("non", telephonyManager.getNetworkOperatorName());
				} catch (Exception e) {
				}

				lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 500, 10, pmc.locationListener);
				Log.e("Panoptes", "Identifying location...");
			}

			if (activeReportCard != null) {
				Log.e("Panoptes", "Updating report card");
				activeReportCard.put(pmc.activeData);
				activeReportCard.put("period_to", new Date());
				activeReportCard.snapshot();
				pmc.pmcs.publishCard(activeReportCard);

				long window = pmc.getMillisecondsSinceLastUnacceptable();
				Log.e("Panoptes", "Window is " + window);
				if (window > 15000) {
					lm.removeUpdates(pmc.locationListener);
					Log.e("Panoptes", "Report card complete. Disabled location updates, and queueing card for uplink.");
					
					Intent checkinServiceIntent = new Intent(pmc.context, PlaceCheckinService.class);
					checkinServiceIntent.putExtra(PanoptesConstants.EXTRA_KEY_REFERENCE, activeReportCard.serialiseToJSON().toString());
					checkinServiceIntent.putExtra(PanoptesConstants.EXTRA_KEY_ID, activeReportCard.id);
					checkinServiceIntent.putExtra(PanoptesConstants.EXTRA_KEY_TIME_STAMP, System.currentTimeMillis());
					pmc.context.startService(checkinServiceIntent);

				    activeReportCard = null;
				}
			}

			// Always publish the monitor card
			pmc.pmcs.publishCard(pmc);
		}

		public void onCallStateChanged(int state, String incomingNumber) {
			// Toast.makeText(context, "Call state changed: " +
			// stateName(state), Toast.LENGTH_SHORT).show();
		}

		String stateName(int state) {
			switch (state) {
			case TelephonyManager.CALL_STATE_IDLE:
				return "Idle";
			case TelephonyManager.CALL_STATE_OFFHOOK:
				return "Off hook";
			case TelephonyManager.CALL_STATE_RINGING:
				return "Ringing";
			}
			return Integer.toString(state);
		}
	}

	class PanoptesPhoneStateTimerTask extends TimerTask {
		public void run() {
			panoptesPhoneStateListener.onResume();
		}
	}

	private final class PanoptesMonitorCardLocationListener implements LocationListener {

		PanoptesMonitorCard pmc;

		PanoptesMonitorCardLocationListener(PanoptesMonitorCard aPmc) {
			super();
			this.pmc = aPmc;
			Log.e("Panoptes", "PanoptesMonitorCardLocationListener constructed");
		}

		@Override
		public void onLocationChanged(Location locFromGps) {
			Log.e("Panoptes", "Received GPS: " + locFromGps.toString());

			String latLong = String.valueOf(locFromGps.getLatitude()) + "," + String.valueOf(locFromGps.getLongitude());
			latLong += "@" + String.valueOf(locFromGps.getAccuracy());

			pmc.activeReportCard.put("loc", latLong);

			pmc.activeReportCard.put("loc_t", String.valueOf(locFromGps.getTime()));

			if (locFromGps.hasAccuracy())
				pmc.activeReportCard.put("loc_accuracy", String.valueOf(locFromGps.getAccuracy()));

			if (locFromGps.hasAltitude())
				pmc.activeReportCard.put("loc_altitude", String.valueOf(locFromGps.getAltitude()));

			if (locFromGps.hasSpeed())
				pmc.activeReportCard.put("loc_speed", String.valueOf(locFromGps.getSpeed()));

			if (locFromGps.hasBearing())
				pmc.activeReportCard.put("loc_bearing", String.valueOf(locFromGps.getBearing()));
		}

		@Override
		public void onProviderDisabled(String provider) {
			// called when the GPS provider is turned off (user turning off the
			// GPS on the phone)
			Log.e("Panoptes", "PanoptesMonitorCardLocationListener dis");
		}

		@Override
		public void onProviderEnabled(String provider) {
			// called when the GPS provider is turned on (user turning on the
			// GPS on the phone)
			Log.e("Panoptes", "PanoptesMonitorCardLocationListener en");
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			// called when the status of the GPS provider changes
			Log.e("Panoptes", "PanoptesMonitorCardLocationListener status");
		}
	}

	public class PanoptesRILThread extends Thread {
		public PanoptesMonitorCard pmc;
		private LocalSocket localsocket;

		public PanoptesRILThread(PanoptesMonitorCard aPmc) {
			this.pmc = aPmc;
		}

		public void run() {
			Log.e("PanoptesRILThread", "Connecting to RILD socket!");
			localsocket = new LocalSocket();
			android.net.LocalSocketAddress.Namespace namespace = android.net.LocalSocketAddress.Namespace.RESERVED;
			LocalSocketAddress localsocketaddress = new LocalSocketAddress("rild", namespace);
			try {
				localsocket.connect(localsocketaddress);
				Log.e("PanoptesRILThread", "Connected to RILD socket!");

				// RILRequest rilrequest = RILRequest.obtain(19, message);
				// StringBuilder stringbuilder = new StringBuilder();
				// String s = rilrequest.serialString();
				// StringBuilder stringbuilder1 =
				// stringbuilder.append(s).append("> ");
				// String s1 = requestToString(rilrequest.mRequest);
				// String s2 = stringbuilder1.append(s1).toString();
				// riljLog(s2);
				// send(rilrequest);

			} catch (IOException e) {
				Log.e("PanoptesRILThread", "Problem connecting to RILD socket: " + e.toString());
			}
		}
	}

	class PanoptesMonitorCardIncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			default:
				super.handleMessage(msg);
			}
		}
	}
}
