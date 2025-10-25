import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { ChargingStation, Booking } from '../types';
import { apiService } from '../services/apiService';
import { useAuth } from '../contexts/AuthContext';

const Dashboard: React.FC = () => {
  const [stations, setStations] = useState<ChargingStation[]>([]);
  const [bookings, setBookings] = useState<Booking[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const { user } = useAuth();

  useEffect(() => {
    loadDashboardData();
  }, [user]);

  const loadDashboardData = async () => {
    try {
      setLoading(true);
      const stationsData = await apiService.getChargingStations();
      setStations(stationsData);


      if (user) {
        let userBookings: Booking[] = [];
        
        switch (user.role) {
          case 'Admin':
          case 'Operator':
          case 'BackofficeUser':

            userBookings = await apiService.getAllBookings();
            break;
          case 'EVOwner':
          default:
            userBookings = await apiService.getUserBookings(user.id);
            break;
        }
        
        setBookings(userBookings);
      }
    } catch (err) {
      setError('Failed to load dashboard data');
      console.error('Dashboard error:', err);
    } finally {
      setLoading(false);
    }
  };

  const stats = {
    totalStations: stations.length,
    activeStations: stations.filter(s => s.status === 0).length,
    maintenanceStations: stations.filter(s => s.status === 2).length,
    totalConnectors: stations.reduce((acc, station) => acc + station.connectors.length, 0),
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
      {/* Header Section */}
      <div className="bg-white shadow-sm border-b">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="py-6">
            <h1 className="text-3xl font-bold text-gray-900">EV Charging Station Dashboard</h1>
            <p className="mt-1 text-sm text-gray-600">
              Welcome back, {user?.firstName}! Here's an overview of your charging network.
            </p>
          </div>
        </div>
      </div>

      {/* Dashboard Content */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {error && (
          <div className="mb-6 bg-red-50 border border-red-200 text-red-600 px-4 py-3 rounded-md">
            {error}
          </div>
        )}

        {/* Stats Cards */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
          <div className="bg-white rounded-lg shadow p-6">
            <div className="flex items-center">
              <div className="p-2 bg-blue-100 rounded-md">
                <svg className="h-6 w-6 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M13 10V3L4 14h7v7l9-11h-7z"></path>
                </svg>
              </div>
              <div className="ml-4">
                <p className="text-sm font-medium text-gray-600">Total Stations</p>
                <p className="text-2xl font-bold text-gray-900">{stats.totalStations}</p>
              </div>
            </div>
          </div>

          <div className="bg-white rounded-lg shadow p-6">
            <div className="flex items-center">
              <div className="p-2 bg-green-100 rounded-md">
                <svg className="h-6 w-6 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                </svg>
              </div>
              <div className="ml-4">
                <p className="text-sm font-medium text-gray-600">Active Stations</p>
                <p className="text-2xl font-bold text-gray-900">{stats.activeStations}</p>
              </div>
            </div>
          </div>

          <div className="bg-white rounded-lg shadow p-6">
            <div className="flex items-center">
              <div className="p-2 bg-yellow-100 rounded-md">
                <svg className="h-6 w-6 text-yellow-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L3.732 16.5c-.77.833.192 2.5 1.732 2.5z"></path>
                </svg>
              </div>
              <div className="ml-4">
                <p className="text-sm font-medium text-gray-600">Maintenance</p>
                <p className="text-2xl font-bold text-gray-900">{stats.maintenanceStations}</p>
              </div>
            </div>
          </div>

          <div className="bg-white rounded-lg shadow p-6">
            <div className="flex items-center">
              <div className="p-2 bg-purple-100 rounded-md">
                <svg className="h-6 w-6 text-purple-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M8 9l3 3-3 3m5 0h3M5 20h14a2 2 0 002-2V6a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"></path>
                </svg>
              </div>
              <div className="ml-4">
                <p className="text-sm font-medium text-gray-600">Total Connectors</p>
                <p className="text-2xl font-bold text-gray-900">{stats.totalConnectors}</p>
              </div>
            </div>
          </div>
        </div>

        {/* Action Cards */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {/* Manage Stations Card */}
          <div className="bg-white rounded-lg shadow-md hover:shadow-lg transition-shadow duration-300">
            <div className="p-6">
              <div className="flex items-center mb-4">
                <div className="p-3 bg-blue-100 rounded-lg">
                  <svg className="h-8 w-8 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M13 10V3L4 14h7v7l9-11h-7z"></path>
                  </svg>
                </div>
                <div className="ml-4">
                  <h3 className="text-lg font-semibold text-gray-900">Manage Stations</h3>
                  <p className="text-sm text-gray-600">View and manage charging stations</p>
                </div>
              </div>
              <div className="space-y-3">
                <Link
                  to="/stations"
                  className="block w-full bg-blue-600 hover:bg-blue-700 text-white text-center py-2 px-4 rounded-md transition-colors duration-200"
                >
                  View All Stations
                </Link>
                {(user?.role === 'Admin' || user?.role === 'Operator') && (
                  <Link
                    to="/stations/create"
                    className="block w-full bg-green-600 hover:bg-green-700 text-white text-center py-2 px-4 rounded-md transition-colors duration-200"
                  >
                    Add New Station
                  </Link>
                )}
              </div>
            </div>
          </div>

          {/* Quick Stats Card */}
          <div className="bg-white rounded-lg shadow-md hover:shadow-lg transition-shadow duration-300">
            <div className="p-6">
              <div className="flex items-center mb-4">
                <div className="p-3 bg-green-100 rounded-lg">
                  <svg className="h-8 w-8 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v4a2 2 0 01-2 2h-2a2 2 0 01-2-2z"></path>
                  </svg>
                </div>
                <div className="ml-4">
                  <h3 className="text-lg font-semibold text-gray-900">Network Overview</h3>
                  <p className="text-sm text-gray-600">Your charging network at a glance</p>
                </div>
              </div>
              <div className="space-y-2 text-sm">
                <div className="flex justify-between">
                  <span className="text-gray-600">Utilization Rate:</span>
                  <span className="font-medium text-green-600">85%</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-600">Average Session:</span>
                  <span className="font-medium">45 min</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-600">Revenue Today:</span>
                  <span className="font-medium text-green-600">$1,245</span>
                </div>
              </div>
            </div>
          </div>

          {/* System Status Card */}
          <div className="bg-white rounded-lg shadow-md hover:shadow-lg transition-shadow duration-300">
            <div className="p-6">
              <div className="flex items-center mb-4">
                <div className="p-3 bg-purple-100 rounded-lg">
                  <svg className="h-8 w-8 text-purple-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                  </svg>
                </div>
                <div className="ml-4">
                  <h3 className="text-lg font-semibold text-gray-900">System Status</h3>
                  <p className="text-sm text-gray-600">Overall system health</p>
                </div>
              </div>
              <div className="space-y-3">
                <div className="flex items-center">
                  <div className="w-3 h-3 bg-green-400 rounded-full mr-2"></div>
                  <span className="text-sm text-gray-600">API Services: Operational</span>
                </div>
                <div className="flex items-center">
                  <div className="w-3 h-3 bg-green-400 rounded-full mr-2"></div>
                  <span className="text-sm text-gray-600">Database: Connected</span>
                </div>
                <div className="flex items-center">
                  <div className="w-3 h-3 bg-green-400 rounded-full mr-2"></div>
                  <span className="text-sm text-gray-600">Network: Stable</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Recent Stations */}
        {stations.length > 0 && (
          <div className="mt-8 bg-white rounded-lg shadow">
            <div className="px-6 py-4 border-b border-gray-200">
              <h3 className="text-lg font-semibold text-gray-900">Recent Stations</h3>
            </div>
            <div className="p-6">
              <div className="space-y-4">
                {stations.slice(0, 3).map((station) => (
                  <div key={station.id} className="flex items-center justify-between p-4 border border-gray-200 rounded-lg">
                    <div className="flex items-center space-x-3">
                      <div className={`w-3 h-3 rounded-full ${
                        station.status === 0 ? 'bg-green-400' : 
                        station.status === 1 ? 'bg-red-400' : 'bg-yellow-400'
                      }`}></div>
                      <div>
                        <p className="font-medium text-gray-900">{station.name}</p>
                        <p className="text-sm text-gray-500">{station.address}</p>
                      </div>
                    </div>
                    <div className="text-right">
                      <p className="text-sm font-medium text-gray-900">
                        {station.connectors.length} connectors
                      </p>
                      <p className="text-xs text-gray-500">
                        {station.status === 0 ? 'Active' : station.status === 1 ? 'Inactive' : 'Maintenance'}
                      </p>
                    </div>
                  </div>
                ))}
              </div>
              {stations.length > 3 && (
                <div className="mt-4 text-center">
                  <Link
                    to="/stations"
                    className="text-blue-600 hover:text-blue-800 text-sm font-medium"
                  >
                    View all stations →
                  </Link>
                </div>
              )}
            </div>
          </div>
        )}

        {/* Upcoming Bookings */}
        {bookings.length > 0 && (
          <div className="mt-8 bg-white rounded-lg shadow">
            <div className="px-6 py-4 border-b border-gray-200">
              <div className="flex items-center justify-between">
                <h3 className="text-lg font-semibold text-gray-900">Upcoming Bookings</h3>
                <span className="text-sm text-gray-500">
                  {user?.role === 'EVOwner' ? 'Your reservations' : 'Recent reservations'}
                </span>
              </div>
            </div>
            <div className="p-6">
              <div className="space-y-4">
                {(() => {
                  const now = new Date();
                  const upcomingBookings = bookings
                    .filter(booking => {
                      const startTime = new Date(booking.startTime);
                      return startTime > now && (booking.status === 0 || booking.status === 1); // Pending or Confirmed
                    })
                    .sort((a, b) => new Date(a.startTime).getTime() - new Date(b.startTime).getTime())
                    .slice(0, 3);

                  if (upcomingBookings.length === 0) {
                    return (
                      <div className="text-center py-8">
                        <div className="text-gray-400 mb-2">
                          <svg className="mx-auto h-12 w-12" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"></path>
                          </svg>
                        </div>
                        <p className="text-gray-500 text-sm">
                          {user?.role === 'EVOwner' ? 'No upcoming bookings' : 'No upcoming bookings in the system'}
                        </p>
                        {user?.role === 'EVOwner' && (
                          <Link 
                            to="/bookings" 
                            className="inline-flex items-center mt-2 text-blue-600 hover:text-blue-800 text-sm font-medium"
                          >
                            Create your first booking →
                          </Link>
                        )}
                      </div>
                    );
                  }

                  return upcomingBookings.map((booking) => {
                    const station = stations.find(s => s.id === booking.stationId);
                    const startTime = new Date(booking.startTime);
                    const endTime = new Date(booking.endTime);
                    const timeUntilStart = Math.ceil((startTime.getTime() - now.getTime()) / (1000 * 60 * 60)); // hours

                    return (
                      <div key={booking.id} className="flex items-center justify-between p-4 border border-gray-200 rounded-lg hover:bg-gray-50 transition-colors">
                        <div className="flex items-center space-x-3">
                          <div className={`w-3 h-3 rounded-full ${
                            booking.status === 0 ? 'bg-yellow-400' : 'bg-green-400'
                          }`}></div>
                          <div>
                            <p className="font-medium text-gray-900">
                              {station?.name || 'Unknown Station'}
                            </p>
                            <p className="text-sm text-gray-500">
                              {startTime.toLocaleDateString()} at {startTime.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                            </p>
                            <p className="text-xs text-gray-400">
                              Duration: {Math.round((endTime.getTime() - startTime.getTime()) / (1000 * 60))} minutes
                            </p>
                          </div>
                        </div>
                        <div className="text-right">
                          <p className="text-sm font-medium text-gray-900">
                            {booking.status === 0 ? 'Pending' : 'Confirmed'}
                          </p>
                          <p className="text-xs text-gray-500">
                            {timeUntilStart <= 24 
                              ? `In ${timeUntilStart}h` 
                              : `In ${Math.ceil(timeUntilStart / 24)} days`
                            }
                          </p>
                        </div>
                      </div>
                    );
                  });
                })()}
              </div>
              {(() => {
                const now = new Date();
                const upcomingCount = bookings.filter(booking => {
                  const startTime = new Date(booking.startTime);
                  return startTime > now && (booking.status === 0 || booking.status === 1);
                }).length;
                
                return upcomingCount > 3 && (
                  <div className="mt-4 text-center">
                    <Link
                      to="/bookings"
                      className="text-blue-600 hover:text-blue-800 text-sm font-medium"
                    >
                      View all bookings →
                    </Link>
                  </div>
                );
              })()}
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default Dashboard;