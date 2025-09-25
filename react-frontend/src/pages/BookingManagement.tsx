import React, { useState, useEffect } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { Booking, ChargingStation, StationStatus } from '../types';
import { apiService } from '../services/apiService';
import BookingForm from '../components/BookingForm';
import BookingList from '../components/BookingList';

const BookingManagement: React.FC = () => {
  const { user } = useAuth();
  const [bookings, setBookings] = useState<Booking[]>([]);
  const [stations, setStations] = useState<ChargingStation[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string>('');
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [activeTab, setActiveTab] = useState<'all' | 'upcoming' | 'past' | 'pending'>('upcoming');

  useEffect(() => {
    loadData();
  }, [user]);

  const loadData = async () => {
    if (!user) return;

    setLoading(true);
    setError('');

    try {
      let userBookings: Booking[] = [];
      let allStations: ChargingStation[] = [];

      // Load stations first
      allStations = await apiService.getChargingStations();

      // Load bookings based on user role
      switch (user.role) {
        case 'Admin':
        case 'Operator':
        case 'BackofficeUser':
          // Admins, operators, and backoffice users can see all bookings
          userBookings = await apiService.getAllBookings();
          break;
        case 'EVOwner':
        default:
          // EV Owners can only see their own bookings
          userBookings = await apiService.getUserBookings(user.id);
          break;
      }

      setBookings(userBookings);
      setStations(allStations);
    } catch (error: any) {
      setError(error.response?.data?.message || 'Failed to load data');
    } finally {
      setLoading(false);
    }
  };

  const handleBookingCreated = (newBooking: Booking) => {
    setBookings(prev => [newBooking, ...prev]);
    setShowCreateForm(false);
  };

  const handleBookingUpdated = () => {
    // Reload bookings after update/cancel
    loadData();
  };

  // Filter stations to only show active ones for booking
  const getActiveStations = (): ChargingStation[] => {
    return stations.filter(station => station.status === StationStatus.Active);
  };

  const filterBookings = (bookings: Booking[]): Booking[] => {
    const now = new Date();
    
    switch (activeTab) {
      case 'upcoming':
        return bookings.filter(booking => {
          const startTime = new Date(booking.startTime);
          return startTime > now && (booking.status === 0 || booking.status === 1); // Pending or Confirmed
        });
      case 'past':
        return bookings.filter(booking => {
          const startTime = new Date(booking.startTime);
          return startTime <= now || booking.status === 3 || booking.status === 4 || booking.status === 5; // Completed, Cancelled, NoShow
        });
      case 'pending':
        return bookings.filter(booking => booking.status === 0); // Pending only
      default:
        return bookings;
    }
  };

  const getTabCount = (tab: 'all' | 'upcoming' | 'past' | 'pending'): number => {
    const now = new Date();
    
    switch (tab) {
      case 'upcoming':
        return bookings.filter(booking => {
          const startTime = new Date(booking.startTime);
          return startTime > now && (booking.status === 0 || booking.status === 1); // Pending or Confirmed
        }).length;
      case 'past':
        return bookings.filter(booking => {
          const startTime = new Date(booking.startTime);
          return startTime <= now || booking.status === 3 || booking.status === 4 || booking.status === 5; // Completed, Cancelled, NoShow
        }).length;
      case 'pending':
        return bookings.filter(booking => booking.status === 0).length; // Pending only
      default:
        return bookings.length;
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center min-h-screen">
        <div className="animate-spin rounded-full h-32 w-32 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  if (!user) {
    return (
      <div className="flex justify-center items-center min-h-screen">
        <div className="text-red-600">Please log in to manage bookings</div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 py-8">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* Header */}
        <div className="bg-white rounded-lg shadow-sm p-6 mb-8">
          <div className="flex justify-between items-center">
            <div>
              <h1 className="text-3xl font-bold text-gray-900">
                {user?.role === 'EVOwner' ? 'My Bookings' : 'Booking Management'}
              </h1>
              <p className="text-gray-600 mt-2">
                {user?.role === 'EVOwner' 
                  ? 'Manage your EV charging reservations - create, modify, or cancel bookings'
                  : user?.role === 'Operator'
                  ? 'Monitor and manage bookings for your charging stations'
                  : 'View and manage all charging station bookings in the system'
                }
              </p>
            </div>
            {user?.role === 'EVOwner' && (
              <button
                onClick={() => setShowCreateForm(true)}
                className="bg-blue-600 hover:bg-blue-700 text-white px-6 py-3 rounded-lg font-medium transition-colors"
              >
                + New Booking
              </button>
            )}
          </div>

          {/* Business Rules Info */}
          <div className="mt-6 p-4 bg-blue-50 border border-blue-200 rounded-lg">
            <h3 className="text-sm font-medium text-blue-900 mb-2">Booking Rules:</h3>
            <ul className="text-sm text-blue-800 space-y-1">
              <li>• Reservations can be made up to 7 days in advance</li>
              <li>• Bookings must be made at least 1 hour in advance</li>
              <li>• Modifications and cancellations require at least 12 hours notice</li>
              <li>• Minimum booking duration: 30 minutes</li>
              <li>• Maximum booking duration: 8 hours</li>
            </ul>
          </div>
        </div>

        {error && (
          <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-6">
            {error}
          </div>
        )}

        {/* Create Booking Form Modal */}
        {showCreateForm && user?.role === 'EVOwner' && (
          <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
            <div className="max-w-4xl w-full max-h-screen overflow-y-auto">
              <BookingForm
                stations={getActiveStations()}
                userId={user.id}
                onBookingCreated={handleBookingCreated}
                onCancel={() => setShowCreateForm(false)}
              />
            </div>
          </div>
        )}

        {/* Tabs */}
        <div className="bg-white rounded-lg shadow-sm mb-6">
          <div className="border-b border-gray-200">
            <nav className="flex space-x-8 px-6">
              {[
                { key: 'upcoming', label: 'Upcoming', count: getTabCount('upcoming') },
                ...(user?.role !== 'EVOwner' ? [{ key: 'pending', label: 'Pending', count: getTabCount('pending') }] : []),
                { key: 'past', label: 'Past', count: getTabCount('past') },
                { key: 'all', label: 'All', count: bookings.length }
              ].map(tab => (
                <button
                  key={tab.key}
                  onClick={() => setActiveTab(tab.key as 'all' | 'upcoming' | 'past' | 'pending')}
                  className={`py-4 px-1 border-b-2 font-medium text-sm whitespace-nowrap ${
                    activeTab === tab.key
                      ? 'border-blue-500 text-blue-600'
                      : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                  }`}
                >
                  {tab.label}
                  {tab.count > 0 && (
                    <span className={`ml-2 py-0.5 px-2 rounded-full text-xs ${
                      activeTab === tab.key
                        ? 'bg-blue-100 text-blue-600'
                        : 'bg-gray-100 text-gray-600'
                    }`}>
                      {tab.count}
                    </span>
                  )}
                </button>
              ))}
            </nav>
          </div>

          <div className="p-6">
            {/* Stats Cards */}
            <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
              <div className="bg-blue-50 p-4 rounded-lg">
                <div className="text-2xl font-bold text-blue-600">{bookings.length}</div>
                <div className="text-sm text-blue-700">Total Bookings</div>
              </div>
              <div className="bg-green-50 p-4 rounded-lg">
                <div className="text-2xl font-bold text-green-600">
                  {bookings.filter(b => b.status === 3).length}
                </div>
                <div className="text-sm text-green-700">Completed</div>
              </div>
              <div className="bg-yellow-50 p-4 rounded-lg">
                <div className="text-2xl font-bold text-yellow-600">
                  {bookings.filter(b => b.status === 0 || b.status === 1).length}
                </div>
                <div className="text-sm text-yellow-700">Active</div>
              </div>
              <div className="bg-red-50 p-4 rounded-lg">
                <div className="text-2xl font-bold text-red-600">
                  {bookings.filter(b => b.status === 4).length}
                </div>
                <div className="text-sm text-red-700">Cancelled</div>
              </div>
            </div>

            {/* Bookings List */}
            <BookingList
              bookings={filterBookings(bookings)}
              stations={stations}
              onBookingUpdated={handleBookingUpdated}
              currentUserId={user.id}
              userRole={user.role}
            />
          </div>
        </div>
      </div>
    </div>
  );
};

export default BookingManagement;