package com.solarparkemulator.app;

import static spark.Spark.*;

import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.stream.Stream;

import org.apache.log4j.BasicConfigurator;
import org.json.*;

public class Main {
	// method to read entire file contents and return it in a string
	public static String readFile(String file) throws Exception {
		StringBuilder contentBuilder = new StringBuilder();
		Stream<String> stream;
		if (file.startsWith("configs")) {
			stream = Files.lines(Paths.get(file), StandardCharsets.UTF_8);
		} else {
			stream = Files.lines(Paths.get("src/main/resources/" + file), StandardCharsets.UTF_8);

		}
		stream.forEach(s -> contentBuilder.append(s).append("\n"));
		stream.close();
		return contentBuilder.toString();
	}

	// write string to file - empties file if exists
	public static void writeToFile(String content, String file) throws Exception {
		FileWriter myWriter = new FileWriter(file);
		myWriter.write(content);
		myWriter.close();
	}

	// parse json file and return an arraylist containing all solar parks specified
	// in the configuration file
	private static ArrayList<solarPark> parseJson(String conf) throws JSONException, ParseException {
		// parse
		JSONArray obj = new JSONObject(conf).getJSONArray("parks");
		// init arraylist
		ArrayList<solarPark> ret = new ArrayList<solarPark>();
		// iterate parks in configuration file and set variables of each park
		for (int i = 0; i < obj.length(); i++) {
			JSONObject obj2 = (JSONObject) obj.get(i);
			// create solarpark object
			solarPark park = new solarPark(i);
			park.setPanels(Integer.parseInt(obj2.getString("panels")));
			park.setPanelsAge(Integer.parseInt(obj2.getString("panels_age")));
			park.setPanelsSize(Double.parseDouble(obj2.getString("panels_size")));
			park.setPanelsEfficency(Integer.parseInt(obj2.getString("panels_efficency")));
			park.setPanelsType(Integer.parseInt(obj2.getString("panel_type")));
			park.setSystemlosses(Integer.parseInt(obj2.getString("system_losses")));
			park.setLocation(obj2.getString("location").replace(" ", "+").toLowerCase());
			park.setInclination(Integer.parseInt(obj2.getString("inclination")));
			park.setInstallationType(Integer.parseInt(obj2.getString("installation_type")));
			park.setAzimuth(Integer.parseInt(obj2.getString("azimuth")));
			park.setElectricityRateType(obj2.getString("electricity_rate_type"));
			park.setElectricityRate(Double.parseDouble(obj2.getString("electricity_rate")));
			park.setType(obj2.getString("type"));
			park.setCapacity(Double.parseDouble(obj2.getString("capacity")));
			park.setPr(Double.parseDouble(obj2.getString("pr")));
			ret.add(park);
		}
		return ret;
	}

