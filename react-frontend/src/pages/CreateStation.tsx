import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { CreateStationRequest, ConnectorType, ConnectorStatus } from '../types';
import { apiService } from '../services/apiService';

const CreateStation: React.FC = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  
  const [formData, setFormData] = useState<CreateStationRequest>({
    name: '',
    description: '',
    location: { latitude: 0, longitude: 0 },
    address: '',
    connectors: [{ type: ConnectorType.Type2, power: 22, isAvailable: true, status: ConnectorStatus.Available }],
    amenities: [],
    openingHours: '24/7',
    pricePerKWh: 0.35,
    imageUrls: [],
  });

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    try {
      await apiService.createChargingStation(formData);
      navigate('/stations');
    } catch (err) {
      setError('Failed to create charging station');
      console.error('Create station error:', err);
    } finally {
      setLoading(false);
    }
  };

  const addConnector = () => {
    setFormData({
      ...formData,
      connectors: [
        ...formData.connectors,
        { type: ConnectorType.Type2, power: 22, isAvailable: true, status: ConnectorStatus.Available }
      ]
    });
  };

  const removeConnector = (index: number) => {
    const newConnectors = formData.connectors.filter((_, i) => i !== index);
    setFormData({ ...formData, connectors: newConnectors });
  };

  const updateConnector = (index: number, field: keyof typeof formData.connectors[0], value: any) => {
    const newConnectors = [...formData.connectors];
    newConnectors[index] = { ...newConnectors[index], [field]: value };
    setFormData({ ...formData, connectors: newConnectors });
  };

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="bg-white rounded-lg shadow p-6">
          <div className="mb-6">
            <h1 className="text-2xl font-bold text-gray-900">Create New Charging Station</h1>
            <p className="text-gray-600">Add a new charging station to your network</p>
          </div>

          {error && (
            <div className="mb-6 bg-red-50 border border-red-200 text-red-600 px-4 py-3 rounded-md">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-6">
            {/* Basic Information */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Station Name *
                </label>
                <input
                  type="text"
                  required
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                  placeholder="Enter station name"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Price per kWh *
                </label>
                <input
                  type="number"
                  step="0.01"
                  required
                  value={formData.pricePerKWh}
                  onChange={(e) => setFormData({ ...formData, pricePerKWh: parseFloat(e.target.value) })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                  placeholder="0.35"
                />
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Description
              </label>
              <textarea
                value={formData.description}
                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                rows={3}
                placeholder="Enter station description"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Address *
              </label>
              <input
                type="text"
                required
                value={formData.address}
                onChange={(e) => setFormData({ ...formData, address: e.target.value })}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder="Enter full address"
              />
            </div>

            {/* Location */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Latitude *
                </label>
                <input
                  type="number"
                  step="any"
                  required
                  value={formData.location.latitude}
                  onChange={(e) => setFormData({ 
                    ...formData, 
                    location: { ...formData.location, latitude: parseFloat(e.target.value) }
                  })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                  placeholder="34.0522"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Longitude *
                </label>
                <input
                  type="number"
                  step="any"
                  required
                  value={formData.location.longitude}
                  onChange={(e) => setFormData({ 
                    ...formData, 
                    location: { ...formData.location, longitude: parseFloat(e.target.value) }
                  })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                  placeholder="-118.2437"
                />
              </div>
            </div>

            {/* Connectors */}
            <div>
              <div className="flex justify-between items-center mb-4">
                <label className="block text-sm font-medium text-gray-700">
                  Connectors *
                </label>
                <button
                  type="button"
                  onClick={addConnector}
                  className="bg-green-600 hover:bg-green-700 text-white px-3 py-1 rounded text-sm"
                >
                  Add Connector
                </button>
              </div>
              {formData.connectors.map((connector, index) => (
                <div key={index} className="border border-gray-200 rounded-md p-4 mb-4">
                  <div className="flex justify-between items-center mb-3">
                    <h4 className="font-medium">Connector {index + 1}</h4>
                    {formData.connectors.length > 1 && (
                      <button
                        type="button"
                        onClick={() => removeConnector(index)}
                        className="text-red-600 hover:text-red-800"
                      >
                        Remove
                      </button>
                    )}
                  </div>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div>
                      <label className="block text-sm text-gray-600 mb-1">Type</label>
                      <select
                        value={connector.type}
                        onChange={(e) => updateConnector(index, 'type', parseInt(e.target.value))}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                      >
                        <option value={ConnectorType.Type1}>Type 1</option>
                        <option value={ConnectorType.Type2}>Type 2</option>
                        <option value={ConnectorType.CHAdeMO}>CHAdeMO</option>
                        <option value={ConnectorType.CCS}>CCS</option>
                        <option value={ConnectorType.TeslaSuper}>Tesla Supercharger</option>
                      </select>
                    </div>
                    <div>
                      <label className="block text-sm text-gray-600 mb-1">Power (kW)</label>
                      <input
                        type="number"
                        value={connector.power}
                        onChange={(e) => updateConnector(index, 'power', parseFloat(e.target.value))}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                        placeholder="22"
                      />
                    </div>
                  </div>
                </div>
              ))}
            </div>

            {/* Opening Hours */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Opening Hours
              </label>
              <input
                type="text"
                value={formData.openingHours}
                onChange={(e) => setFormData({ ...formData, openingHours: e.target.value })}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder="24/7 or 06:00-22:00"
              />
            </div>

            {/* Actions */}
            <div className="flex justify-end space-x-4 pt-6">
              <button
                type="button"
                onClick={() => navigate('/stations')}
                className="px-4 py-2 border border-gray-300 rounded-md text-gray-700 hover:bg-gray-50"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={loading}
                className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50"
              >
                {loading ? 'Creating...' : 'Create Station'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};

export default CreateStation;