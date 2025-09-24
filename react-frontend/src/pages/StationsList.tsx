import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { ChargingStation, StationStatus } from '../types';
import { apiService } from '../services/apiService';
import { useAuth } from '../contexts/AuthContext';

const StationsList: React.FC = () => {
  const [stations, setStations] = useState<ChargingStation[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const { user } = useAuth();

  useEffect(() => {
    loadStations();
  }, []);

  const loadStations = async () => {
    try {
      setLoading(true);
      const data = await apiService.getChargingStations();
      setStations(data);
    } catch (err) {
      setError('Failed to load charging stations');
      console.error('Load stations error:', err);
    } finally {
      setLoading(false);
    }
  };

  const getStatusColor = (status: StationStatus) => {
    switch (status) {
      case StationStatus.Active:
        return 'bg-green-100 text-green-800';
      case StationStatus.Inactive:
        return 'bg-red-100 text-red-800';
      case StationStatus.Maintenance:
        return 'bg-yellow-100 text-yellow-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  const getStatusText = (status: StationStatus) => {
    switch (status) {
      case StationStatus.Active:
        return 'Active';
      case StationStatus.Inactive:
        return 'Inactive';
      case StationStatus.Maintenance:
        return 'Maintenance';
      default:
        return 'Unknown';
    }
  };

  const getConnectorTypeText = (type: number) => {
    const types = ['Type1', 'Type2', 'CHAdeMO', 'CCS', 'Tesla Super'];
    return types[type] || 'Unknown';
  };

  const handleStatusChange = async (stationId: string, newStatus: StationStatus) => {
    try {
      await apiService.updateStationStatus(stationId, newStatus);
      await loadStations(); // Reload the list
    } catch (err) {
      console.error('Status update error:', err);
      setError('Failed to update station status');
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="animate-spin rounded-full h-32 w-32 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Header */}
        <div className="mb-8">
          <div className="flex justify-between items-center">
            <div>
              <h1 className="text-3xl font-bold text-gray-900">Charging Stations</h1>
              <p className="mt-2 text-gray-600">
                Manage your network of {stations.length} charging stations
              </p>
            </div>
            {(user?.role === 'Admin' || user?.role === 'Operator') && (
              <Link
                to="/stations/create"
                className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-md font-medium transition-colors flex items-center"
              >
                <svg className="h-5 w-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
                </svg>
                Add New Station
              </Link>
            )}
          </div>
        </div>

        {error && (
          <div className="mb-6 bg-red-50 border border-red-200 text-red-600 px-4 py-3 rounded-md">
            {error}
          </div>
        )}

        {/* Stations List */}
        {stations.length === 0 ? (
          <div className="bg-white rounded-lg shadow p-8 text-center">
            <div className="p-4 bg-gray-100 rounded-full inline-block mb-4">
              <svg className="h-12 w-12 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
              </svg>
            </div>
            <h3 className="text-lg font-medium text-gray-900 mb-2">No charging stations found</h3>
            <p className="text-gray-600 mb-4">Get started by creating your first charging station.</p>
            {(user?.role === 'Admin' || user?.role === 'Operator') && (
              <Link
                to="/stations/create"
                className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-md font-medium transition-colors inline-flex items-center"
              >
                <svg className="h-5 w-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
                </svg>
                Create First Station
              </Link>
            )}
          </div>
        ) : (
          <div className="bg-white shadow rounded-lg overflow-hidden">
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-gray-200">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Station
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Location
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Connectors
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Status
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Price
                    </th>
                    <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Actions
                    </th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                  {stations.map((station) => (
                    <tr key={station.id} className="hover:bg-gray-50">
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="flex items-center">
                          <div className="p-2 bg-blue-100 rounded-md mr-3">
                            <svg className="h-5 w-5 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
                            </svg>
                          </div>
                          <div>
                            <div className="text-sm font-medium text-gray-900">{station.name}</div>
                            <div className="text-sm text-gray-500">{station.description}</div>
                          </div>
                        </div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="text-sm text-gray-900">{station.address}</div>
                        <div className="text-sm text-gray-500">
                          {station.location.latitude.toFixed(4)}, {station.location.longitude.toFixed(4)}
                        </div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="text-sm text-gray-900">{station.connectors.length} connectors</div>
                        <div className="text-sm text-gray-500">
                          {station.connectors.map((connector, index) => (
                            <span key={connector.id} className="inline-block mr-2">
                              {getConnectorTypeText(connector.type)}
                              {index < station.connectors.length - 1 && ','}
                            </span>
                          ))}
                        </div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <span className={`inline-flex px-2 py-1 text-xs font-semibold rounded-full ${getStatusColor(station.status)}`}>
                          {getStatusText(station.status)}
                        </span>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                        ${station.pricePerKWh.toFixed(2)}/kWh
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                        <div className="flex justify-end space-x-2">
                          <Link
                            to={`/stations/edit/${station.id}`}
                            className="text-blue-600 hover:text-blue-900"
                          >
                            <svg className="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                            </svg>
                          </Link>
                          {(user?.role === 'Admin' || user?.role === 'Operator') && (
                            <div className="flex space-x-1">
                              {station.status !== StationStatus.Active && (
                                <button
                                  onClick={() => handleStatusChange(station.id, StationStatus.Active)}
                                  className="text-green-600 hover:text-green-900"
                                  title="Activate"
                                >
                                  <svg className="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                                  </svg>
                                </button>
                              )}
                              {station.status !== StationStatus.Maintenance && (
                                <button
                                  onClick={() => handleStatusChange(station.id, StationStatus.Maintenance)}
                                  className="text-yellow-600 hover:text-yellow-900"
                                  title="Set to Maintenance"
                                >
                                  <svg className="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L3.732 16.5c-.77.833.192 2.5 1.732 2.5z" />
                                  </svg>
                                </button>
                              )}
                              {station.status !== StationStatus.Inactive && (
                                <button
                                  onClick={() => handleStatusChange(station.id, StationStatus.Inactive)}
                                  className="text-red-600 hover:text-red-900"
                                  title="Deactivate"
                                >
                                  <svg className="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                                  </svg>
                                </button>
                              )}
                            </div>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default StationsList;