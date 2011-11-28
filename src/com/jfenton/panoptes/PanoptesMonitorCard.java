package com.jfenton.panoptes;

import java.util.Date;

import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class PanoptesMonitorCard extends PanoptesCard {

	PanoptesPhoneStateListener panoptesPhoneStateListener;
	
	PanoptesMonitorCard() {
		super();
		panoptesPhoneStateListener = new PanoptesPhoneStateListener(this);
	}
	
	public class PanoptesPhoneStateListener extends PhoneStateListener {

		TelephonyManager telephonyManager;
		PanoptesMonitorCard pmc;
		
		PanoptesPhoneStateListener(PanoptesMonitorCard pmc) {
			telephonyManager = (TelephonyManager)PanoptesCardView.context.getSystemService(Context.TELEPHONY_SERVICE);
			telephonyManager.listen(panoptesPhoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
			telephonyManager.listen(panoptesPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);			
			this.pmc = pmc;
		}

		protected void onPause() {
			telephonyManager.listen(this, PhoneStateListener.LISTEN_NONE);
		}

		protected void onResume() {
			telephonyManager.listen(this, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
		}
		
		@Override
		public void onSignalStrengthsChanged(SignalStrength signalStrength) {
			super.onSignalStrengthsChanged(signalStrength);

			// TextView cinr = (TextView) findViewById(R.id.cinr_value);
			// cinr.setText(String.valueOf(signalStrength.getGsmSignalStrength()));
			//
			// ProgressBar cinr_pb = (ProgressBar)
			// findViewById(R.id.cinr_gauge);
			// cinr_pb.setProgress(signalStrength.getGsmSignalStrength());
			// cinr_pb.setMax(5);
			//
			// TextView ber = (TextView) findViewById(R.id.ber_value);
			// ber.setText(String.valueOf(signalStrength.getGsmBitErrorRate()));
			//
			// ProgressBar ber_pb = (ProgressBar) findViewById(R.id.ber_gauge);
			// ber_pb.setProgress(signalStrength.getGsmBitErrorRate());
			// ber_pb.setMax(5);
			//
			// TextView timestamp = (TextView) findViewById(R.id.timestamp);
			// timestamp.setText(new Date().toString());

			if (pmc.dataView != null && pmc.dataView.cds != null) {
				pmc.dataView.cds.get(1).value = String.valueOf(signalStrength.getGsmSignalStrength());
				pmc.dataView.cds.get(1).rawValue = signalStrength.getGsmSignalStrength();
				pmc.dataView.cds.get(2).value = String.valueOf(signalStrength.getGsmBitErrorRate());
				pmc.dataView.cds.get(2).rawValue = signalStrength.getGsmBitErrorRate();
				pmc.dataView.invalidate();
				
				TextView timestamp = (TextView)pmc.view.findViewById(R.id.timestamp);
				timestamp.setText(new Date().toString());
				pmc.view.invalidate();
			}

			// Toast.makeText(
			// getApplicationContext(),
			// "GSM Signal Strength Cinr = "
			// + String.valueOf(signalStrength
			// .getGsmSignalStrength()),
			// Toast.LENGTH_SHORT).show();
		}

		public void onCallStateChanged(int state, String incomingNumber) {
			Toast.makeText(PanoptesCardView.context, "Call state changed: " + stateName(state), Toast.LENGTH_SHORT).show();
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

}
