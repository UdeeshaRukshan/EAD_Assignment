import React, { useEffect } from 'react';
import { MapContainer, TileLayer, Marker, Popup, useMap } from 'react-leaflet';
import { ChargingStation } from '../types';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';

// Fix for default markers in react-leaflet
delete (L.Icon.Default.prototype as any)._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-icon-2x.png',
  iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-icon.png',
  shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-shadow.png',
});

// Custom icons for station status
const createIcon = (color: string) => new L.Icon({
  iconUrl: `https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-${color}.png`,
  shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-shadow.png',
  iconSize: [25, 41],
  iconAnchor: [12, 41],
  popupAnchor: [1, -34],
  shadowSize: [41, 41]
});

const greenIcon = createIcon('green'); // Available stations
const redIcon = createIcon('red');     // Occupied stations
const orangeIcon = createIcon('orange'); // Partially available
const greyIcon = createIcon('grey');   // Offline/inactive stations

interface StationsMapViewProps {
  stations: ChargingStation[];
  height?: string;
  onStationClick?: (station: ChargingStation) => void;
}

const getStationIcon = (station: ChargingStation) => {
  if (station.status !== 0) return greyIcon; // Not Active (StationStatus.Active = 0)
  
  const availableConnectors = station.connectors?.filter(c => c.isAvailable).length || 0;
  const totalConnectors = station.connectors?.length || 0;
  
  if (availableConnectors === 0) return redIcon;
  if (availableConnectors === totalConnectors) return greenIcon;
  return orangeIcon; // Partially available
};

// Component to fit bounds when stations change
const FitBounds: React.FC<{ stations: ChargingStation[] }> = ({ stations }) => {
  const map = useMap();

  useEffect(() => {
    if (stations.length > 0) {
      const bounds = L.latLngBounds(
        stations.map(station => [station.location.latitude, station.location.longitude])
      );
      map.fitBounds(bounds, { padding: [20, 20] });
    }
  }, [stations, map]);

  return null;
};

const StationsMapView: React.FC<StationsMapViewProps> = ({
  stations,
  height = '500px',
  onStationClick
}) => {
  const defaultCenter: [number, number] = stations.length > 0 
    ? [stations[0].location.latitude, stations[0].location.longitude]
    : [40.7128, -74.0060]; // Default to New York

  return (
    <div className="w-full border border-gray-300 rounded-lg overflow-hidden">
      <div className="bg-gray-50 px-4 py-2 border-b border-gray-200">
        <div className="flex justify-between items-center">
          <p className="font-medium text-gray-900">
            Charging Stations Map ({stations.length} stations)
          </p>
          <div className="flex space-x-4 text-xs">
            <div className="flex items-center">
              <div className="w-3 h-3 bg-green-500 rounded-full mr-1"></div>
              <span>Available</span>
            </div>
            <div className="flex items-center">
              <div className="w-3 h-3 bg-orange-500 rounded-full mr-1"></div>
              <span>Partial</span>
            </div>
            <div className="flex items-center">
              <div className="w-3 h-3 bg-red-500 rounded-full mr-1"></div>
              <span>Occupied</span>
            </div>
            <div className="flex items-center">
              <div className="w-3 h-3 bg-gray-500 rounded-full mr-1"></div>
              <span>Offline</span>
            </div>
          </div>
        </div>
      </div>
      <div style={{ height }}>
        <MapContainer
          center={defaultCenter}
          zoom={10}
          style={{ height: '100%', width: '100%' }}
          scrollWheelZoom={true}
        >
          <TileLayer
            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          />
          <FitBounds stations={stations} />
          {stations.map((station) => (
            <Marker
              key={station.id}
              position={[station.location.latitude, station.location.longitude]}
              icon={getStationIcon(station)}
              eventHandlers={{
                click: () => onStationClick?.(station),
              }}
            >
              <Popup>
                <div className="max-w-sm cursor-pointer" onClick={() => onStationClick?.(station)}>
                  <h3 className="font-bold text-lg mb-2">{station.name}</h3>
                  <div className="space-y-1 text-sm">
                    <p><strong>Address:</strong> {station.address}</p>
                    {station.description && (
                      <p><strong>Description:</strong> {station.description}</p>
                    )}
                    <p>
                      <strong>Status:</strong> 
                      <span className={`ml-1 px-2 py-1 rounded-full text-xs font-medium ${
                        station.status === 0 ? 'bg-green-100 text-green-800' : 
                        station.status === 1 ? 'bg-gray-100 text-gray-800' : 'bg-yellow-100 text-yellow-800'
                      }`}>
                        {station.status === 0 ? 'Active' : 
                         station.status === 1 ? 'Inactive' : 'Maintenance'}
                      </span>
                    </p>
                    <p><strong>Connectors:</strong> {station.connectors?.length || 0}</p>
                    <p><strong>Available:</strong> {station.connectors?.filter(c => c.isAvailable).length || 0}</p>
                    <p><strong>Price:</strong> ${station.pricePerKWh}/kWh</p>
                    <p><strong>Hours:</strong> {station.openingHours}</p>
                    <div className="mt-2 pt-2 border-t border-gray-200">
                      <p className="text-xs text-blue-600 hover:text-blue-800 cursor-pointer">
                        Click for more details →
                      </p>
                    </div>
                  </div>
                </div>
              </Popup>
            </Marker>
          ))}
        </MapContainer>
      </div>
    </div>
  );
};

export default StationsMapView;