import React from 'react';
import { MapContainer, TileLayer, Marker, Popup } from 'react-leaflet';
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
const greyIcon = createIcon('grey');   // Offline stations

interface StationMapViewProps {
  station: ChargingStation;
  height?: string;
  showDetails?: boolean;
}

const getStationIcon = (station: ChargingStation) => {
  const availableConnectors = station.connectors?.filter(c => c.isAvailable).length || 0;
  const totalConnectors = station.connectors?.length || 0;
  
  if (availableConnectors === 0) return redIcon;
  if (availableConnectors === totalConnectors) return greenIcon;
  return greyIcon; // Partially available
};

const StationMapView: React.FC<StationMapViewProps> = ({
  station,
  height = '300px',
  showDetails = true
}) => {
  const center: [number, number] = [station.location.latitude, station.location.longitude];

  return (
    <div className="w-full border border-gray-300 rounded-lg overflow-hidden">
      {showDetails && (
        <div className="bg-gray-50 px-4 py-2 border-b border-gray-200">
          <p className="font-medium text-gray-900">{station.name}</p>
          <p className="text-sm text-gray-600">{station.address}</p>
        </div>
      )}
      <div style={{ height }}>
        <MapContainer
          center={center}
          zoom={15}
          style={{ height: '100%', width: '100%' }}
          scrollWheelZoom={true}
        >
          <TileLayer
            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          />
          <Marker position={center} icon={getStationIcon(station)}>
            <Popup>
              <div className="max-w-sm">
                <h3 className="font-bold text-lg mb-2">{station.name}</h3>
                <div className="space-y-1 text-sm">
                  <p><strong>Address:</strong> {station.address}</p>
                  {station.description && (
                    <p><strong>Description:</strong> {station.description}</p>
                  )}
                  <p><strong>Connectors:</strong> {station.connectors?.length || 0}</p>
                  <p><strong>Available:</strong> {station.connectors?.filter(c => c.isAvailable).length || 0}</p>
                  <p><strong>Price:</strong> ${station.pricePerKWh}/kWh</p>
                  <p><strong>Hours:</strong> {station.openingHours}</p>
                  <div className="mt-2 pt-2 border-t border-gray-200">
                    <p className="text-xs text-gray-500">
                      Lat: {station.location.latitude.toFixed(6)}<br />
                      Lng: {station.location.longitude.toFixed(6)}
                    </p>
                  </div>
                </div>
              </div>
            </Popup>
          </Marker>
        </MapContainer>
      </div>
    </div>
  );
};

export default StationMapView;