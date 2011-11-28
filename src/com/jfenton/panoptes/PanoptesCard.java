package com.jfenton.panoptes;

import android.widget.LinearLayout;
import android.widget.ListView;

public class PanoptesCard {
	public ListView controller;
	public LinearLayout view;
	public PanoptesCardDataView dataView;
	public long id;

	public PanoptesCard() {
		this.id = PanoptesCardView.nextCardIndex++;
	}

	public long getId() {
		return this.id;
	}
}
