import React, { useEffect, useState } from 'react';
import { MapContainer, TileLayer, Marker, useMapEvents, Popup } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';

// Fix for default markers in react-leaflet
delete (L.Icon.Default.prototype as any)._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-icon-2x.png',
  iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-icon.png',
  shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-shadow.png',
});

// Custom icons for different purposes
const createIcon = (color: string) => new L.Icon({
  iconUrl: `https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-${color}.png`,
  shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-shadow.png',
  iconSize: [25, 41],
  iconAnchor: [12, 41],
  popupAnchor: [1, -34],
  shadowSize: [41, 41]
});

const greenIcon = createIcon('green');
const blueIcon = createIcon('blue');

interface Location {
  latitude: number;
  longitude: number;
}

interface LocationSelectorProps {
  selectedLocation?: Location;
  onLocationSelect: (location: Location) => void;
  height?: string;
  isReadOnly?: boolean;
}

// Component to handle map clicks
function LocationMarker({ onLocationSelect, selectedLocation, isReadOnly }: {
  onLocationSelect: (location: Location) => void;
  selectedLocation?: Location;
  isReadOnly?: boolean;
}) {
  const [position, setPosition] = useState<[number, number] | null>(
    selectedLocation ? [selectedLocation.latitude, selectedLocation.longitude] : null
  );

  const map = useMapEvents({
    click(e) {
      if (!isReadOnly) {
        const { lat, lng } = e.latlng;
        setPosition([lat, lng]);
        onLocationSelect({ latitude: lat, longitude: lng });
      }
    },
  });

  useEffect(() => {
    if (selectedLocation) {
      setPosition([selectedLocation.latitude, selectedLocation.longitude]);
      map.flyTo([selectedLocation.latitude, selectedLocation.longitude], map.getZoom());
    }
  }, [selectedLocation, map]);

  return position === null ? null : (
    <Marker position={position} icon={isReadOnly ? greenIcon : blueIcon}>
      <Popup>
        <div className="text-center">
          <p className="font-medium">
            {isReadOnly ? 'Station Location' : 'Selected Location'}
          </p>
          <p className="text-sm text-gray-600">
            Lat: {position[0].toFixed(6)}
            <br />
            Lng: {position[1].toFixed(6)}
          </p>
        </div>
      </Popup>
    </Marker>
  );
}

const LocationSelector: React.FC<LocationSelectorProps> = ({
  selectedLocation,
  onLocationSelect,
  height = '400px',
  isReadOnly = false
}) => {
  const defaultCenter: [number, number] = selectedLocation 
    ? [selectedLocation.latitude, selectedLocation.longitude]
    : [40.7128, -74.0060]; // Default to New York

  return (
    <div className="w-full border border-gray-300 rounded-lg overflow-hidden">
      <div className="bg-gray-50 px-4 py-2 border-b border-gray-200">
        <p className="text-sm text-gray-600">
          {isReadOnly 
            ? 'Station location on map' 
            : 'Click on the map to select location'
          }
        </p>
        {selectedLocation && (
          <div className="mt-1 text-xs text-gray-500">
            Coordinates: {selectedLocation.latitude.toFixed(6)}, {selectedLocation.longitude.toFixed(6)}
          </div>
        )}
      </div>
      <div style={{ height }}>
        <MapContainer
          center={defaultCenter}
          zoom={13}
          style={{ height: '100%', width: '100%' }}
          scrollWheelZoom={true}
        >
          <TileLayer
            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          />
          <LocationMarker 
            onLocationSelect={onLocationSelect} 
            selectedLocation={selectedLocation}
            isReadOnly={isReadOnly}
          />
        </MapContainer>
      </div>
    </div>
  );
};

export default LocationSelector;