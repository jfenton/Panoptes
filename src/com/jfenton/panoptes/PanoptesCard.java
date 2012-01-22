package com.jfenton.panoptes;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Observable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.database.ContentObservable;
import android.database.ContentObserver;
import android.database.DataSetObserver;
import android.os.Handler;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.ListView;

public class PanoptesCard {
	public ListView controller;
	public LinearLayout view;
	public PanoptesCardDataView dataView;
	public long id;
	public Date lastUpdated;
	public HashMap<String, String> activeData;
	public ArrayList<HashMap<String, String>> data;
	public Date period_from;
	public Date period_to;
	public Date last_updated;
	public Date last_unacceptable;
	public Boolean forceUnacceptable;
	
	public PanoptesCard() {
		this.id = PanoptesCardView.nextCardIndex++;
		activeData = new HashMap<String, String>();
		data = new ArrayList<HashMap<String, String>>();
		activeData.put("id", String.valueOf(this.id));
		forceUnacceptable = false;
	}

	public long getId() {
		return this.id;
	}

	public void put(String key, String value) {
		this.activeData.put(key, value);
		this.last_updated = new Date();
		this.activeData.put("last_updated", this.last_updated.toString());
	}

	public void put(String key, Date value) {
		this.put(key, value.toString());
		if (key == "period_from") {
			period_from = value;
		} else if (key == "period_to") {
			period_to = value;
		}
	}

	public void put(HashMap<?, ?> aExistingData) {
		Iterator<?> iter = aExistingData.keySet().iterator();
		while (iter.hasNext()) {
			String key = (String) iter.next();
			if (key != "id" && !key.startsWith("period_"))
				activeData.put(key, (String) aExistingData.get(key));
		}
	}

	public void snapshot() {
		Boolean sample = false;
		if (data.size() > 0) {
			HashMap<String, String> lastData = data.get(data.size() - 1);
			HashMap<String, String> sampledData = (HashMap<String, String>) activeData.clone();
			Iterator<?> iter = lastData.keySet().iterator();
			while (iter.hasNext()) {
				String key = (String) iter.next();
				if (key != "last_updated" && key != "period_from" && key != "period_to") {
					if (activeData.containsKey(key) && lastData.containsKey(key)) {
						String lastValue = (String) lastData.get(key);
						String latestValue = (String) activeData.get(key);
						if (lastValue == null && latestValue == null) {
							sampledData.remove(key);
						} else if(!lastValue.equals(latestValue)) {
							Log.e("Panoptes", "Data point " + key + " has changed from " + lastValue + " to " + latestValue);
							sample = true;
						} else {
							sampledData.remove(key);
						}
					}
				}
			}
		} else
			sample = true;
		if (sample) {
			HashMap<String, String> sampledData = (HashMap<String, String>) activeData.clone();
			sampledData.put("ts", sampledData.get("period_to"));
			sampledData.remove("period_to");
			sampledData.remove("period_from");
			data.add(sampledData);
		}
	}

	public String get(String key) {
		return this.activeData.get(key);
	}

	public JSONArray serialiseAllToJSON() {
		return new JSONArray(data);
	}

	public JSONObject serialiseToJSON() {
		HashMap<String, String> dataToSerialiseHashMap = (HashMap<String, String>) this.activeData.clone();
		
		JSONObject json = new JSONObject();

		try {
			Iterator<?> datumiter = activeData.keySet().iterator();
			json.accumulate("id", activeData.get("id"));
			while (datumiter.hasNext()) {
				String key = (String)datumiter.next();
				if(key != "id") {
					String value = (String)activeData.get(key);
					json.accumulate(key,  value);
				}
			}
		
			Iterator<?> iter = data.iterator();
			while (iter.hasNext()) {
				HashMap<String, String> datum = (HashMap<String, String>)iter.next();
				datumiter = datum.keySet().iterator();
				while (datumiter.hasNext()) {
					String key = (String)datumiter.next();
					if(key != "id") {
						String value = (String)datum.get(key);
						json.accumulate(key,  value);
					}
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return json;
	}

	public Boolean isAcceptable() {
		if(forceUnacceptable == true) {
			last_unacceptable = new Date();
			forceUnacceptable = false;
			return false;
		}
		if (activeData.containsKey("gsm_rssi")) {
			int gsm_rssi = Integer.parseInt(get("gsm_rssi"));
			/*
			 * SIGNAL_STRENGTH_NONE_OR_UNKNOWN (99) SIGNAL_STRENGTH_GREAT
			 * (16-32) SIGNAL_STRENGTH_GOOD (8-15) SIGNAL_STRENGTH_MODERATE
			 * (4-7) SIGNAL_STRENGTH_POOR (0-3)
			 */
			if (gsm_rssi >= 0 && gsm_rssi < 3 || gsm_rssi == 99) {
				last_unacceptable = new Date();
				return false;
			}
		} else if (activeData.containsKey("called_number") && activeData.containsKey("disconnect_cause")) {
			last_unacceptable = new Date();
			return false;
		}
		return true;
	}

	public long getMillisecondsSinceLastUnacceptable() {
		if (last_unacceptable != null) {
			return new Date().getTime() - last_unacceptable.getTime();
		}
		return 0;
	}

	public long getPeriodMilliseconds() {
		if (period_to != null && period_from != null)
			return period_to.getTime() - period_from.getTime();
		return 0;
	}
}