	// construct GetPowerFlowRealtimeData response
	public static String GetPowerFlowRealtimeData(ArrayList<solarPark> parks, String arg1, String arg2)
			throws Exception {
		// construct response
		JSONObject ar = new JSONObject();
		// construct head
		JSONObject head = new JSONObject();
		head.put("RequestArguments", new JSONObject());
		head.put("Status", new JSONObject().put("Code", "0").put("Reason", "").put("UserMessage", ""));
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		ar.put("Head", head);
		// construct body
		JSONObject body = new JSONObject();
		JSONObject data = new JSONObject().put("Version", "12");
		JSONObject site = new JSONObject();
		double edaytotal = 0.0;
		double eyeartotal = 0.0;
		double etotaltotal = 0.0;
		double ptotal = 0.0;
		site.put("Meter_Location", "unknown");
		site.put("Mode", "produce-only");
		site.put("P_Akku", "null");
		site.put("P_Grid", "null");
		site.put("P_Load", "null");
		site.put("rel_Autonomy", "null");
		site.put("rel_SelfConsumption", "null");
		data.put("Site", site);
		JSONObject inverters = new JSONObject();
		for (int i = 0; i < parks.size(); i++) {
			solarPark a = parks.get(i);

			double e_day = 0.0;
			double e_year = 0.0;
			double e_total = 0.0;
			double p = 0.0;

			long tnow = System.currentTimeMillis() / 1000L;
			if (arg1 != null && arg2 != null) {
				return "only 1 argument allowed";
			}
			if (arg1 != null) {
				try {
					SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
					Date date = new Date();
					date = dateFormat.parse(arg1);
					tnow = (long) date.getTime() / 1000;
				} catch (Exception e) {
					return "cant parse timestamp";
				}
			}
			if (arg2 != null) {
				try {
					tnow = Long.parseLong(arg2);
				} catch (Exception e) {
					return "cant parse unixtimestamp";
				}
			}

			LocalDateTime now = LocalDateTime.ofInstant(Instant.ofEpochSecond(tnow), TimeZone.getDefault().toZoneId());
			head.put("Timestamp", dtf.format(now).replace(" ", "T") + "+03:00");

			long closest = Math.abs(a.getFinalt().get(0) - tnow);
			int index = 0;
			for (int k = 1; k < a.getFinalt().size(); k++) {
				long dt = a.getFinalt().get(k);
				if (Math.abs(dt - tnow) < closest) {
					closest = Math.abs(dt - tnow);
					index = k;
				}
			}
			p = p + a.getFinalp().get(index);
			if (closest > 120)
				head.put("Status",
						new JSONObject().put("Code", "-1").put("Reason",
								"No historical data added. Please wait to get new data (Max waiting time 1hour)")
								.put("UserMessage", "Try again in a few moments or provide a valid timestamp"));
			if (p < 1 || closest > 120)
				p = 0;
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
			Date date = new Date();
			LocalDate localDate = LocalDate.parse(formatter.format(date));
			LocalDateTime startOfDay = localDate.atStartOfDay();
			String time = startOfDay.toString().replace('T', ' ').replace("00:00", "00:01");
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm");
			date = dateFormat.parse(time);
			long unixTime = (long) date.getTime() / 1000;
			Timestamp timestamp = new Timestamp(System.currentTimeMillis());
			tnow = timestamp.getTime() / 1000;

			for (int j = 0; j < a.getFinalt().size(); j++) {
				long t = a.getFinalt().get(j);
				if (t <= tnow && t >= unixTime)
					e_day = e_day + a.getFinalp().get(j);
			}

			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.YEAR, -1); // to get previous year add -1
			date = cal.getTime();
			localDate = LocalDate.parse(formatter.format(date));
			startOfDay = localDate.atStartOfDay();
			time = startOfDay.toString().replace('T', ' ');
			dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm");
			date = dateFormat.parse(time);
			unixTime = (long) date.getTime() / 1000;
			for (int j = 0; j < a.getFinalt().size(); j++) {
				long t = a.getFinalt().get(j);
				if (t <= tnow && t >= unixTime) {
					e_year = e_year + a.getFinalp().get(j);
					e_total = e_total + a.getFinalp().get(j);
				}
			}

			JSONObject t = new JSONObject();
			t.put("DT", a.getType());
			t.put("E_Day", e_day);
			edaytotal = edaytotal + e_day;
			t.put("E_Total", e_total);
			etotaltotal = etotaltotal + e_total;
			t.put("E_Year", e_year);
			eyeartotal = eyeartotal + e_year;
			t.put("P", p);
			ptotal = ptotal + p;
			inverters.put(String.valueOf(i + 1), t);
		}

		site.put("E_Day", edaytotal);
		site.put("E_Total", etotaltotal);
		site.put("E_Year", eyeartotal);
		site.put("P_PV", ptotal);
		data.put("Inverters", inverters);
		body.put("Data", data);
		ar.put("Body", body);

