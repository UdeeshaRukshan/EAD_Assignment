import React, { useState } from 'react';
import { Booking, BookingStatus, ChargingStation } from '../types';
import { apiService } from '../services/apiService';

interface BookingListProps {
  bookings: Booking[];
  stations: ChargingStation[];
  onBookingUpdated: () => void;
  currentUserId: string;
  userRole: string;
}

const BookingList: React.FC<BookingListProps> = ({
  bookings,
  stations,
  onBookingUpdated,
  currentUserId,
  userRole
}) => {
  const [loading, setLoading] = useState<string>('');
  const [error, setError] = useState<string>('');
  const [showCompleteForm, setShowCompleteForm] = useState<string>('');
  const [energyConsumed, setEnergyConsumed] = useState<number>(0);

  const getStatusBadge = (status: BookingStatus): string => {
    const badges = {
      [BookingStatus.Pending]: 'bg-yellow-100 text-yellow-800',
      [BookingStatus.Confirmed]: 'bg-green-100 text-green-800',
      [BookingStatus.InProgress]: 'bg-blue-100 text-blue-800',
      [BookingStatus.Completed]: 'bg-gray-100 text-gray-800',
      [BookingStatus.Cancelled]: 'bg-red-100 text-red-800',
      [BookingStatus.NoShow]: 'bg-red-100 text-red-800'
    };
    return badges[status] || 'bg-gray-100 text-gray-800';
  };

  const getStatusText = (status: BookingStatus): string => {
    const statusTexts = {
      [BookingStatus.Pending]: 'Pending',
      [BookingStatus.Confirmed]: 'Confirmed',
      [BookingStatus.InProgress]: 'In Progress',
      [BookingStatus.Completed]: 'Completed',
      [BookingStatus.Cancelled]: 'Cancelled',
      [BookingStatus.NoShow]: 'No Show'
    };
    return statusTexts[status] || 'Unknown';
  };

  const canCancelBooking = (booking: Booking): boolean => {
    const startTime = new Date(booking.startTime);
    const now = new Date();
    const hoursUntilStart = (startTime.getTime() - now.getTime()) / (1000 * 60 * 60);
    
    // EV Owners can cancel their own bookings with 12-hour notice
    if (booking.userId === currentUserId && userRole === 'EVOwner') {
      return (
        (booking.status === BookingStatus.Pending || booking.status === BookingStatus.Confirmed) &&
        hoursUntilStart >= 12
      );
    }
    
    // Admins and Operators can cancel any booking (within reason)
    if (userRole === 'Admin' || userRole === 'Operator') {
      return (
        booking.status === BookingStatus.Pending || 
        booking.status === BookingStatus.Confirmed ||
        booking.status === BookingStatus.InProgress
      );
    }
    
    return false;
  };

  const canConfirmBooking = (booking: Booking): boolean => {
    return (
      booking.status === BookingStatus.Pending &&
      (userRole === 'Admin' || userRole === 'Operator')
    );
  };

  const canCompleteBooking = (booking: Booking): boolean => {
    return (
      booking.status === BookingStatus.Confirmed &&
      (userRole === 'Admin' || userRole === 'Operator')
    );
  };

  const handleConfirmBooking = async (bookingId: string) => {
    if (!confirm('Are you sure you want to confirm this booking?')) return;

    setLoading(bookingId);
    setError('');

    try {
      await apiService.confirmBooking(bookingId);
      onBookingUpdated();
    } catch (error: any) {
      setError(error.response?.data?.message || 'Failed to confirm booking');
    } finally {
      setLoading('');
    }
  };

  const handleCompleteBooking = async (bookingId: string) => {
    if (energyConsumed <= 0) {
      setError('Please enter a valid energy consumption amount');
      return;
    }

    setLoading(bookingId);
    setError('');

    try {
      await apiService.completeBooking(bookingId, { energyConsumed });
      setShowCompleteForm('');
      setEnergyConsumed(0);
      onBookingUpdated();
    } catch (error: any) {
      setError(error.response?.data?.message || 'Failed to complete booking');
    } finally {
      setLoading('');
    }
  };

  const handleCancelBooking = async (bookingId: string) => {
    if (!confirm('Are you sure you want to cancel this booking?')) return;

    setLoading(bookingId);
    setError('');

    try {
      await apiService.cancelBooking(bookingId);
      onBookingUpdated();
    } catch (error: any) {
      setError(error.response?.data?.message || 'Failed to cancel booking');
    } finally {
      setLoading('');
    }
  };

  const getStationName = (stationId: string): string => {
    const station = stations.find(s => s.id === stationId);
    return station ? station.name : 'Unknown Station';
  };

  const getStationAddress = (stationId: string): string => {
    const station = stations.find(s => s.id === stationId);
    return station ? station.address : 'Unknown Address';
  };

  const getConnectorInfo = (stationId: string, connectorId: string): string => {
    const station = stations.find(s => s.id === stationId);
    if (!station) return 'Unknown Connector';
    
    const connector = station.connectors.find(c => c.id === connectorId);
    if (!connector) return 'Unknown Connector';

    const types = ['Type1', 'Type2', 'CHAdeMO', 'CCS', 'Tesla Super'];
    return `${types[connector.type]} - ${connector.power}kW`;
  };

  const formatDateTime = (dateString: string): string => {
    return new Date(dateString).toLocaleString();
  };

  const getDurationText = (startTime: string, endTime: string): string => {
    const duration = Math.round((new Date(endTime).getTime() - new Date(startTime).getTime()) / (1000 * 60));
    const hours = Math.floor(duration / 60);
    const minutes = duration % 60;
    
    if (hours > 0) {
      return `${hours}h ${minutes}m`;
    }
    return `${minutes} minutes`;
  };

  if (bookings.length === 0) {
    return (
      <div className="text-center py-8">
        <div className="text-gray-500 text-lg">No bookings found</div>
        <p className="text-gray-400 mt-2">Your booking history will appear here</p>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {error && (
        <div className="p-3 bg-red-100 border border-red-400 text-red-700 rounded">
          {error}
        </div>
      )}

      {bookings.map((booking) => (
        <div
          key={booking.id}
          className="bg-white rounded-lg shadow-md p-6 border border-gray-200"
        >
          <div className="flex justify-between items-start mb-4">
            <div className="flex items-center space-x-3">
              <h3 className="text-lg font-semibold text-gray-900">
                {getStationName(booking.stationId)}
              </h3>
              <span className={`px-2 py-1 rounded-full text-xs font-medium ${getStatusBadge(booking.status)}`}>
                {getStatusText(booking.status)}
              </span>
            </div>
            <div className="text-sm text-gray-500">
              #{booking.id.substring(0, 8)}
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
            <div>
              <p className="text-sm text-gray-600">Station Address</p>
              <p className="font-medium">{getStationAddress(booking.stationId)}</p>
            </div>
            <div>
              <p className="text-sm text-gray-600">Connector</p>
              <p className="font-medium">{getConnectorInfo(booking.stationId, booking.connectorId)}</p>
            </div>
            <div>
              <p className="text-sm text-gray-600">Start Time</p>
              <p className="font-medium">{formatDateTime(booking.startTime)}</p>
            </div>
            <div>
              <p className="text-sm text-gray-600">End Time</p>
              <p className="font-medium">{formatDateTime(booking.endTime)}</p>
            </div>
            <div>
              <p className="text-sm text-gray-600">Duration</p>
              <p className="font-medium">{getDurationText(booking.startTime, booking.endTime)}</p>
            </div>
            <div>
              <p className="text-sm text-gray-600">Created</p>
              <p className="font-medium">{formatDateTime(booking.createdAt)}</p>
            </div>
          </div>

          {booking.notes && (
            <div className="mb-4">
              <p className="text-sm text-gray-600">Notes</p>
              <p className="text-gray-800 bg-gray-50 p-2 rounded">{booking.notes}</p>
            </div>
          )}

          {booking.status === BookingStatus.Completed && (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4 p-3 bg-green-50 rounded border border-green-200">
              <div>
                <p className="text-sm text-gray-600">Energy Consumed</p>
                <p className="font-medium text-green-700">{booking.energyConsumed} kWh</p>
              </div>
              <div>
                <p className="text-sm text-gray-600">Total Cost</p>
                <p className="font-medium text-green-700">${booking.totalCost.toFixed(2)}</p>
              </div>
            </div>
          )}

          {booking.qrCode && (booking.status === BookingStatus.Confirmed || booking.status === BookingStatus.Pending) && (
            <div className="mb-4 p-3 bg-blue-50 rounded border border-blue-200">
              <p className="text-sm text-gray-600 mb-2">QR Code</p>
              <p className="text-xs font-mono text-blue-700 break-all">{booking.qrCode}</p>
            </div>
          )}

          {/* Complete Booking Form */}
          {showCompleteForm === booking.id && (
            <div className="mb-4 p-4 bg-green-50 border border-green-200 rounded">
              <h4 className="font-medium text-green-800 mb-2">Complete Booking</h4>
              <div className="flex items-center space-x-3">
                <div className="flex-1">
                  <label className="block text-sm text-green-700 mb-1">Energy Consumed (kWh)</label>
                  <input
                    type="number"
                    min="0"
                    step="0.1"
                    value={energyConsumed}
                    onChange={(e) => setEnergyConsumed(Number(e.target.value))}
                    className="w-full px-3 py-2 border border-green-300 rounded-md focus:outline-none focus:ring-2 focus:ring-green-500"
                    placeholder="Enter energy consumed"
                  />
                </div>
                <button
                  onClick={() => handleCompleteBooking(booking.id)}
                  disabled={loading === booking.id}
                  className="px-4 py-2 bg-green-600 text-white rounded-md hover:bg-green-700 disabled:opacity-50"
                >
                  {loading === booking.id ? 'Completing...' : 'Complete'}
                </button>
                <button
                  onClick={() => {
                    setShowCompleteForm('');
                    setEnergyConsumed(0);
                  }}
                  className="px-3 py-2 border border-gray-300 text-gray-700 rounded-md hover:bg-gray-50"
                >
                  Cancel
                </button>
              </div>
            </div>
          )}

          {/* Action Buttons */}
          <div className="flex justify-end space-x-2 border-t pt-4">
            {/* Cancel Action - Available for EV Owners (own bookings) and Admin/Operators (any booking) */}
            {canCancelBooking(booking) && (
              <button
                onClick={() => handleCancelBooking(booking.id)}
                disabled={loading === booking.id}
                className="px-4 py-2 border border-red-300 text-red-600 rounded-md hover:bg-red-50 disabled:opacity-50 transition-colors"
              >
                {loading === booking.id ? 'Cancelling...' : 'Cancel Booking'}
              </button>
            )}

            {/* Operator/Admin Actions */}
            {canConfirmBooking(booking) && (
              <button
                onClick={() => handleConfirmBooking(booking.id)}
                disabled={loading === booking.id}
                className="px-4 py-2 bg-green-600 text-white rounded-md hover:bg-green-700 disabled:opacity-50 transition-colors"
              >
                {loading === booking.id ? 'Confirming...' : 'Confirm Booking'}
              </button>
            )}

            {canCompleteBooking(booking) && (
              <button
                onClick={() => setShowCompleteForm(booking.id)}
                className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 transition-colors"
              >
                Complete Booking
              </button>
            )}
          </div>

          {/* Information Messages */}
          {!canCancelBooking(booking) && userRole === 'EVOwner' && booking.userId === currentUserId &&
           (booking.status === BookingStatus.Pending || booking.status === BookingStatus.Confirmed) && (
            <div className="text-sm text-gray-500 border-t pt-4">
              <p className="flex items-center">
                <svg className="h-4 w-4 mr-1 text-yellow-500" fill="currentColor" viewBox="0 0 20 20">
                  <path fillRule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
                </svg>
                Bookings can only be cancelled at least 12 hours before the reservation time.
              </p>
            </div>
          )}

          {/* Additional booking information for Admins/Operators */}
          {userRole !== 'EVOwner' && (
            <div className="text-sm text-gray-600 border-t pt-4 space-y-1">
              <p><span className="font-medium">Customer ID:</span> {booking.userId}</p>
              {booking.confirmedBy && (
                <p><span className="font-medium">Confirmed by:</span> {booking.confirmedBy}</p>
              )}
              {booking.confirmedAt && (
                <p><span className="font-medium">Confirmed at:</span> {formatDateTime(booking.confirmedAt)}</p>
              )}
            </div>
          )}
        </div>
      ))}
    </div>
  );
};

export default BookingList;