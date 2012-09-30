package com.ae.apps.pnrstatus.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.util.Log;

import com.ae.apps.pnrstatus.exceptions.StatusException;
import com.ae.apps.pnrstatus.utils.AppConstants;
import com.ae.apps.pnrstatus.utils.PNRUtils;
import com.ae.apps.pnrstatus.vo.PNRStatusVo;
import com.ae.apps.pnrstatus.vo.PassengerDataVo;

public class IxigoService implements IStatusService {

	/**
	 * The URL for IXIGO Service
	 */
	private final String	url			= "http://216.139.222.96:80/train/pnr_status";
	private final String	serviceName	= "IXIGO Service";

	@Override
	public String getServiceName() {
		return serviceName;
	}

	public String getStubResponse() {
		String response = "{\"passengers\": [{\"trainBookingBerth\": \"S5  , 43,GN    \",\"trainPassenger\": \"Passenger 1\",\"trainCurrentStatus\": \"   CNF  \"},{\"trainBookingBerth\": \"S5  , 46,GN    \",\"trainPassenger\": \"Passenger 2\",\"trainCurrentStatus\": \"   CNF  \"},{\"trainBookingBerth\": \"S5  , 42,GN    \",\"trainPassenger\": \"Passenger 3\",\"trainCurrentStatus\": \"   CNF  \"},{\"trainBookingBerth\": \"S5  , 45,GN    \",\"trainPassenger\": \"Passenger 4\",\"trainCurrentStatus\": \"   CNF  \"}],\"trainDest\": \"Chennai Central\",\"trainOrigin\": \"Eranakulam Jn\",\"trainFareClass\": \" SL\",\"chartStat\": \" CHART NOT PREPARED \",\"trainBoard\": \"Eranakulam Jn\",\"trainEmbark\": \"Chennai Central\",\"trainNo\": \"*16042\",\"trainName\": \"CHENNAI EXPRESS\",\"trainJourney\": \"26-12-2010\"}";
		// String response =
		// "{\"passengers\": [{\"trainBookingBerth\": \"S3  , 14,GN    \",\"trainPassenger\": \"Passenger 1\",\"trainCurrentStatus\": \"   CNF  \"}],\"trainDest\": \"Chennai Central\",\"trainOrigin\": \"Eranakulam Jn\",\"trainFareClass\": \" SL\",\"chartStat\": \" CHART NOT PREPARED \",\"trainBoard\": \"Eranakulam Jn\",\"trainEmbark\": \"Chennai Central\",\"trainNo\": \"*16042\",\"trainName\": \"CHENNAI EXPRESS\",\"trainJourney\": \"14-11-2011\"}";
		return response;
	}

	@Override
	public PNRStatusVo getResponse(String pnrNumber) throws JSONException, StatusException, IOException {
		String searchUrl = getServiceUrl(pnrNumber);
		Log.i(AppConstants.TAG, "Using " + getServiceName());
		Log.d(AppConstants.TAG, "SearchURL :  " + searchUrl);

		String response = PNRUtils.getWebResult(searchUrl);
		// String response = pnrService.getStubResponse();

		Log.d(AppConstants.TAG, "WebResultResponse : " + response);
		return parseResponse(response);
	}

	@Override
	public PNRStatusVo getResponse(String pnrNumber, Boolean stubResponse) throws JSONException, StatusException,
			IOException {
		if (stubResponse == true) {
			return parseResponse(getStubResponse());
		}
		return getResponse(pnrNumber);
	}

	/**
	 * Returns the
	 */

	private String getServiceUrl(String pnrNumber) {
		if (null != pnrNumber && !pnrNumber.equals("")) {
			String pnr1 = pnrNumber.substring(0, 3);
			String pnr2 = pnrNumber.substring(3);

			return url + "?pnr1=" + pnr1 + "&pnr2=" + pnr2;
		}
		return "";
	}

	/**
	 * This function parses the response
	 */
	protected PNRStatusVo parseResponse(String responseString) throws JSONException, StatusException {
		PNRStatusVo statusVo = new PNRStatusVo();

		JSONTokener jsonTokener = new JSONTokener(responseString);
		JSONObject object = (JSONObject) jsonTokener.nextValue();

		JSONArray passengersArray = object.getJSONArray("passengers");

		String firstPassengerStatus = "";
		String trainDest = object.getString("trainDest");
		String trainJourney = object.getString("trainJourney");
		String trainName = object.getString("trainName");
		String trainNo = PNRUtils.getTrainNo(object.getString("trainNo"));
		String trainBoard = object.getString("trainBoard");
		String trainEmbark = object.getString("trainEmbark");
		String ticketClass = object.getString("trainFareClass");

		// Read the PassengerDataVos
		List<PassengerDataVo> passengers = new ArrayList<PassengerDataVo>();

		for (int i = 0; i < passengersArray.length(); i++) {
			JSONObject object2 = passengersArray.getJSONObject(i);
			String trainBookingBerth = object2.getString("trainBookingBerth").trim();
			String trainCurrentStatus = object2.getString("trainCurrentStatus").trim();
			String trainPassenger = object2.getString("trainPassenger");
			String berthPosition = "";

			// Calculate the BerthPosition
			berthPosition = PNRUtils.getBerthPosition(trainCurrentStatus, trainBookingBerth,
					AppConstants.CLASS_UNKNOWN, ",");

			PassengerDataVo dataVo = new PassengerDataVo();
			dataVo.setTrainBookingBerth(trainBookingBerth);
			dataVo.setTrainCurrentStatus(trainCurrentStatus);
			dataVo.setTrainPassenger(trainPassenger);
			dataVo.setBerthPosition(berthPosition);

			passengers.add(dataVo);
		}

		// Get the first passenger status
		if (passengers.size() > 0) {
			PassengerDataVo dataVo = (passengers.get(0));
			statusVo.setFirstPassengerData(dataVo);
			firstPassengerStatus = dataVo.getTrainCurrentStatus();
		}

		// Set the values for the StatusVo
		statusVo.setTrainBoard(trainBoard);
		statusVo.setDestination(trainDest);
		statusVo.setTrainEmbark(trainEmbark);
		statusVo.setTrainJourney(trainJourney);
		statusVo.setTrainName(trainName);
		statusVo.setTrainNo(trainNo);
		statusVo.setTicketClass(ticketClass);
		statusVo.setCurrentStatus(firstPassengerStatus);

		statusVo.setPassengers(passengers);

		return statusVo;
	}
}