		return ar.toString() + "\n";
	}

	public static ArrayList<solarPark> init() {
		// read configuration file
		String filename = "configs/solarpark.conf";
		String conf = "";
		try {
			conf = readFile(filename);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("error reading file");
			System.exit(-1);
		}
		ArrayList<solarPark> parks = new ArrayList<solarPark>();
		try {
			parks = parseJson(conf);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("error parsing file");
			System.exit(-1);
		}
		try {
			// start simulation for each of the parks in the configuration file after
			// successful initialize
			for (int i = 0; i < parks.size(); i++) {
				parks.get(i).startSimulation();
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("error simulating");
			System.exit(-1);
		}
		return parks;

	}

	public static String GetSensorRealtimeData(String scope, String datacollection, String deviceid,
			ArrayList<solarPark> parks, String arg1, String arg2) throws Exception {
		if (scope.equals("System") && datacollection.equals("NowSensorData")) {
			// construct response
			JSONObject ar = new JSONObject();
			// construct head
			JSONObject head = new JSONObject();
			head.put("RequestArguments", new JSONObject().put("DataCollection", "NowSensorData")
					.put("DeviceClass", "SensorCard").put("Scope", "System"));
			head.put("Status", new JSONObject().put("Code", "0").put("Reason", "").put("UserMessage", ""));
			DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
			ar.put("Head", head);

			long tnow = System.currentTimeMillis() / 1000L;
			if (arg1 != null && arg2 != null) {
				return "only 1 argument allowed";
			}
			if (arg1 != null) {
				try {
					SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
					Date date = new Date();
					date = dateFormat.parse(arg1);
					tnow = (long) date.getTime() / 1000;
				} catch (Exception e) {
					return "cant parse timestamp";
				}
			}
			if (arg2 != null) {
				try {
					tnow = Long.parseLong(arg2);
				} catch (Exception e) {
					return "cant parse unixtimestamp";
				}
			}

			LocalDateTime now = LocalDateTime.ofInstant(Instant.ofEpochSecond(tnow), TimeZone.getDefault().toZoneId());
			head.put("Timestamp", dtf.format(now).replace(" ", "T") + "+03:00");

			// get airtemp and irradiance
			solarPark a = parks.get(0);

			ArrayList<Long> times = new ArrayList<Long>();
			ArrayList<Double> air = new ArrayList<Double>();
			ArrayList<Integer> ghit = new ArrayList<Integer>();

			long before = a.getTimestamps().get(0);
			double beforeair = a.getAir_temp().get(0);
			int beforeghi = a.getGhi().get(0);
			for (int i = 1; i < a.getTimestamps().size(); i++) {
				long after = a.getTimestamps().get(i);
				double afterair = a.getAir_temp().get(i);
				int afterghi = a.getGhi().get(i);
				int count = 0;
				if (after - before < 60) {
					continue;
				}
				for (long k = before; k < after; k = k + 60) {
					times.add(k);
					count++;
				}
				double stepair = (afterair - beforeair) / count;
				int stepghi = ((int) ((afterghi - beforeghi) / count));
				for (int k = 0; k < count; k++) {
					air.add(beforeair);
					ghit.add(beforeghi);
					beforeair = beforeair + stepair;
					beforeghi = beforeghi + stepghi;
				}

				before = after;
				beforeair = afterair;
				beforeghi = afterghi;
			}

			long closest = Math.abs(times.get(0) - tnow);
			int index = 0;
			for (int k = 1; k < times.size(); k++) {
				long dt = times.get(k);
				if (Math.abs(dt - tnow) < closest) {
					closest = Math.abs(dt - tnow);
					index = k;
				}
			}
			index = index - 3 * 60;
			double airtemp = air.get(index);
			int ghi = ghit.get(index);

			if (closest > 120) {
				head.put("Status",
						new JSONObject().put("Code", "-1").put("Reason",
								"No historical data added. Please wait to get new data (Max waiting time 1hour)")
								.put("UserMessage", "Try again in a few moments or provide valid timestamp"));
				airtemp = 0.0;
				ghi = 0;
			}
			// construct body
			JSONObject body = new JSONObject();
			JSONObject data = new JSONObject();

			JSONObject inverters = new JSONObject();

			JSONObject t = new JSONObject();
			t.put("Unit", "°C");
			t.put("Value", String.valueOf(airtemp));
			inverters.put("0", t);// airtemp

			JSONObject t1 = new JSONObject();
			t1.put("Unit", "°C");
			t1.put("Value", String.valueOf(airtemp));
			inverters.put("1", t1);// airtemp

			JSONObject t2 = new JSONObject();
			t2.put("Unit", "W/m2");
			t2.put("Value", String.valueOf(ghi));// irradiance
			inverters.put("2", t2);

			JSONObject t3 = new JSONObject();
			t3.put("Unit", "km/h");
			t3.put("Value", "0");// windspeed
			inverters.put("3", t3);

			data.put("1", inverters);
			body.put("Data", data);
			ar.put("Body", body);

			return ar.toString() + "\n";
		} else {
			return "Wrong arguments";
		}

	}

	public static void main(String[] args) {
		BasicConfigurator.configure();
		// port(8080);
		get("/solar_api/v1/GetPowerFlowRealtimeData.fcgi", (request, response) -> {
			String timestamp = null;
			try {
				timestamp = request.queryParams("Timestamp").replace("T", " ");
			} catch (Exception e) {
				System.out.println();
			}
			String unixtimestamp = request.queryParams("UnixTimestamp");
			return GetPowerFlowRealtimeData(init(), timestamp, unixtimestamp);
		});
		get("/solar_api/v1/GetSensorRealtimeData.cgi", (request, response) -> {
			String timestamp = null;
			try {
				timestamp = request.queryParams("Timestamp").replace("T", " ");
			} catch (Exception e) {
				System.out.println();
			}
			String unixtimestamp = request.queryParams("UnixTimestamp");
			String scope = request.queryParams("Scope");
			String datacollection = request.queryParams("DataCollection");
			String deviceid = request.queryParams("DeviceId");
			return GetSensorRealtimeData(scope, datacollection, deviceid, init(), timestamp, unixtimestamp);
		});

	}
}
