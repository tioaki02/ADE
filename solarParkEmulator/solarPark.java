package solarParkEmulator;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

//https://api.worldweatheronline.com/premium/v1/past-weather.ashx?q=%22latsia%22&key=20b902398c404da18bb152224201611&date=2009-07-20&format=json&tp=1

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;
//class that simulates an array of pvs
public class solarPark {
	//variables of each array
	private int panels;
	private int panelsAge;
	private int panelsSize;
	private int panelsEfficency;
	private int panelsDC;
	private int id;
	private int systemlosses;
	private int inclination;
	private int azimuth;
	private double electricityRate;
	private int interval;
	private String location;
	private String panelsType;
	private String installationType;
	private String electricityRateType;
	private ArrayList<solarPanel> park;
	private String jsontype;
	private String jsonquery;
	private long starttime;
	private long endtime;
	private String datefrom;
	private String dateto;
	private ArrayList<Integer> tempC;
	private ArrayList<Integer> uvIndex;

	//constructior, initializes some default values for the panels in this array
	public solarPark(int id) throws ParseException {
		this.setId(id);
		park = new ArrayList<solarPanel>();
		panels = 0;
		panelsAge = 0;
		panelsSize = 1;
		panelsEfficency = 17;
		systemlosses = 14;
		inclination = 45;
		azimuth = 180;
		electricityRate = 0.233;
		interval = 5000;
		installationType = "fixed";
		panelsType = "standard";
		electricityRateType = "residential";
	}
	//start simulation for array
	public void startSimulation() throws Exception {
		System.out.println("starting simulation for park with id "+this.id);
		//get weather data from api
		this.getWeatherData();
		//iterate panels in this array
		for (int i = 0; i < this.panels; i++) {
			//create a panel as a thread
			solarPanel thread = new solarPanel(this, i, this.id);
			//add panel to array
			park.add(thread);
			//start panel-thread
			thread.start();
		}
		//wait until all thread in array finish execution
		for (int i = 0; i < this.panels; i++) {
			park.get(i).join();
		}
		//export data to csv
		this.exportCsv();
		System.out.println("simulation end");
	}

	//export data to csv
	public void exportCsv() throws IOException {
		//create file
		Date date = new Date();
		FileWriter out = new FileWriter(date.toString().replace(" ", "-").replace(":", "-") + "logPark"+this.id+".csv", true);
		//headers
		for (int i = 0; i < this.panels; i++) {
			out.append("panel " + i + ',');
		}
		out.append("\n");
		//data
		for (int j = 0; j < this.park.get(0).getData().size(); j++) {
			for (int i = 0; i < this.panels; i++) {
				int r = this.park.get(i).getData().get(j);
				out.append(String.valueOf(r) + ',');
			}
			out.append("\n");
		}
		out.close();
	}

