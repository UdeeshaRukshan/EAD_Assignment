import React, { useState, useEffect } from 'react';
import { ChargingStation, Connector, CreateBookingRequest, ConnectorStatus } from '../types';
import { apiService } from '../services/apiService';

interface BookingFormProps {
  stations: ChargingStation[];
  userId: string;
  onBookingCreated: (booking: any) => void;
  onCancel: () => void;
}

const BookingForm: React.FC<BookingFormProps> = ({
  stations,
  userId,
  onBookingCreated,
  onCancel
}) => {
  const [selectedStationId, setSelectedStationId] = useState<string>('');
  const [selectedConnectorId, setSelectedConnectorId] = useState<string>('');
  const [startTime, setStartTime] = useState<string>('');
  const [endTime, setEndTime] = useState<string>('');
  const [notes, setNotes] = useState<string>('');
  const [availableConnectors, setAvailableConnectors] = useState<Connector[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string>('');

  // Get the current date and time for validation
  const now = new Date();
  const maxDate = new Date(now.getTime() + 7 * 24 * 60 * 60 * 1000); // 7 days from now
  const defaultStartTime = new Date(now.getTime() + 60 * 60 * 1000); // 1 hour from now

  // Format datetime-local input values
  const formatDateTimeLocal = (date: Date): string => {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    return `${year}-${month}-${day}T${hours}:${minutes}`;
  };

  const minDateTime = formatDateTimeLocal(defaultStartTime); // Use 1 hour ahead as minimum
  const maxDateTime = formatDateTimeLocal(maxDate);
  const defaultStartDateTime = formatDateTimeLocal(defaultStartTime); // Default value for start time

  // Set default start time on component mount
  useEffect(() => {
    if (!startTime) {
      setStartTime(defaultStartDateTime);
      // Also set default end time to 30 minutes after start time
      const defaultEndTime = new Date(defaultStartTime.getTime() + 30 * 60 * 1000);
      setEndTime(formatDateTimeLocal(defaultEndTime));
    }
  }, []); // Empty dependency array to run only once on mount

  useEffect(() => {
    if (selectedStationId) {
      const station = stations.find(s => s.id === selectedStationId);
      if (station) {
        // Only show connectors that are available for booking
        const available = station.connectors.filter(
          c => c.isAvailable && c.status === ConnectorStatus.Available
        );
        setAvailableConnectors(available);
        setSelectedConnectorId('');
      }
    }
  }, [selectedStationId, stations]);

  // Handle start time change with auto-adjustment
  const handleStartTimeChange = (value: string) => {
    if (!value) {
      setStartTime('');
      return;
    }

    const selectedStart = new Date(value);
    const nowPlusHour = new Date(now.getTime() + 60 * 60 * 1000);

    // If selected time is less than 1 hour from now, adjust it
    if (selectedStart < nowPlusHour) {
      const adjustedTime = formatDateTimeLocal(nowPlusHour);
      setStartTime(adjustedTime);
      // Auto-set end time to 30 minutes after adjusted start time
      const defaultEnd = new Date(nowPlusHour.getTime() + 30 * 60 * 1000);
      setEndTime(formatDateTimeLocal(defaultEnd));
    } else {
      setStartTime(value);
      // Auto-set end time to 30 minutes after selected start time if end time is empty
      if (!endTime) {
        const defaultEnd = new Date(selectedStart.getTime() + 30 * 60 * 1000);
        setEndTime(formatDateTimeLocal(defaultEnd));
      }
    }

    // Auto-adjust end time if it becomes invalid
    if (endTime) {
      const currentEnd = new Date(endTime);
      const newStart = selectedStart < nowPlusHour ? nowPlusHour : selectedStart;
      
      if (currentEnd <= newStart) {
        // Set end time to 30 minutes after the new start time
        const newEnd = new Date(newStart.getTime() + 30 * 60 * 1000);
        setEndTime(formatDateTimeLocal(newEnd));
      }
    }
  };

  // Handle end time change with auto-adjustment
  const handleEndTimeChange = (value: string) => {
    if (!value) {
      setEndTime('');
      return;
    }

    const selectedEnd = new Date(value);
    
    if (startTime) {
      const currentStart = new Date(startTime);
      
      // If end time is not after start time, adjust it to 30 minutes after start
      if (selectedEnd <= currentStart) {
        const adjustedEnd = new Date(currentStart.getTime() + 30 * 60 * 1000);
        setEndTime(formatDateTimeLocal(adjustedEnd));
      } else {
        setEndTime(value);
      }
    } else {
      setEndTime(value);
    }
  };

  const validateForm = (): string | null => {
    if (!selectedStationId) return 'Please select a charging station';
    if (!selectedConnectorId) return 'Please select a connector';
    if (!startTime) return 'Please select a start time';
    if (!endTime) return 'Please select an end time';

    const start = new Date(startTime);
    const end = new Date(endTime);
    const nowPlusHour = new Date(now.getTime() + 60 * 60 * 1000);

    // Validate booking is at least 1 hour from now
    if (start < nowPlusHour) {
      return 'Booking must be at least 1 hour from now';
    }

    // Validate booking is within 7 days
    if (start > maxDate) {
      return 'Booking cannot be more than 7 days in advance';
    }

    // Validate end time is after start time
    if (end <= start) {
      return 'End time must be after start time';
    }

    // Validate minimum booking duration (30 minutes)
    const durationMinutes = (end.getTime() - start.getTime()) / (1000 * 60);
    if (durationMinutes < 30) {
      return 'Minimum booking duration is 30 minutes';
    }

    // Validate maximum booking duration (8 hours)
    if (durationMinutes > 480) {
      return 'Maximum booking duration is 8 hours';
    }

    return null;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    const validationError = validateForm();
    if (validationError) {
      setError(validationError);
      return;
    }

    setLoading(true);
    setError('');

    try {
      const bookingData: CreateBookingRequest = {
        userId,
        stationId: selectedStationId,
        connectorId: selectedConnectorId,
        startTime: new Date(startTime).toISOString(),
        endTime: new Date(endTime).toISOString(),
        notes
      };

      const newBooking = await apiService.createBooking(bookingData);
      onBookingCreated(newBooking);
    } catch (error: any) {
      setError(error.response?.data?.message || 'Failed to create booking');
    } finally {
      setLoading(false);
    }
  };

  const getConnectorTypeName = (type: number): string => {
    const types = ['Type1', 'Type2', 'CHAdeMO', 'CCS', 'Tesla Super'];
    return types[type] || 'Unknown';
  };

  return (
    <div className="max-w-2xl mx-auto bg-white rounded-lg shadow-md p-6">
      <div className="flex justify-between items-center mb-6">
        <h2 className="text-2xl font-bold text-gray-900">Create New Booking</h2>
        <button
          onClick={onCancel}
          className="text-gray-500 hover:text-gray-700"
        >
          ✕
        </button>
      </div>

      {error && (
        <div className="mb-4 p-3 bg-red-100 border border-red-400 text-red-700 rounded">
          {error}
        </div>
      )}

      {stations.length === 0 && (
        <div className="mb-4 p-4 bg-yellow-100 border border-yellow-400 text-yellow-700 rounded">
          <p className="font-medium">No Active Stations Available</p>
          <p className="text-sm mt-1">
            Currently, there are no active charging stations available for booking. 
            Stations under maintenance or out of order cannot be booked. Please try again later.
          </p>
        </div>
      )}

      {stations.length > 0 && (
        <form onSubmit={handleSubmit} className="space-y-6">
          <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Charging Station *
          </label>
          <select
            value={selectedStationId}
            onChange={(e) => setSelectedStationId(e.target.value)}
            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            required
          >
            <option value="">Select a charging station</option>
            {stations.map((station) => (
              <option key={station.id} value={station.id}>
                {station.name} - {station.address}
              </option>
            ))}
          </select>
        </div>

        {availableConnectors.length > 0 && (
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Connector *
            </label>
            <select
              value={selectedConnectorId}
              onChange={(e) => setSelectedConnectorId(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              required
            >
              <option value="">Select a connector</option>
              {availableConnectors.map((connector) => (
                <option key={connector.id} value={connector.id}>
                  {getConnectorTypeName(connector.type)} - {connector.power}kW
                </option>
              ))}
            </select>
          </div>
        )}

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Start Time *
            </label>
            <input
              type="datetime-local"
              value={startTime}
              onChange={(e) => handleStartTimeChange(e.target.value)}
              min={minDateTime}
              max={maxDateTime}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              required
            />
            <p className="text-xs text-gray-500 mt-1">
              Must be at least 1 hour from now and within 7 days
            </p>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              End Time *
            </label>
            <input
              type="datetime-local"
              value={endTime}
              onChange={(e) => handleEndTimeChange(e.target.value)}
              min={startTime ? new Date(new Date(startTime).getTime() + 30 * 60 * 1000).toISOString().slice(0, 16) : minDateTime}
              max={maxDateTime}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              required
            />
            <p className="text-xs text-gray-500 mt-1">
              Minimum 30 minutes, maximum 8 hours
            </p>
          </div>
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Notes (Optional)
          </label>
          <textarea
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
            rows={3}
            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            placeholder="Any additional notes for your booking..."
          />
        </div>

        <div className="flex justify-end space-x-4">
          <button
            type="button"
            onClick={onCancel}
            className="px-4 py-2 border border-gray-300 rounded-md text-gray-700 hover:bg-gray-50"
            disabled={loading}
          >
            Cancel
          </button>
          <button
            type="submit"
            disabled={loading || !selectedStationId || !selectedConnectorId}
            className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {loading ? 'Creating...' : 'Create Booking'}
          </button>
        </div>
        </form>
      )}
    </div>
  );
};

export default BookingForm;