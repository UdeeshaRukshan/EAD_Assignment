import React, { useState, useEffect } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { User } from '../types';
import { apiService } from '../services/apiService';
import ApiDiagnostics from '../components/ApiDiagnostics';

const UserManagement: React.FC = () => {
  const { user: currentUser } = useAuth();
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string>('');
  const [success, setSuccess] = useState<string>('');
  const [editingUser, setEditingUser] = useState<User | null>(null);
  const [showEditModal, setShowEditModal] = useState(false);
  const [formData, setFormData] = useState({
    firstName: '',
    lastName: '',
    email: '',
    phoneNumber: '',
    role: 1,
    nic: '',
    password: ''
  });

  useEffect(() => {
    if (currentUser?.role !== 'Admin') {
      setError('Access denied. Admin privileges required.');
      setLoading(false);
      return;
    }
    loadUsers();
  }, [currentUser]);

  const loadUsers = async () => {
    try {
      setLoading(true);
      setError('');
      const data = await apiService.getAllUsers();
      setUsers(data);
    } catch (err: any) {
      const status = err.response?.status;
      
      if (status === 401) {
        setError('Your session has expired. Please log in again.');
        // Optionally redirect to login after a delay
        setTimeout(() => {
          window.location.href = '/login';
        }, 2000);
      } else if (status === 403) {
        setError('Access Denied: You do not have permission to view users. Admin privileges required.');
      } else if (status === 404) {
        setError('API Endpoint Not Found: The user management endpoint (GET /api/auth/users) is not available on the backend. Please verify the backend endpoint is implemented and running.');
      } else {
        setError(err.response?.data?.message || 'Failed to load users. Please try again later.');
      }
      console.error('Load users error:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleEditUser = async (user: User) => {
    setEditingUser(user);
    
    // Fetch full user details to get NIC
    try {
      const fullUser = await apiService.getUserById(user.id);
      setFormData({
        firstName: fullUser.firstName,
        lastName: fullUser.lastName,
        email: fullUser.email,
        phoneNumber: fullUser.phoneNumber || '',
        role: getRoleNumber(fullUser.role),
        nic: (fullUser as any).nic || '',
        password: '' // Password will be optional for updates
      });
      setShowEditModal(true);
    } catch (error) {
      console.error('Failed to fetch user details:', error);
      // Fallback to available data
      setFormData({
        firstName: user.firstName,
        lastName: user.lastName,
        email: user.email,
        phoneNumber: user.phoneNumber || '',
        role: getRoleNumber(user.role),
        nic: '',
        password: ''
      });
      setShowEditModal(true);
    }
  };

  const handleUpdateUser = async () => {
    if (!editingUser) return;

    try {
      setError('');
      await apiService.updateUser(editingUser.id, formData);
      setSuccess('User updated successfully');
      setShowEditModal(false);
      setEditingUser(null);
      await loadUsers();
      setTimeout(() => setSuccess(''), 3000);
    } catch (err: any) {
      const status = err.response?.status;
      
      if (status === 401) {
        setError('Session expired. Please log in again.');
      } else if (status === 403) {
        setError('Access Denied: You do not have permission to update users.');
      } else if (status === 404) {
        setError('Backend Error: PUT /api/auth/users/{id} endpoint not found. The update user endpoint is not implemented on the backend. Contact the backend developer.');
      } else {
        setError(err.response?.data?.message || 'Failed to update user. Please try again.');
      }
      console.error('Update user error:', err);
      // Keep modal open on error so user can see the error and try again
    }
  };

  const handleDeleteUser = async (userId: string, userName: string) => {
    if (!confirm(`Are you sure you want to delete ${userName}? This action cannot be undone.`)) {
      return;
    }

    try {
      setError('');
      await apiService.deleteUserByAdmin(userId);
      setSuccess('User deleted successfully');
      await loadUsers();
      setTimeout(() => setSuccess(''), 3000);
    } catch (err: any) {
      const status = err.response?.status;
      
      if (status === 401) {
        setError('Session expired. Please log in again.');
      } else if (status === 403) {
        setError('Access Denied: You do not have permission to delete users.');
      } else if (status === 404) {
        setError('Backend Error: DELETE /api/auth/users/{id} endpoint not found. The delete user endpoint is not implemented on the backend.');
      } else {
        setError(err.response?.data?.message || 'Failed to delete user. Please try again.');
      }
      console.error('Delete user error:', err);
    }
  };

  const handleReactivateUser = async (userId: string, userName: string) => {
    if (!confirm(`Reactivate account for ${userName}?`)) {
      return;
    }

    try {
      setError('');
      await apiService.reactivateUser(userId);
      setSuccess('User reactivated successfully');
      await loadUsers();
      setTimeout(() => setSuccess(''), 3000);
    } catch (err: any) {
      const status = err.response?.status;
      
      if (status === 401) {
        setError('Session expired. Please log in again.');
      } else if (status === 403) {
        setError('Access Denied: You do not have permission to reactivate users.');
      } else if (status === 404) {
        setError('Backend Error: PATCH /api/auth/users/{id}/reactivate endpoint not found. The reactivate user endpoint is not implemented on the backend.');
      } else {
        setError(err.response?.data?.message || 'Failed to reactivate user. Please try again.');
      }
      console.error('Reactivate user error:', err);
    }
  };

  const handleDeactivateUser = async (userId: string, userName: string) => {
    if (!confirm(`Deactivate account for ${userName}? The user will no longer be able to log in until reactivated.`)) {
      return;
    }

    try {
      setError('');
      await apiService.deactivateUserByAdmin(userId);
      setSuccess('User deactivated successfully');
      await loadUsers();
      setTimeout(() => setSuccess(''), 3000);
    } catch (err: any) {
      const status = err.response?.status;
      const responseData = err.response?.data;
      
      console.error('Deactivate error details:', {
        status,
        statusText: err.response?.statusText,
        data: responseData,
        message: err.message
      });
      
      if (status === 401) {
        setError('Session expired. Please log in again.');
      } else if (status === 403) {
        setError('Access Denied: You do not have permission to deactivate users.');
      } else if (status === 404) {
        setError(`Backend Error: The deactivate endpoint returned 404. According to Swagger, PATCH /api/auth/users/{id}/deactivate should exist. Please check: 1) Backend is running on port 5105, 2) User ID is valid: ${userId.substring(0, 8)}..., 3) Backend logs for errors.`);
      } else if (status === 400) {
        setError(responseData?.message || 'Bad Request: ' + JSON.stringify(responseData));
      } else if (status === 500) {
        setError('Server Error: ' + (responseData?.message || 'Internal server error occurred'));
      } else {
        setError(err.response?.data?.message || `Failed to deactivate user. Status: ${status || 'unknown'}`);
      }
      console.error('Deactivate user error:', err);
    }
  };

  const getRoleNumber = (roleName: string): number => {
    const roleMap: { [key: string]: number } = {
      'Admin': 0,
      'EVOwner': 1,
      'Operator': 2,
      'BackofficeUser': 3
    };
    return roleMap[roleName] || 1;
  };

  const getRoleBadgeColor = (role: string): string => {
    const colors: { [key: string]: string } = {
      'Admin': 'bg-purple-100 text-purple-800',
      'EVOwner': 'bg-blue-100 text-blue-800',
      'Operator': 'bg-green-100 text-green-800',
      'BackofficeUser': 'bg-yellow-100 text-yellow-800'
    };
    return colors[role] || 'bg-gray-100 text-gray-800';
  };

  if (currentUser?.role !== 'Admin') {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="bg-white rounded-lg shadow-md p-8 max-w-md">
          <div className="text-center">
            <svg className="mx-auto h-12 w-12 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
            </svg>
            <h2 className="mt-4 text-xl font-bold text-gray-900">Access Denied</h2>
            <p className="mt-2 text-gray-600">You need Admin privileges to access this page.</p>
          </div>
        </div>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="flex justify-center items-center min-h-screen">
        <div className="animate-spin rounded-full h-32 w-32 border-b-2 border-blue-600"></div>
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
              <h1 className="text-3xl font-bold text-gray-900">User Management</h1>
              <p className="text-gray-600 mt-2">
                Manage system users - {users.length} total users
              </p>
            </div>
            <div className="flex items-center space-x-2">
              <div className="px-3 py-1 bg-purple-100 text-purple-800 rounded-full text-sm font-medium">
                Admin Access
              </div>
            </div>
          </div>

          {/* Info Banner */}
          <div className="mt-4 p-3 bg-blue-50 border border-blue-200 rounded-md">
            <div className="flex items-start">
              <svg className="h-5 w-5 text-blue-600 mt-0.5 mr-2" fill="currentColor" viewBox="0 0 20 20">
                <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clipRule="evenodd" />
              </svg>
              <div className="text-sm text-blue-800">
                <p className="font-medium">Admin Endpoint Information</p>
                <p className="mt-1">All user management operations require a valid Admin JWT token.</p>
                <ul className="mt-2 list-disc list-inside space-y-1">
                  <li>GET /api/auth/users - Get all users</li>
                  <li>GET /api/auth/users/{'{id}'} - Get user by ID</li>
                  <li>PUT /api/auth/users/{'{id}'} - Update user</li>
                  <li>DELETE /api/auth/users/{'{id}'} - Delete user permanently</li>
                  <li>PATCH /api/auth/users/{'{id}'}/reactivate - Activate inactive user</li>
                  <li>PATCH /api/auth/users/{'{id}'}/deactivate - Deactivate active user</li>
                </ul>
                <p className="mt-2 text-xs">Base URL: http://localhost:5105/api</p>
              </div>
            </div>
          </div>
        </div>

        {/* Messages */}
        {error && (
          <div className="mb-6">
            {error.includes('404') && <ApiDiagnostics />}
            
            <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-md">
              <div className="flex items-center">
                <svg className="h-5 w-5 mr-2" fill="currentColor" viewBox="0 0 20 20">
                  <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clipRule="evenodd" />
                </svg>
                <div className="flex-1">
                  <p className="font-medium">{error}</p>
                  {error.includes('404') && (
                    <div className="mt-2 text-sm">
                      <p className="font-semibold">Troubleshooting Steps:</p>
                      <ol className="list-decimal list-inside mt-1 space-y-1">
                        <li>Verify the backend API is running on http://localhost:5105</li>
                        <li>Check if the endpoint GET /api/auth/users is implemented in the backend</li>
                        <li>Ensure your Admin JWT token is valid (check browser console for token details)</li>
                        <li>Test the endpoint directly using Postman or curl with the Admin JWT token</li>
                        <li>Check backend logs for any errors or missing route configurations</li>
                      </ol>
                      <div className="mt-3 p-2 bg-red-100 rounded text-xs font-mono">
                        Expected URL: http://localhost:5105/api/auth/users
                      </div>
                    </div>
                  )}
                </div>
              </div>
            </div>
          </div>
        )}

        {success && (
          <div className="mb-6 bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded-md flex items-center">
            <svg className="h-5 w-5 mr-2" fill="currentColor" viewBox="0 0 20 20">
              <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
            </svg>
            {success}
          </div>
        )}

        {/* Stats Cards */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
          <div className="bg-white rounded-lg shadow p-6">
            <div className="text-2xl font-bold text-blue-600">{users.length}</div>
            <div className="text-sm text-gray-600">Total Users</div>
          </div>
          <div className="bg-white rounded-lg shadow p-6">
            <div className="text-2xl font-bold text-green-600">
              {users.filter(u => u.isActive).length}
            </div>
            <div className="text-sm text-gray-600">Active Users</div>
          </div>
          <div className="bg-white rounded-lg shadow p-6">
            <div className="text-2xl font-bold text-red-600">
              {users.filter(u => !u.isActive).length}
            </div>
            <div className="text-sm text-gray-600">Inactive Users</div>
          </div>
          <div className="bg-white rounded-lg shadow p-6">
            <div className="text-2xl font-bold text-purple-600">
              {users.filter(u => u.role === 'Admin').length}
            </div>
            <div className="text-sm text-gray-600">Administrators</div>
          </div>
        </div>

        {/* Users Table */}
        <div className="bg-white rounded-lg shadow overflow-hidden">
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    User
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Email
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Phone
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Role
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Status
                  </th>
                  <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {users.map((user) => (
                  <tr key={user.id} className="hover:bg-gray-50">
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="flex items-center">
                        <div className="h-10 w-10 rounded-full bg-blue-100 flex items-center justify-center">
                          <span className="text-blue-600 font-medium text-sm">
                            {user.firstName[0]}{user.lastName[0]}
                          </span>
                        </div>
                        <div className="ml-4">
                          <div className="text-sm font-medium text-gray-900">
                            {user.firstName} {user.lastName}
                          </div>
                          <div className="text-sm text-gray-500">ID: {user.id.substring(0, 8)}...</div>
                        </div>
                      </div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="text-sm text-gray-900">{user.email}</div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="text-sm text-gray-900">{user.phoneNumber || 'N/A'}</div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <span className={`px-2 py-1 inline-flex text-xs leading-5 font-semibold rounded-full ${getRoleBadgeColor(user.role)}`}>
                        {user.role}
                      </span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <span className={`px-2 py-1 inline-flex text-xs leading-5 font-semibold rounded-full ${
                        user.isActive ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
                      }`}>
                        {user.isActive ? 'Active' : 'Inactive'}
                      </span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                      <div className="flex justify-end space-x-2">
                        <button
                          onClick={() => handleEditUser(user)}
                          className="text-blue-600 hover:text-blue-900"
                          title="Edit User"
                        >
                          <svg className="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                          </svg>
                        </button>
                        
                        {!user.isActive ? (
                          <button
                            onClick={() => handleReactivateUser(user.id, `${user.firstName} ${user.lastName}`)}
                            className="text-green-600 hover:text-green-900"
                            title="Activate User"
                          >
                            <svg className="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                            </svg>
                          </button>
                        ) : (
                          user.id !== currentUser.id && (
                            <>
                              <button
                                onClick={() => handleDeactivateUser(user.id, `${user.firstName} ${user.lastName}`)}
                                className="text-orange-600 hover:text-orange-900"
                                title="Deactivate User"
                              >
                                <svg className="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M18.364 18.364A9 9 0 005.636 5.636m12.728 12.728A9 9 0 015.636 5.636m12.728 12.728L5.636 5.636" />
                                </svg>
                              </button>
                              <button
                                onClick={() => handleDeleteUser(user.id, `${user.firstName} ${user.lastName}`)}
                                className="text-red-600 hover:text-red-900"
                                title="Delete User Permanently"
                              >
                                <svg className="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                                </svg>
                              </button>
                            </>
                          )
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>

      {/* Edit User Modal */}
      {showEditModal && editingUser && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-lg max-w-md w-full p-6">
            <div className="flex justify-between items-center mb-6">
              <h3 className="text-xl font-bold text-gray-900">Edit User</h3>
              <button
                onClick={() => {
                  setShowEditModal(false);
                  setEditingUser(null);
                }}
                className="text-gray-400 hover:text-gray-600"
              >
                <svg className="h-6 w-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>

            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  NIC
                </label>
                <input
                  type="text"
                  value={formData.nic}
                  onChange={(e) => setFormData({ ...formData, nic: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                  placeholder="National ID Card"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  First Name
                </label>
                <input
                  type="text"
                  value={formData.firstName}
                  onChange={(e) => setFormData({ ...formData, firstName: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Last Name
                </label>
                <input
                  type="text"
                  value={formData.lastName}
                  onChange={(e) => setFormData({ ...formData, lastName: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Email
                </label>
                <input
                  type="email"
                  value={formData.email}
                  onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Phone Number
                </label>
                <input
                  type="text"
                  value={formData.phoneNumber}
                  onChange={(e) => setFormData({ ...formData, phoneNumber: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Role
                </label>
                <select
                  value={formData.role}
                  onChange={(e) => setFormData({ ...formData, role: Number(e.target.value) })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                  <option value={0}>Admin</option>
                  <option value={1}>EV Owner</option>
                  <option value={2}>Operator</option>
                  <option value={3}>Backoffice User</option>
                </select>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Password (optional)
                </label>
                <input
                  type="password"
                  value={formData.password}
                  onChange={(e) => setFormData({ ...formData, password: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                  placeholder="Leave blank to keep current password"
                />
                <p className="mt-1 text-xs text-gray-500">
                  Only fill this if you want to change the user's password
                </p>
              </div>
            </div>

            <div className="flex justify-end space-x-3 mt-6">
              <button
                onClick={() => {
                  setShowEditModal(false);
                  setEditingUser(null);
                }}
                className="px-4 py-2 bg-gray-200 text-gray-700 rounded-md hover:bg-gray-300"
              >
                Cancel
              </button>
              <button
                onClick={handleUpdateUser}
                className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
              >
                Update User
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default UserManagement;