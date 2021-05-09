package com.solarparkemulator.app;

import java.util.ArrayList;

//Size (kW) = Array Area m^2 � 1 kW/m^2 � Module Efficiency (%)
public class solarPanel extends Thread {
	private solarPark park;
	private int panelid;
	private int parkid;
	private ArrayList<Double> data;
	private ArrayList<Long> datatimestamps;

	// constructor of panel
	public solarPanel(solarPark park, int id, int parkid) throws Exception {
		this.setPark(park);
		this.setPanelid(id);
		this.setParkid(parkid);
		this.data = new ArrayList<Double>();
		datatimestamps = new ArrayList<Long>();
	}

	// code to run in each panel- thread
	public void run() {
		int index = 0;
		while (this.park.getGhi().size() > index) {

			// Global variables
			// Reference Irradiance (default value is 1000 W/m2)
			double Eo = 1000;
			// Reference Temperature (default value is 25 C)
			double To = 25;
			// convective heat transfer component not considered (power temperature
			// coeffcient)
			double g = -0.0042;
			// PV module rated power (KW)
			double Pmp0 = 270;

			// Derating factors
			double Soiling = 1; // was 2
			double Shading = 0; // was 3
			double Snow = 0; // was 0
			double Mismatch = 1; // was 2
			double Wiring = 1; // was 2
			double Connections = 0.5; // was 0.5
			double LID = 0; // was 1
			double Nameplate = 1; // was 1
			double Age = this.park.getPanelsAge(); // was 0
			double Availability = 3; // was 3
			double a = -3.47; // Thermal Properties Isc (%/K)
			double b = -0.0594; // Thermal Properties Voc (%/K)

			int Gpoa = this.getPark().getGhi().get(index); // incident irradiance (or global horizontal irradiance)

			// Tamb <- #ambient temperature

			double NOCT = 48; // The best module operated at a NOCT of 33°C, the worst at 58°C and the typical
								// module at 48°C

			double S = 80;// insolation in mW/cm2
			double Deratingfactor = Soiling + Shading + Snow + Mismatch + Wiring + Connections + LID + Nameplate + Age
					+ Availability + a + b;

			double Tm = this.park.getAir_temp().get(index) + ((NOCT - 20) / 80) * S; // module temperature (calculated
																						// from ambient temperature)

			// Predicted power production with PVwatts Model
			// Check for the level of irradiance
			double p = 0;
			if (Gpoa >= 125) {
				p = ((Gpoa / Eo) * Pmp0 * (1 + g * (Tm - To))) * ((100 - Deratingfactor) / 100);
			} else {
				p = ((0.008 * (Gpoa ^ 2) / Eo) * Pmp0 * (1 + g * (Tm - To))) * ((100 - Deratingfactor) / 100);
			}
			if (p < 1) {
				p = 0;
			}
			// equation for energy
			double s1 = (double) ((100 - (double) this.park.getSystemlosses()) / 100);
			double s2 = (double) (1 - ((100 - this.park.getPr()) / 100));
			double energy = 0.0;
			// if(this.park.getZenith().get(index)<=90) {
			energy = this.park.getPanelsSize() * p * s1 * s2;// *this.park.getZenith().get(index)/90;
			// }
			// double energy=p;
			data.add(energy);
			this.datatimestamps.add(this.getPark().getTimestamps().get(index) + 3600 * 3);
			index++;
		}
	}

	// getters and setters
	public ArrayList<Long> getDatatimestamps() {
		return datatimestamps;
	}

	public void setDatatimestamps(ArrayList<Long> datatimestamps) {
		this.datatimestamps = datatimestamps;
	}

	public ArrayList<Double> getData() {
		return data;
	}

	public void setData(ArrayList<Double> data) {
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
