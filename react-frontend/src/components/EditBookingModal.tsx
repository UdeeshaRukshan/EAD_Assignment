import React, { useState, useEffect } from 'react';
import { Booking, ChargingStation, UpdateBookingRequest } from '../types';
import { apiService } from '../services/apiService';

interface EditBookingModalProps {
  booking: Booking;
  stations: ChargingStation[];
  isOpen: boolean;
  onClose: () => void;
  onBookingUpdated: () => void;
}

const EditBookingModal: React.FC<EditBookingModalProps> = ({
  booking,
  stations,
  isOpen,
  onClose,
  onBookingUpdated
}) => {
  const [formData, setFormData] = useState<UpdateBookingRequest>({
    stationId: booking.stationId,
    connectorId: booking.connectorId,
    startTime: booking.startTime,
    endTime: booking.endTime,
    notes: booking.notes || ''
  });
  
  const [selectedStation, setSelectedStation] = useState<ChargingStation | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string>('');

  useEffect(() => {
    if (isOpen) {
      // Reset form data when modal opens with properly formatted datetime values
      setFormData({
        stationId: booking.stationId,
        connectorId: booking.connectorId,
        startTime: formatDateTimeLocal(booking.startTime),
        endTime: formatDateTimeLocal(booking.endTime),
        notes: booking.notes || ''
      });
      setError('');
      
      // Find the selected station
      const station = stations.find(s => s.id === booking.stationId);
      setSelectedStation(station || null);
    }
  }, [isOpen, booking, stations]);

  const handleStationChange = (stationId: string) => {
    const station = stations.find(s => s.id === stationId);
    setSelectedStation(station || null);
    setFormData(prev => ({
      ...prev,
      stationId,
      connectorId: station?.connectors[0]?.id || ''
    }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    try {
      // Validate dates
      const startTime = new Date(formData.startTime);
      const endTime = new Date(formData.endTime);
      const now = new Date();

      if (startTime <= now) {
        throw new Error('Start time must be in the future');
      }

      if (endTime <= startTime) {
        throw new Error('End time must be after start time');
      }

      // Check 12-hour notice
      const hoursUntilStart = (startTime.getTime() - now.getTime()) / (1000 * 60 * 60);
      if (hoursUntilStart < 12) {
        throw new Error('Bookings can only be modified at least 12 hours before the reservation time');
      }

      // Format the data for API submission
      const apiData = {
        ...formData,
        startTime: formatDateTimeForAPI(formData.startTime),
        endTime: formatDateTimeForAPI(formData.endTime)
      };

      await apiService.updateBooking(booking.id, apiData);
      onBookingUpdated();
      onClose();
    } catch (err: any) {
      setError(err.response?.data?.message || err.message || 'Failed to update booking');
    } finally {
      setLoading(false);
    }
  };

  const formatDateTimeLocal = (dateString: string): string => {
    const date = new Date(dateString);
    // Create a date object that represents the local time without timezone conversion
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    
    return `${year}-${month}-${day}T${hours}:${minutes}`;
  };

  const formatDateTimeForAPI = (localDateTimeString: string): string => {
    // Convert from datetime-local format to ISO string
    const date = new Date(localDateTimeString);
    return date.toISOString();
  };

  const getConnectorDisplayName = (connector: any): string => {
    const types = ['Type1', 'Type2', 'CHAdeMO', 'CCS', 'Tesla Super'];
    return `${types[connector.type]} - ${connector.power}kW`;
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
      <div className="bg-white rounded-lg max-w-2xl w-full max-h-[90vh] overflow-y-auto">
        <div className="p-6">
          <div className="flex justify-between items-center mb-6">
            <h2 className="text-2xl font-bold text-gray-900">Edit Booking</h2>
            <button
              onClick={onClose}
              className="text-gray-400 hover:text-gray-600 text-2xl"
            >
              ×
            </button>
          </div>

          {error && (
            <div className="mb-4 p-3 bg-red-100 border border-red-400 text-red-700 rounded">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-6">
            {/* Station Selection */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Charging Station
              </label>
              <select
                value={formData.stationId}
                onChange={(e) => handleStationChange(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                required
              >
                <option value="">Select a station</option>
                {stations.map((station) => (
                  <option key={station.id} value={station.id}>
                    {station.name} - {station.address}
                  </option>
                ))}
              </select>
            </div>

            {/* Connector Selection */}
            {selectedStation && (
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Connector
                </label>
                <select
                  value={formData.connectorId}
                  onChange={(e) => setFormData(prev => ({ ...prev, connectorId: e.target.value }))}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                  required
                >
                  <option value="">Select a connector</option>
                  {selectedStation.connectors
                    .filter(c => c.isAvailable)
                    .map((connector) => (
                      <option key={connector.id} value={connector.id}>
                        {getConnectorDisplayName(connector)}
                      </option>
                    ))}
                </select>
              </div>
            )}

            {/* Start Time */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Start Time
              </label>
              <input
                type="datetime-local"
                value={formData.startTime}
                onChange={(e) => setFormData(prev => ({ ...prev, startTime: e.target.value }))}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                required
              />
            </div>

            {/* End Time */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                End Time
              </label>
              <input
                type="datetime-local"
                value={formData.endTime}
                onChange={(e) => setFormData(prev => ({ ...prev, endTime: e.target.value }))}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                required
              />
            </div>

            {/* Notes */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Notes (Optional)
              </label>
              <textarea
                value={formData.notes}
                onChange={(e) => setFormData(prev => ({ ...prev, notes: e.target.value }))}
                rows={3}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder="Any additional notes for your booking..."
              />
            </div>

            {/* 12-hour notice warning */}
            <div className="bg-yellow-50 border border-yellow-200 rounded-md p-3">
              <div className="flex">
                <svg className="w-5 h-5 text-yellow-400 mr-2 mt-0.5" fill="currentColor" viewBox="0 0 20 20">
                  <path fillRule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
                </svg>
                <div className="text-sm text-yellow-800">
                  <p className="font-medium">Modification Policy</p>
                  <p>Bookings can only be modified at least 12 hours before the reservation time.</p>
                </div>
              </div>
            </div>

            {/* Action Buttons */}
            <div className="flex justify-end space-x-3 pt-6 border-t">
              <button
                type="button"
                onClick={onClose}
                className="px-4 py-2 text-gray-700 bg-gray-200 rounded-md hover:bg-gray-300 focus:outline-none focus:ring-2 focus:ring-gray-500"
                disabled={loading}
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={loading}
                className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-50"
              >
                {loading ? 'Updating...' : 'Update Booking'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};

export default EditBookingModal;