from datetime import datetime
import os
from venv import logger
import requests
from prometheus_client.metrics_core import GaugeMetricFamily
from prometheus_client.registry import CollectorRegistry
import json

url = str(os.getenv("FRONIUS_URL", "http://0.0.0.0:4567"))
sensors_path = str(os.getenv("FRONIUS_SENSORS_PATH",
                             "solar_api/v1/GetSensorRealtimeData.cgi?DataCollection=NowSensorData&Scope=System"))
pv_path = str(os.getenv("FRONIUS_PV_PATH", "solar_api/v1/GetPowerFlowRealtimeData.fcgi"))
prefix = str(os.getenv("PREFIX", "cyprus__nicosia__ucy__dc1__fronius_"))
timeout = int(os.getenv("FRONIUS_TIMEOUT", 15))

registry = CollectorRegistry()


class CustomCollector(object):
    __stored_data = {}

    def collect(self):
        try:
            logger.error(f"http://{url}/{pv_path}")
            power_request = requests.get(f"http://{url}/{pv_path}")
            tmp = power_request.text.replace('null', '0.0')
            power_request = json.loads(tmp)
            self.__stored_data['power_request'] = power_request
        except Exception:
            logger.error("Exception during PVs' data request")
            power_request = self.__stored_data.get('power_request', {})
        power_data, timestamp = self.get_data_and_timestamp(power_request)
        site = power_data.get('Site', {})
        inverters = power_data.get('Inverters', {})
        for id_, inverter in inverters.items():
            yield self.__inverter_output(id_, inverter, timestamp, prefix)
        yield self.__overall_pvs(site, timestamp, prefix)
        yield self.__overall_grid(site, timestamp, prefix)
        yield self.__overall_accumulator(site, timestamp, prefix)
        yield self.__E_Year(site, timestamp, prefix)
        yield self.__E_Day(site, timestamp, prefix)

        try:
            sensors_request = requests.get(f"http://{url}/{sensors_path}")
            tmp=sensors_request.text.replace('null', '0.0')
            logger.error(f"http://{url}/{sensors_path}")
            sensors_request = json.loads(tmp)
            self.__stored_data['sensors_request'] = sensors_request
        except Exception:
            logger.error("Exception during sensors' data request")
            sensors_request = self.__stored_data.get('sensors_request', {})


        sensor_data, timestamp = self.get_data_and_timestamp(sensors_request)
        yield self.__temperature_top(sensor_data, timestamp, prefix)
        yield self.__temperature_bottom(sensor_data, timestamp, prefix)
        yield self.__watts_per_sqm(sensor_data, timestamp, prefix)
        yield self.__wind_speed(sensor_data, timestamp, prefix)

    def get_data_and_timestamp(self, sensors_request):
        sensor_data = sensors_request.get('Body', {}).get('Data', {})
        timestamp = sensors_request.get('Head', {}).get('Timestamp', datetime.now())
        if type(timestamp) == str:
            timestamp = datetime.fromisoformat(timestamp)
        timestamp = timestamp.timestamp()
        return sensor_data, timestamp

    def __temperature_top(self, sensor_data, timestamp, prefix):
        value = sensor_data.get('1', {}).get('0', {}).get("Value", 0.0)
        value = value if value else 0.0
        c = GaugeMetricFamily(f'{prefix}fronius_temperature_output_C_average', '',
                              labels=['chart', 'family', 'dimension'])
        c.add_metric(value=value,
                     labels=['fronius_GetSensorRealtimeData.temperature.top.output', 'sensors', 'temperature'],
                     timestamp=timestamp)
        return c

    def __temperature_bottom(self, sensor_data, timestamp, prefix):
        value = sensor_data.get('1', {}).get('1', {}).get("Value", 0.0)
        value = value if value else 0.0
        c = GaugeMetricFamily(f'{prefix}fronius_temperature_output_C_average', '',
                              labels=['chart', 'family', 'dimension'])
        c.add_metric(value=value,
                     labels=['fronius_GetSensorRealtimeData.temperature.bottom.output', 'sensors', 'temperature'],
                     timestamp=timestamp)
        return c

    def __watts_per_sqm(self, sensor_data, timestamp, prefix):
        value = sensor_data.get('1', {}).get('2', {}).get("Value", 0.0)
        value = value if value else 0.0
        c = GaugeMetricFamily(f'{prefix}fronius_watts_per_sqm_output_W_m2_average', '',
                              labels=['chart', 'family', 'dimension'])
        c.add_metric(value=value,
                     labels=['fronius_GetSensorRealtimeData.watts.per.sqm.output', 'sensors', 'wattsPerSqm'],
                     timestamp=timestamp)
        return c

    def __wind_speed(self, sensor_data, timestamp, prefix):
        value = sensor_data.get('1', {}).get('3', {}).get("Value", 0.0)
        value = value if value else 0.0
        c = GaugeMetricFamily(f'{prefix}fronius_wind_speed_output_km_h_average', '',
                              labels=['chart', 'family', 'dimension'])
        c.add_metric(value=value,
                     labels=['fronius_GetSensorRealtimeData.wind.speed.output', 'sensors', 'windSpeed'],
                     timestamp=timestamp)
        return c

    def __inverter_output(self, id_, inverter, timestamp, prefix):
        P_PV = inverter.get('P', 0.0)
        P_PV = P_PV if P_PV else 0.0
        c = GaugeMetricFamily(f'{prefix}fronius_inverter_output_W_average', '', labels=['chart', 'family', 'dimension'])
        c.add_metric(value=P_PV,
                     labels=['fronius_GetPowerFlowRealtimeData.inverters.output', 'inverters', f'inverter_{id_}'],
                     timestamp=timestamp)
        return c

    def __overall_pvs(self, site, timestamp, prefix):
        P_PV = site.get('P_PV', 0.0)
        P_PV = P_PV if P_PV else 0.0
        c = GaugeMetricFamily(f'{prefix}fronius_power_W_average', '', labels=['chart', 'family', 'dimension'])
        c.add_metric(value=P_PV, labels=['fronius_GetPowerFlowRealtimeData.power', 'power', 'photovoltaics'],
                     timestamp=timestamp)
        return c

    def __overall_grid(self, site, timestamp, prefix):
        P_Grid = site.get('P_Grid')
        P_Grid = P_Grid if P_Grid else 0.0
        c = GaugeMetricFamily(f'{prefix}fronius_power_W_average', '', labels=['chart', 'family', 'dimension'])
        c.add_metric(value=P_Grid, labels=['fronius_GetPowerFlowRealtimeData.power', 'power', 'grid'],
                     timestamp=timestamp)
        return c

    def __overall_accumulator(self, site, timestamp, prefix):  # TODO check the accumulator
        P_Akku = site.get('P_Akku', 0.0)
        P_Akku = P_Akku if P_Akku else 0.0
        c = GaugeMetricFamily(f'{prefix}fronius_power_W_average', '', labels=['chart', 'family', 'dimension'])
        c.add_metric(value=P_Akku, labels=['fronius_GetPowerFlowRealtimeData.power', 'power', 'accumulator'],
                     timestamp=timestamp)
        return c

    def __E_Day(self, site, timestamp, prefix):  # TODO check the accumulator
        E_Day = site.get('E_Day', 0.0)
        E_Day = E_Day / 1000 if E_Day else 0.0
        c = GaugeMetricFamily(f'{prefix}fronius_energy_today_kWh_average', '', labels=['chart', 'family', 'dimension'])
        c.add_metric(value=E_Day, labels=['fronius_GetPowerFlowRealtimeData', 'energy', 'today'], timestamp=timestamp)
        return c

    def __E_Year(self, site, timestamp, prefix):  # TODO check the accumulator
        E_Year = site.get('E_Year', 0.0)
        E_Year = E_Year / 1000 if E_Year else 0.0
        c = GaugeMetricFamily(f'{prefix}fronius_year_kWh_average', '', labels=['chart', 'family', 'dimension'])
        c.add_metric(value=E_Year, labels=['fronius_GetPowerFlowRealtimeData.energy.year', 'energy', 'year'],
                     timestamp=timestamp)
        return c


