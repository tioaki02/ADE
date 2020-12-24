package solarParkEmulator;

import java.util.ArrayList;

//Size (kW) = Array Area m^2 × 1 kW/m^2 × Module Efficiency (%)
public class solarPanel extends Thread{
	private solarPark park;
	private int panelid;
	private int parkid;
	private ArrayList<Integer> data;
	
	//constructor of panel
	public solarPanel(solarPark park,int id,int parkid) throws Exception {
		this.setPark(park);
		this.setPanelid(id);
		this.setParkid(parkid);
		this.data=new ArrayList<Integer>();
	}
	//code to run in each panel- thread
	public void run() {
	    long start=park.getStarttime();
	    long end=park.getEndtime();
	    int index=0;
	    while(start<end) {
	    	//equation for energy-- REPLACE
	    	int energy=this.park.getPanelsSize()*this.park.getPanelsEfficency()*this.park.getTempC().get(index)*this.park.getSystemlosses()*this.park.getUvIndex().get(index);
	    	data.add(energy);
	    	start=start+park.getInterval();
	    	index++;
	    }
	  }

	//getters and setters
	public ArrayList<Integer> getData() {
		return data;
	}

	public void setData(ArrayList<Integer> data) {
		this.data = data;
	}

	public int getPanelid() {
		return panelid;
	}

	public void setPanelid(int panelid) {
		this.panelid = panelid;
	}

	public int getParkid() {
		return parkid;
	}

	public void setParkid(int parkid) {
		this.parkid = parkid;
	}

	public solarPark getPark() {
		return park;
	}

	public void setPark(solarPark park) {
		this.park = park;
	}

}