	//request weather data for date of simulation
	public void getWeatherData() throws Exception {
		//construct request
		String request = "https://api.worldweatheronline.com/premium/v1/past-weather.ashx?q=" + this.getLocation()
				+ "&key=&date=" + this.getDatefrom() + "&enddate=" + this.getDateto()
				+ "&format=json&tp=1";
		//make httprequest
		String data = this.httpRequest(request, "GET", "");
		//parse response to json
		JSONObject obj = new JSONObject(data);
		System.out.println(request);
		//parse data to array
		this.setJsontype(obj.getJSONObject("data").getJSONArray("request").getJSONObject(0).getString("type"));
		this.setJsonquery(obj.getJSONObject("data").getJSONArray("request").getJSONObject(0).getString("query"));
		JSONArray weather = obj.getJSONObject("data").getJSONArray("weather");
		this.tempC = new ArrayList<Integer>();
		this.uvIndex=new ArrayList<Integer>();
		for (int i = 0; i < weather.length(); i++) {
			JSONArray hourly = weather.getJSONObject(i).getJSONArray("hourly");
			for (int j = 0; j < hourly.length(); j++) {
				int temp = Integer.parseInt(hourly.getJSONObject(j).get("tempC").toString());
				int uv=Integer.parseInt(hourly.getJSONObject(j).get("uvIndex").toString());
				//api gives values for each hour, if the interval is less we broadcast the data to the length of an hour
				for (int mil = 0; mil < 3600000; mil = mil + this.interval) {
					this.tempC.add(temp);
					this.uvIndex.add(uv-1);
				}

			}
		}
		
	}
	//meke http request
	//arguments: url, request method, json (leave empty if api doesnt support it)
	public String httpRequest(String uri, String requestmethod, String jsonInputString) throws Exception {
		//construct url
		URL url = new URL(uri);
		//open connection
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setRequestMethod(requestmethod);
		con.setRequestProperty("Content-Type", "application/json; utf-8");
		con.setRequestProperty("Accept", "application/json");
		con.setDoOutput(true);
		//send data
		try (OutputStream os = con.getOutputStream()) {
			byte[] input = jsonInputString.getBytes("utf-8");
			os.write(input, 0, input.length);
		}
		//get response
		try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), "utf-8"))) {
			StringBuilder response = new StringBuilder();
			String responseLine = null;
			while ((responseLine = br.readLine()) != null) {
				response.append(responseLine.trim());
			}
			//return response
			return response.toString();
		}
	}

	//getters and setters
	public ArrayList<Integer> getTempC() {
		return tempC;
	}

	public void setTempC(ArrayList<Integer> tempC) {
		this.tempC = tempC;
	}

	public int getPanels() {
		return panels;
	}

	public void setPanels(int panels) {
		this.panels = panels;
	}

	public int getInterval() {
		return interval;
	}

	public void setInterval(int interval) {
		this.interval = interval;
	}

	public int getPanelsAge() {
		return panelsAge;
	}

	public void setPanelsAge(int panelsAge) {
		this.panelsAge = panelsAge;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getPanelsSize() {
		return panelsSize;
	}

	public void setPanelsSize(int panelsSize) {
		this.panelsSize = panelsSize;
	}

	public int getPanelsEfficency() {
		return panelsEfficency;
	}

	public void setPanelsEfficency(int panelsEfficency) {
		this.panelsEfficency = panelsEfficency;
	}

	public String getPanelsType() {
		return panelsType;
	}

	public void setPanelsType(String panelsType) {
		this.panelsType = panelsType;
	}

	public int getPanelsDC() {
		return panelsDC;
	}

	public void setPanelsDC(int panelsDC) {
		this.panelsDC = panelsDC;
	}

	public int getSystemlosses() {
		return systemlosses;
	}

	public void setSystemlosses(int systemlosses) {
		this.systemlosses = systemlosses;
	}

	public int getInclination() {
		return inclination;
	}

	public void setInclination(int inclination) {
		this.inclination = inclination;
	}

	public String getInstallationType() {
		return installationType;
	}

	public void setInstallationType(String installationType) {
		this.installationType = installationType;
	}

	public int getAzimuth() {
		return azimuth;
	}

	public void setAzimuth(int azimuth) {
		this.azimuth = azimuth;
	}

	public String getElectricityRateType() {
		return electricityRateType;
	}

	public void setElectricityRateType(String electricityRateType) {
		this.electricityRateType = electricityRateType;
	}

	public double getElectricityRate() {
		return electricityRate;
	}

	public void setElectricityRate(double electricityRate) {
		this.electricityRate = electricityRate;
	}

	public String getJsontype() {
		return jsontype;
	}

	public void setJsontype(String jsontype) {
		this.jsontype = jsontype;
	}

	public String getJsonquery() {
		return jsonquery;
	}

	public void setJsonquery(String jsonquery) {
		this.jsonquery = jsonquery;
	}

	public long getStarttime() {
		return starttime;
	}

	public void setStarttime(String starttime) throws ParseException {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Date date = dateFormat.parse(starttime);
		long time = date.getTime();
		Timestamp ts = new Timestamp(time);
		this.starttime = ts.getTime();
	}

	public long getEndtime() {
		return endtime;
	}

	public void setEndtime(String endtime) throws ParseException {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Date date = dateFormat.parse(endtime);
		long time = date.getTime();
		Timestamp ts = new Timestamp(time);
		this.endtime = ts.getTime();
	}

	public static void main(String[] args) throws Exception {
		solarPark r = new solarPark(0);
		r.getWeatherData();

	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getDatefrom() {
		return datefrom;
	}

	public void setDatefrom(String datefrom) {
		this.datefrom = datefrom;
	}

	public String getDateto() {
		return dateto;
	}

	public void setDateto(String dateto) {
		this.dateto = dateto;
	}
	public ArrayList<solarPanel> getPark() {
		return park;
	}

	public void setPark(ArrayList<solarPanel> park) {
		this.park = park;
	}

	public ArrayList<Integer> getUvIndex() {
		return uvIndex;
	}

	public void setUvIndex(ArrayList<Integer> uvIndex) {
		this.uvIndex = uvIndex;
	}

	public void setStarttime(long starttime) {
		this.starttime = starttime;
	}

	public void setEndtime(long endtime) {
		this.endtime = endtime;
	}


}