registry.register(CustomCollector())

def register_to_consul():
    name = os.getenv("FRONIUS_CONTAINER_NAME")
    server = os.getenv("CONSUL_SERVER")
    payload = {
      "ID": name,
      "Name": name,
      "Tags": [
        "fronius"
      ],
      "Address": name,
      "Port": 19999,
      "EnableTagOverride": False,
      "Check": {
        "DeregisterCriticalServiceAfter": "90m",
        "HTTP": f"http://{name}:19999/api/v1/allmetrics?format=prometheus&help=no",
        "Interval": "15s"
      }
    }
    print(json.dumps(payload))
    print(requests.put(f"http://{server}:8500/v1/agent/service/register?replace-existing-checks=1", data=json.dumps(payload)).text)

from flask import Flask
from werkzeug.middleware.dispatcher import DispatcherMiddleware
from prometheus_client import make_wsgi_app

# Create my app
app = Flask(__name__)

# Add prometheus wsgi middleware to route /metrics requests
app.wsgi_app = DispatcherMiddleware(app.wsgi_app, {
    '/api/v1/allmetrics': make_wsgi_app(registry)
})

consul_is_enabled = os.getenv("CONSUL_ENABLED").upper() == "TRUE"
is_register = False
if consul_is_enabled:
    while(not is_register):
        try:
            register_to_consul()
            is_register = True
            print("register success")
        except Exception as e:
            print(e)

app.run(host="0.0.0.0", port=19999)

