package solarParkEmulator;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.stream.Stream;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class runner {

	//method to read entire file contents and return it in a string
	public static String readFile(String file) throws Exception {

		StringBuilder contentBuilder = new StringBuilder();

		Stream<String> stream = Files.lines(Paths.get(file), StandardCharsets.UTF_8);

		stream.forEach(s -> contentBuilder.append(s).append("\n"));
		stream.close();
		return contentBuilder.toString();
	}
	//parse csv file
	public static void parseCSV(String conf) throws ParseException {
		//tokenize
				StringTokenizer st = new StringTokenizer(conf, "\n");
				int id=-1;
				while (st.hasMoreTokens()) {
					String tok = st.nextToken();
					//skip comments
					if(tok.contains("/*")) {
						while (st.hasMoreTokens()) {
							String tmp = st.nextToken();
							if(tmp.contains("*/")) {
								break;
							}
						}
						continue;
					}
					if(tok.startsWith("//")) {
						continue;
					}
					//start
					if (tok.equals("start_park")) {
						//create park
						id++;
						solarPark park=new solarPark(id);
						//continue to read params until end_park found
						while (!tok.equals("end_park") && st.hasMoreTokens()) {
							tok = st.nextToken();
							//terminal case
							if(tok.equals("end_park")) {
								break;
							}
							//skip comments
							if(tok.startsWith("//")) {
								continue;
							}
							if(tok.contains("/*")) {
								while (st.hasMoreTokens()) {
									String tmp = st.nextToken();
									if(tmp.contains("*/")) {
										break;
									}
								}
								continue;
							}
							//read conf (setting,value)
							String setting[] = tok.split(",");
							String s1 = setting[0];
							String s2 = setting[1];
							//set based on param-switch
							switch (s1) {
							
							case "panels": {
								int param=0;
								try {
									param=Integer.parseInt(s2);
								}catch(Exception e) {
									System.out.println("invalid setting: "+tok);
									continue;
								}
								park.setPanels(param);	
								break;
							}
							case "panel_age": {
								int param=0;
								try {
									param=Integer.parseInt(s2);
								}catch(Exception e) {
									System.out.println("invalid setting: "+tok);
									continue;
								}
								park.setPanelsAge(param);	
								break;
							}
							case "panel_size": {
								int param=0;
								try {
									param=Integer.parseInt(s2);
								}catch(Exception e) {
									System.out.println("invalid setting: "+tok);
									continue;
								}
								park.setPanelsSize(param);	
								break;
							}
							case "panel_efficency": {
								int param=0;
								try {
									param=Integer.parseInt(s2);
								}catch(Exception e) {
									System.out.println("invalid setting: "+tok);
									continue;
								}
								park.setPanelsEfficency(param);	
								break;
							}
							case "panel_type": {
								if(!s2.equals("standard")&&!s2.equals("premium")&&!s2.equals("thin film")) {
									System.out.println("invalid setting: "+tok);
									continue;
								}
									
								park.setPanelsType(s2);	
								break;
							}
							case "dc_system_size": {
								int param=0;
								try {
									param=Integer.parseInt(s2);
								}catch(Exception e) {
									System.out.println("invalid setting: "+tok);
									continue;
								}
								park.setPanelsDC(param);	
								break;
							}
							case "system_losses": {
								int param=0;
								try {
									param=Integer.parseInt(s2);
								}catch(Exception e) {
									System.out.println("invalid setting: "+tok);
									continue;
								}
								park.setSystemlosses(param);	
								break;
							}
							case "inclination": {
								int param=0;
								try {
									param=Integer.parseInt(s2);
								}catch(Exception e) {
									System.out.println("invalid setting: "+tok);
									continue;
								}
								park.setInclination(param);	
								break;
							}
							case "installation_type": {
								if(!s2.equals("fixed")&&!s2.equals("1_axis_tracking")&&!s2.equals("1_axis_backtracking")&&!s2.equals("2_axis_tracking")) {
									System.out.println("invalid setting: "+tok);
									continue;
								}
									
								park.setInstallationType(s2);	
								break;
							}
							
							case "interval": {
								int param=0;
								try {
									param=Integer.parseInt(s2);
								}catch(Exception e) {
									System.out.println("invalid setting: "+tok);
									continue;
								}
								park.setInterval(param);	
								break;
							}
							case "azimuth": {
								int param=0;
								try {
									param=Integer.parseInt(s2);
								}catch(Exception e) {
									System.out.println("invalid setting: "+tok);
									continue;
								}
								park.setAzimuth(param);	
								break;
							}
							
							case "electricity_rate_type": {
								if(!s2.equals("commercial")&&!s2.equals("residential")) {
									System.out.println("invalid setting: "+tok);
									continue;
								}
									
								park.setElectricityRateType(s2);	
								break;
							}
							case "electricity_rate": {
								double param=0;
								try {
									param=Double.parseDouble(s2);
								}catch(Exception e) {
									System.out.println("invalid setting: "+tok);
									continue;
								}
								park.setElectricityRate(param);	
								break;
							}
							
							default:{
								System.out.println("invalid setting: "+tok);
								break;
							}
							}
						}
						
						//initialize park
						if(tok.equals("end_park")) {
							try {
								park.startSimulation();
								System.out.println(park.toString());
							}catch(Exception e) {
								e.printStackTrace();
								System.out.println("error initializing park");
								System.exit(-1);
							}
						}else {
							System.out.println("error in configuration file");
							System.exit(-1);
						}
					}
				}
	}
	//parse json file and return an arraylist containing all solar parks specified in the configuration file
	private static ArrayList<solarPark> parseJson(String conf) throws JSONException, ParseException {
		//parse
		JSONArray obj = new JSONObject(conf).getJSONArray("parks");
		//init arraylist
		ArrayList<solarPark> ret=new ArrayList<solarPark>();
		//iterate parks in configuration file and set variables of each park
		for(int i=0;i<obj.length();i++) {
			JSONObject obj2=(JSONObject) obj.get(i);
			//create solarpark object
			solarPark park=new solarPark(i);
			park.setPanels(Integer.parseInt(obj2.getString("panels")));
			park.setPanelsAge(Integer.parseInt(obj2.getString("panels_age")));
			park.setPanelsSize(Integer.parseInt(obj2.getString("panels_size")));
			park.setPanelsEfficency(Integer.parseInt(obj2.getString("panels_efficency")));
			park.setPanelsType(obj2.getString("panel_type"));
			park.setSystemlosses(Integer.parseInt(obj2.getString("system_losses")));
			park.setLocation(obj2.getString("location").replace(" ", "+"));
			park.setInclination(Integer.parseInt(obj2.getString("inclination")));
			park.setInstallationType(obj2.getString("installation_type"));
			park.setInterval(Integer.parseInt(obj2.getString("interval")));
			park.setAzimuth(Integer.parseInt(obj2.getString("azimuth")));
			park.setElectricityRateType(obj2.getString("electricity_rate_type"));
			park.setElectricityRate(Double.parseDouble(obj2.getString("electricity_rate")));
			park.setStarttime(obj2.getString("datefrom"));
			park.setEndtime(obj2.getString("dateto"));
			park.setDatefrom(obj2.getString("datefrom"));
			park.setDateto(obj2.getString("dateto"));
			ret.add(park);
		}
		return ret;
	}

	
	public static void main(String[] args) throws Exception {
		//read configuration file
		String filename = "solarpark.conf";
		String conf="";
		try {
			conf = readFile(filename);
		}
		catch(Exception e) {
			e.printStackTrace();
			System.out.println("error reading file");
			System.exit(-1);
		}
		//parse json
		ArrayList<solarPark> parks=parseJson(conf);
		//start simulation for each of the parks in the configuration file after successful initialize
		for(int i=0;i<parks.size();i++) {
			parks.get(i).startSimulation();
		}

	}
}
