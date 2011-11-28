package com.jfenton.panoptes;

import java.util.ArrayList;
import java.util.Random;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ListView;

public class PanoptesMonitorCardDataView extends PanoptesCardDataView {

	public PanoptesMonitorCardDataView(Context context) {
		super(context);
		this.initialised = true; // Prevents superclass from initialising
	}

	public PanoptesMonitorCardDataView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.initialised = true;
	}

	public PanoptesMonitorCardDataView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		this.initialised = true;
	}

	public void onFinishInflate() {
		super.onFinishInflate();
		cds = new ArrayList<CardData>();

		Random randomGenerator = new Random();

		CardData cd = new CardData();
		cd.title = "Current Network";
		cd.subtitle = "Your handset is registered on this network.";
		cd.value = "3UK";
		cd.rawValue = -1;
		cds.add(cd);

		cd = new CardData();
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

		cda = new CardDataAdapter(cds, context);
		ListView lv = (ListView) findViewById(R.id.card_datums);
		setAdapter(cda);
	}

}
