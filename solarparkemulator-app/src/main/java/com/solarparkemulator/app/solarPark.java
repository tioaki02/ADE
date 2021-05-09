package com.solarparkemulator.app;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;

//class that simulates an array of pvs
public class solarPark {
	// variables of each array
	private int panels;
	private int panelsAge;
	private double panelsSize;
	private int panelsEfficency;
	private int id;
	private int systemlosses;
	private int inclination;
	private int azimuth;
	private double electricityRate;
	private String location;
	private int panelsType;
	private int installationType;
	private String electricityRateType;
	private ArrayList<solarPanel> park;
	private double lat;
	private double lon;
	private ArrayList<Integer> ghi;
	private ArrayList<Integer> ebh;
	private ArrayList<Integer> dni;
	private ArrayList<Integer> dhi;
	private ArrayList<Double> air_temp;
	private ArrayList<Integer> zenith;
	private ArrayList<Integer> azimuthapi;
	private ArrayList<Double> cloud_opacity;
	private ArrayList<Long> timestamps;
	private double capacity;
	private double pr;
	private String type;
	private ArrayList<Double> finalp;
	private ArrayList<Double> finalpmin;
	private ArrayList<Long> finalt;

	// constructior, initializes some default values for the panels in this array
	public solarPark(int id) throws ParseException {
		this.setId(id);
		park = new ArrayList<solarPanel>();
		panels = 0;
		panelsAge = 0;
		panelsSize = 1.0;
		panelsEfficency = 17;
		systemlosses = 14;
		inclination = 45;
		azimuth = 180;
		electricityRate = 0.233;
		installationType = 0;
		panelsType = 0;
		electricityRateType = "residential";
		ghi = new ArrayList<Integer>();
		ebh = new ArrayList<Integer>();
		dni = new ArrayList<Integer>();
		dhi = new ArrayList<Integer>();
		air_temp = new ArrayList<Double>();
		zenith = new ArrayList<Integer>();
		azimuthapi = new ArrayList<Integer>();
		cloud_opacity = new ArrayList<Double>();
		timestamps = new ArrayList<Long>();
		finalp=new ArrayList<Double>();
		finalpmin=new ArrayList<Double>();
		finalt=new ArrayList<Long>();
	}
	// start simulation for array
	public void startSimulation() throws Exception {
		// get weather data from api
		this.getWeatherData();
		// iterate panels in this array
		for (int i = 0; i < this.panels; i++) {
			// create a panel as a thread
			solarPanel thread = new solarPanel(this, i, this.id);
			// add panel to array
			park.add(thread);
			// start panel-thread
			thread.start();
		}
		// wait until all thread in array finish execution
		for (int i = 0; i < this.panels; i++) {
			park.get(i).join();
		}

		this.combineData();
	}
	public void combineData() {
		int datasize=this.park.get(0).getData().size();
		double sum=0.0;
		for(int j=0;j<this.park.size();j++) {
			solarPanel panel=this.park.get(j);
			sum=sum+panel.getData().get(0);
		}
		this.finalp.add(sum);
		this.finalt.add(this.park.get(0).getDatatimestamps().get(0));
		
		for(int i=1;i<datasize;i++) {
			sum=0.0;
			for(int j=0;j<this.park.size();j++) {
				solarPanel panel=this.park.get(j);
				sum=sum+panel.getData().get(i);
			}
			long nexttime=this.park.get(0).getDatatimestamps().get(i);
			long prevtime=this.finalt.get(this.getFinalt().size()-1)+1;
			double nextp=sum;
			double prevp=this.finalp.get(this.getFinalp().size()-1);
			double step = (nextp - prevp) / (nexttime - prevtime);
			for(long j=prevtime;j<=nexttime;j=j+60) {
				if(prevp<1) {
					prevp=0;
				}
				this.finalp.add(prevp);
				this.finalt.add(j);
				prevp=prevp+step*60;
			}
		}
	}
	public long stringtounix(String time) throws ParseException {
		time = time.replace('T', ' ').replace('Z', ' ').replace(".0000000", "");
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss ");
		if (time.contains(" 12:00:00 ")) {
			dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a");
			time = time + "PM";
		}
		Date date = dateFormat.parse(time);
		long unixTime = (long) date.getTime() / 1000;
		return unixTime;
	}

	// request solar irradiance data
	public void getWeatherData() throws Exception {
		// read historical data from file
		String filename = this.getLocation() + "_Solcast_PT60M.csv";
		String hist = "";
		try {
			hist = Main.readFile(filename);
		} catch (Exception e) {
			try {
				File myObj = new File("src/main/resources/"+filename);
			      if (!myObj.createNewFile()) {
			       throw new Exception();
			      }
			}catch (Exception e2) {
				e2.printStackTrace();
				System.out.println("error reading historical data file");
				System.exit(-1);
			}
		}
		String[] lines = hist.split("\n");
		for (int i = 1; i < lines.length; i++) {
			String[] fields = lines[i].split(",");
			String time = fields[1];
			long unixTime = this.stringtounix(time);
			this.timestamps.add(unixTime);
			this.air_temp.add(Double.parseDouble(fields[3]));
			this.azimuthapi.add(Integer.parseInt(fields[5]));
			this.cloud_opacity.add(Double.parseDouble(fields[6]));
			this.dhi.add(Integer.parseInt(fields[8]));
			this.dni.add(Integer.parseInt(fields[9]));
			this.ebh.add(Integer.parseInt(fields[10]));
			this.ghi.add(Integer.parseInt(fields[11]));
			this.zenith.add(Integer.parseInt(fields[20]));
			if (i > 1 && Math.abs(unixTime - timestamps.get(timestamps.size() - 2)) > 3600) {
				System.out.println("data error");
				java.util.Date time2 = new java.util.Date((long) unixTime * 1000);
				java.util.Date time3 = new java.util.Date((long) timestamps.get(timestamps.size() - 2) * 1000);
				System.out.println(unixTime + " " + timestamps.get(timestamps.size() - 2));
				System.out.println(time2+" "+time3);
				System.out.println();
			}
		}

		// convert location to coordinates
		String apikey = "409cf208db8efe981766b8f47baad952";
		String call = "http://api.positionstack.com/v1/forward?access_key=" + apikey + "&query=" + this.getLocation()
				+ "&limit=10&output=json";
		String mykey = "positionstack" + this.getLocation();
		String data;
		File f = new File("src/main/resources/" + mykey);
		if (f.exists() && !f.isDirectory()) {
			data = Main.readFile(f.getName());
		} else {
			data = this.httpRequest(call, "GET", "");
			Main.writeToFile(data, f.getAbsolutePath());
		}
		JSONArray obj = new JSONObject(data).getJSONArray("data");
		JSONObject obj2 = obj.getJSONObject(0);
		this.setLon(Double.parseDouble(obj2.get("longitude").toString()));
		this.setLat(Double.parseDouble(obj2.get("latitude").toString()));

		// make forecast request
		apikey = "kpJiv1vg8BsMQacqjPnsMgXgQer90ii6";
		call = "https://api.solcast.com.au/world_radiation/forecasts?format=json&latitude="
				+ String.valueOf(this.getLat()) + "&longitude=" + String.valueOf(this.getLon()) + "&hours=168&api_key="
				+ apikey;
		Timestamp timestamp = new Timestamp(System.currentTimeMillis());
		mykey = "solcastforecasts" +this.location+ timestamp.getTime();

		File folder = new File("src/main/resources/");
		File[] listOfFiles = folder.listFiles();
		String tempkey="solcastforecasts"+this.location;
		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].getName().startsWith(tempkey)) {
				String name = listOfFiles[i].getName().replace(tempkey, "");
				long time = Long.parseLong(name);
				long time2 = timestamp.getTime();
				if (time2 - time > 86400000 && !listOfFiles[i].isDirectory()) {
					data = this.httpRequest(call, "GET", "");
					listOfFiles[i].delete();
					Main.writeToFile(data, folder.getAbsolutePath() + "/" + mykey);
				} else {
					data = Main.readFile(listOfFiles[i].getName());
					break;
				}
			}
			if(i==listOfFiles.length-1) {
				data = this.httpRequest(call, "GET", "");
				Main.writeToFile(data, folder.getAbsolutePath() + "/" + mykey);
			}
		}
		obj = new JSONObject(data).getJSONArray("forecasts");
		StringBuffer st=new StringBuffer();
		for (int i = 0; i < obj.length(); i++) {
			String time = obj.getJSONObject(i).getString("period_end");
			long unix = this.stringtounix(time);
			if(i>96) {
				break;
			}
			if (this.timestamps.contains(unix) || time.contains(":30:00.0000")) {
				continue;
			} else {
				this.timestamps.add(unix);
				this.ghi.add(obj.getJSONObject(i).getInt("ghi"));
				this.ebh.add(obj.getJSONObject(i).getInt("ebh"));
				this.dni.add(obj.getJSONObject(i).getInt("dni"));
				this.dhi.add(obj.getJSONObject(i).getInt("dhi"));
				this.air_temp.add(obj.getJSONObject(i).getDouble("air_temp"));
				this.zenith.add(obj.getJSONObject(i).getInt("zenith"));
				this.azimuthapi.add(obj.getJSONObject(i).getInt("azimuth"));
				this.cloud_opacity.add(obj.getJSONObject(i).getDouble("cloud_opacity"));
				String append = "0," + obj.getJSONObject(i).getString("period_end") + ",0,"
						+ obj.getJSONObject(i).getDouble("air_temp") + ",0," + obj.getJSONObject(i).getInt("azimuth")
						+ "," + obj.getJSONObject(i).getDouble("cloud_opacity") + ",0,"
						+ obj.getJSONObject(i).getInt("dhi") + "," + obj.getJSONObject(i).getInt("dni") + ","
						+ obj.getJSONObject(i).getInt("ebh") + "," + obj.getJSONObject(i).getInt("ghi") + ",0,0,0,0,0,0,0,0,"+obj.getJSONObject(i).getInt("zenith")+"\n";
				 st.append(append);
			}
		}
		Main.writeToFile(hist+st.toString(), "src/main/resources/"+filename);
	}

	// meke http request
	// arguments: url, request method, json (leave empty if api doesnt support it)
	public String httpRequest(String uri, String requestmethod, String jsonInputString) throws Exception {
		// construct urlchange
		URL url = new URL(uri);
		// open connection
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setRequestMethod(requestmethod);
		con.setRequestProperty("Content-Type", "application/json; utf-8");
		con.setRequestProperty("Accept", "application/json");
		con.setDoOutput(true);
		// send data
		if (jsonInputString.length() > 0) {
			try (OutputStream os = con.getOutputStream()) {
				byte[] input = jsonInputString.getBytes("utf-8");
				os.write(input, 0, input.length);
			}
		}
		// get response
		try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), "utf-8"))) {
			StringBuilder response = new StringBuilder();
			String responseLine = null;
			while ((responseLine = br.readLine()) != null) {
				response.append(responseLine.trim());
			}
			// return response
			return response.toString();
		}
	}

	// getters and setters
	public int getPanels() {
		return panels;
	}

	public void setPanels(int panels) {
		this.panels = panels;
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

	public double getPanelsSize() {
		return panelsSize;
	}

	public void setPanelsSize(double panelsSize) {
		this.panelsSize = panelsSize;
	}

	public int getPanelsEfficency() {
		return panelsEfficency;
	}

	public void setPanelsEfficency(int panelsEfficency) {
		this.panelsEfficency = panelsEfficency;
	}

	public int getPanelsType() {
		return panelsType;
	}

	public void setPanelsType(int panelsType) {
		this.panelsType = panelsType;
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

	public int getInstallationType() {
		return installationType;
	}

	public void setInstallationType(int installationType) {
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
	public ArrayList<Long> getTimestamps() {
		return timestamps;
	}
	public void setTimestamps(ArrayList<Long> timestamps) {
		this.timestamps = timestamps;
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

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public ArrayList<solarPanel> getPark() {
		return park;
	}

	public void setPark(ArrayList<solarPanel> park) {
		this.park = park;
	}

	public double getLat() {
		return lat;
	}

	public void setLat(double lat) {
		this.lat = lat;
	}

	public double getLon() {
		return lon;
	}

	public void setLon(double lon) {
		this.lon = lon;
	}

	public ArrayList<Integer> getGhi() {
		return ghi;
	}

	public void setGhi(ArrayList<Integer> ghi) {
		this.ghi = ghi;
	}

	public ArrayList<Integer> getEbh() {
		return ebh;
	}

	public void setEbh(ArrayList<Integer> ebh) {
		this.ebh = ebh;
	}

	public ArrayList<Integer> getDni() {
		return dni;
	}

	public void setDni(ArrayList<Integer> dni) {
		this.dni = dni;
	}

	public ArrayList<Integer> getDhi() {
		return dhi;
	}

	public void setDhi(ArrayList<Integer> dhi) {
		this.dhi = dhi;
	}

	public ArrayList<Double> getAir_temp() {
		return air_temp;
	}

	public void setAir_temp(ArrayList<Double> air_temp) {
		this.air_temp = air_temp;
	}

	public ArrayList<Integer> getZenith() {
		return zenith;
	}

	public void setZenith(ArrayList<Integer> zenith) {
		this.zenith = zenith;
	}

	public ArrayList<Integer> getAzimuthapi() {
		return azimuthapi;
	}

	public void setAzimuthapi(ArrayList<Integer> azimuthapi) {
		this.azimuthapi = azimuthapi;
	}

	public ArrayList<Double> getCloud_opacity() {
		return cloud_opacity;
	}

	public void setCloud_opacity(ArrayList<Double> cloud_opacity) {
		this.cloud_opacity = cloud_opacity;
	}

	public ArrayList<Long> getPeriod_end() {
		return timestamps;
	}

	public void setPeriod_end(ArrayList<Long> period_end) {
		this.timestamps = period_end;
	}

	public double getCapacity() {
		return capacity;
	}

	public void setCapacity(double capacity) {
		this.capacity = capacity;
	}

	public double getPr() {
		return pr;
	}

	public void setPr(double pr) {
		this.pr = pr;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
	public ArrayList<Double> getFinalp() {
		return finalp;
	}
	public void setFinalp(ArrayList<Double> finalp) {
		this.finalp = finalp;
	}
	public ArrayList<Long> getFinalt() {
		return finalt;
	}
	public void setFinalt(ArrayList<Long> finalt) {
		this.finalt = finalt;
	}
	public ArrayList<Double> getFinalpmin() {
		return finalpmin;
	}
	public void setFinalpmin(ArrayList<Double> finalpmin) {
		this.finalpmin = finalpmin;
	}

}
