import React, { useEffect, useState } from 'react';
import { apiService } from '../services/apiService';
import { useAuth } from '../contexts/AuthContext';
import { useNavigate } from 'react-router-dom';
import { FiUser, FiMail, FiPhone, FiEdit, FiTrash2, FiToggleLeft, FiToggleRight, FiCreditCard, FiLogOut, FiHome, FiSettings, FiLock, FiBell, FiHelpCircle } from 'react-icons/fi';

const EVUserProfile: React.FC = () => {
  const { logout } = useAuth();
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState('overview');
  const [editMode, setEditMode] = useState(false);
  const [profile, setProfile] = useState<any>(null);
  const [formData, setFormData] = useState<any>({});
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');

  useEffect(() => {
    const fetchProfile = async () => {
      try {
        const res = await apiService.getProfile();
        setProfile(res);
        setFormData(res);
      } catch {
        setError('Error loading profile');
      }
    };
    fetchProfile();
  }, []);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleUpdate = async () => {
    try {
      await apiService.updateProfile(profile.nic, formData);
      setProfile(formData);
      setEditMode(false);
      setMessage('Profile updated successfully ✅');
      setTimeout(() => setMessage(''), 3000);
    } catch {
      setError('Update failed ❌');
      setTimeout(() => setError(''), 3000);
    }
  };

  const handleActivate = async () => {
    try {
      await apiService.activateUser(profile.nic);
      setProfile({ ...profile, isActive: true });
      setMessage('Account activated ✅');
      setTimeout(() => setMessage(''), 3000);
    } catch {
      setError('Activation failed');
      setTimeout(() => setError(''), 3000);
    }
  };

  const handleDeactivate = async () => {
    try {
      await apiService.deactivateUser(profile.nic);
      setProfile({ ...profile, isActive: false });
      setMessage('Account deactivated ✅');
      setTimeout(() => setMessage(''), 3000);
      logout();
    } catch {
      setError('Deactivation failed');
      setTimeout(() => setError(''), 3000);
    }
  };

  const handleDelete = async () => {
    if (window.confirm('Are you sure you want to delete your account? This action cannot be undone.')) {
      try {
        await apiService.deleteUser(profile.nic);
        logout();
      } catch {
        setError('Delete failed');
        setTimeout(() => setError(''), 3000);
      }
    }
  };

  const handleLogout = () => {
    logout();
  };

  if (!profile) return (
    <div className="flex items-center justify-center h-screen bg-gray-50">
      <p className="text-center text-gray-600 text-lg">Loading profile...</p>
    </div>
  );

  const sidebarItems = [
    { id: 'overview', label: 'Overview', icon: FiHome },
    { id: 'settings', label: 'Settings', icon: FiSettings },
    { id: 'security', label: 'Security', icon: FiLock },
    { id: 'notifications', label: 'Notifications', icon: FiBell },
    { id: 'help', label: 'Help & Support', icon: FiHelpCircle },
  ];

  return (
    <div className="flex h-screen bg-gray-50">
      {/* Sidebar */}
      <div className="w-64 bg-gradient-to-b from-green-700 to-green-900 text-white shadow-lg">
        <div className="p-6 border-b border-green-600">
          <div className="flex items-center gap-3">
            <div className="w-12 h-12 bg-green-300 rounded-full flex items-center justify-center">
              <FiUser className="text-green-900 text-xl" />
            </div>
            <div>
              <h3 className="font-bold text-sm">{profile.firstName} {profile.lastName}</h3>
              <p className="text-xs text-green-200">{profile.role || 'User'}</p>
            </div>
          </div>
        </div>

        <nav className="mt-8">
          {sidebarItems.map(({ id, label, icon: Icon }) => (
            <button
              key={id}
              onClick={() => setActiveTab(id)}
              className={`w-full flex items-center gap-3 px-6 py-3 transition-all ${
                activeTab === id
                  ? 'bg-green-600 border-r-4 border-green-300'
                  : 'hover:bg-green-600/50'
              }`}
            >
              <Icon className="text-lg" />
              <span>{label}</span>
            </button>
          ))}
        </nav>

        <div className="bottom-6 left-6 right-6 mt-80 flex justify-center">
          <button
            onClick={handleLogout}
            className="w-52 flex items-center justify-center gap-2 bg-red-600 hover:bg-red-900 text-white px-4 py-2 rounded-lg transition-colors "
          >
            <FiLogOut /> Logout
          </button>
        </div>
      </div>

      {/* Main Content */}
      <div className="flex-1 overflow-auto">
        {/* Header Banner */}
        <div className="bg-gradient-to-r from-green-600 via-green-500 to-blue-600 h-32 flex items-center px-8 shadow-md relative">
          <button
            className="absolute left-6 top-6 flex items-center gap-2 bg-white hover:bg-gray-200 text-green-700 px-4 py-2 rounded-lg shadow transition"
            onClick={() => navigate('/')}
          >
            <span className="font-bold">&#8592;</span> Go Back
          </button>
          <div className="ml-32">
            <h1 className="text-4xl font-bold text-white mb-2">
              {profile.firstName} {profile.lastName}
            </h1>
            <p className="text-green-100 text-lg">{profile.role || 'EV User'}</p>
          </div>
        </div>

        {/* Content Container */}
        <div className="p-8">
          {error && (
            <div className="mb-4 bg-red-100 border-l-4 border-red-500 text-red-700 p-4 rounded">
              {error}
            </div>
          )}
          {message && (
            <div className="mb-4 bg-green-100 border-l-4 border-green-500 text-green-700 p-4 rounded">
              {message}
            </div>
          )}

          {/* Overview Tab */}
          {activeTab === 'overview' && (
            <div className="space-y-6">
              {/* Profile Information */}
              <div className="bg-white rounded-lg shadow p-6">
                <div className="flex justify-between items-center mb-6">
                  <h2 className="text-2xl font-bold text-gray-800">Personal Information</h2>
                  {!editMode && (
                    <button
                      onClick={() => setEditMode(true)}
                      className="flex items-center gap-2 bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg transition"
                    >
                      <FiEdit /> Edit Profile
                    </button>
                  )}
                </div>

                {!editMode ? (
                  <div className="space-y-4">
                    <div className="flex items-center gap-4 pb-4 border-b">
                      <FiUser className="text-gray-400 text-xl" />
                      <div className="flex-1">
                        <p className="text-sm text-gray-500">Full Name</p>
                        <p className="text-lg font-semibold text-gray-800">{profile.firstName} {profile.lastName}</p>
                      </div>
                    </div>
                    <div className="flex items-center gap-4 pb-4 border-b">
                      <FiCreditCard className="text-gray-400 text-xl" />
                      <div className="flex-1">
                        <p className="text-sm text-gray-500">NIC</p>
                        <p className="text-lg font-semibold text-gray-800">{profile.nic || 'Not provided'}</p>
                      </div>
                    </div>
                    <div className="flex items-center gap-4 pb-4 border-b">
                      <FiMail className="text-gray-400 text-xl" />
                      <div className="flex-1">
                        <p className="text-sm text-gray-500">Email Address</p>
                        <p className="text-lg font-semibold text-gray-800">{profile.email}</p>
                      </div>
                    </div>
                    <div className="flex items-center gap-4">
                      <FiPhone className="text-gray-400 text-xl" />
                      <div className="flex-1">
                        <p className="text-sm text-gray-500">Phone Number</p>
                        <p className="text-lg font-semibold text-gray-800">{profile.phoneNumber}</p>
                      </div>
                    </div>
                  </div>
                ) : (
                  <div className="space-y-4">
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">First Name</label>
                      <input
                        name="firstName"
                        value={formData.firstName || ''}
                        onChange={handleChange}
                        placeholder="First Name"
                        className="w-full border border-gray-300 px-4 py-2 rounded-lg focus:outline-none focus:border-blue-500"
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">Last Name</label>
                      <input
                        name="lastName"
                        value={formData.lastName || ''}
                        onChange={handleChange}
                        placeholder="Last Name"
                        className="w-full border border-gray-300 px-4 py-2 rounded-lg focus:outline-none focus:border-blue-500"
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">Email</label>
                      <input
                        name="email"
                        value={formData.email || ''}
                        onChange={handleChange}
                        placeholder="Email"
                        className="w-full border border-gray-300 px-4 py-2 rounded-lg focus:outline-none focus:border-blue-500"
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">Phone Number</label>
                      <input
                        name="phoneNumber"
                        value={formData.phoneNumber || ''}
                        onChange={handleChange}
                        placeholder="Phone Number"
                        className="w-full border border-gray-300 px-4 py-2 rounded-lg focus:outline-none focus:border-blue-500"
                      />
                    </div>

                    <div className="flex gap-3 mt-6 justify-end pt-4 border-t">
                      <button
                        onClick={() => {
                          setEditMode(false);
                          setFormData(profile);
                        }}
                        className="bg-gray-400 hover:bg-gray-500 text-white px-6 py-2 rounded-lg transition"
                      >
                        Cancel
                      </button>
                      <button
                        onClick={handleUpdate}
                        className="bg-green-600 hover:bg-green-700 text-white px-6 py-2 rounded-lg transition"
                      >
                        Save Changes
                      </button>
                    </div>
                  </div>
                )}
              </div>

              {/* Account Status */}
              <div className="bg-white rounded-lg shadow p-6">
                <h2 className="text-2xl font-bold text-gray-800 mb-6">Account Status</h2>
                <div className="flex items-center justify-between p-4 bg-gray-50 rounded-lg mb-6">
                  <div>
                    <p className="text-gray-600 font-semibold">Account Status</p>
                    <p className="text-sm text-gray-500">Current account state</p>
                  </div>
                  <span className={`px-4 py-2 rounded-full font-bold ${profile.isActive ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'}`}>
                    {profile.isActive ? 'Active' : 'Inactive'}
                  </span>
                </div>

                <div className="flex gap-3">
                  {profile.isActive ? (
                    <button
                      onClick={handleDeactivate}
                      className="flex items-center gap-2 bg-yellow-500 hover:bg-yellow-600 text-white px-6 py-2 rounded-lg transition flex-1 justify-center"
                    >
                      <FiToggleLeft /> Deactivate Account
                    </button>
                  ) : (
                    <button
                      onClick={handleActivate}
                      className="flex items-center gap-2 bg-green-600 hover:bg-green-700 text-white px-6 py-2 rounded-lg transition flex-1 justify-center"
                    >
                      <FiToggleRight /> Activate Account
                    </button>
                  )}
                  <button
                    onClick={handleDelete}
                    className="flex items-center gap-2 bg-red-600 hover:bg-red-700 text-white px-6 py-2 rounded-lg transition"
                  >
                    <FiTrash2 /> Delete
                  </button>
                </div>
              </div>
            </div>
          )}

          {/* Settings Tab */}
          {activeTab === 'settings' && (
            <div className="bg-white rounded-lg shadow p-6">
              <h2 className="text-2xl font-bold text-gray-800 mb-6">Settings</h2>
              <div className="space-y-4">
                <div className="flex items-center justify-between p-4 border rounded-lg hover:bg-gray-50 transition">
                  <span className="text-gray-700 font-medium">Dark Mode</span>
                  <input type="checkbox" className="w-5 h-5 cursor-pointer" />
                </div>
                <div className="flex items-center justify-between p-4 border rounded-lg hover:bg-gray-50 transition">
                  <span className="text-gray-700 font-medium">Email Notifications</span>
                  <input type="checkbox" className="w-5 h-5 cursor-pointer" defaultChecked />
                </div>
                <div className="flex items-center justify-between p-4 border rounded-lg hover:bg-gray-50 transition">
                  <span className="text-gray-700 font-medium">Marketing Emails</span>
                  <input type="checkbox" className="w-5 h-5 cursor-pointer" />
                </div>
                <div className="flex items-center justify-between p-4 border rounded-lg hover:bg-gray-50 transition">
                  <span className="text-gray-700 font-medium">SMS Notifications</span>
                  <input type="checkbox" className="w-5 h-5 cursor-pointer" defaultChecked />
                </div>
              </div>
            </div>
          )}

          {/* Security Tab */}
          {activeTab === 'security' && (
            <div className="space-y-6">
              <div className="bg-white rounded-lg shadow p-6">
                <h2 className="text-2xl font-bold text-gray-800 mb-6">Security</h2>
                <button className="w-full bg-blue-600 hover:bg-blue-700 text-white px-6 py-3 rounded-lg transition text-left font-semibold flex items-center gap-2">
                  <FiLock /> Change Password
                </button>
                <div className="mt-6 p-4 bg-blue-50 border border-blue-200 rounded-lg">
                  <p className="text-sm text-blue-800">Last password change: 3 months ago</p>
                </div>
              </div>

              <div className="bg-white rounded-lg shadow p-6">
                <h2 className="text-2xl font-bold text-gray-800 mb-6">Two-Factor Authentication</h2>
                <div className="p-4 bg-gray-50 rounded-lg">
                  <p className="text-gray-700 font-semibold mb-2">Status: Inactive</p>
                  <p className="text-sm text-gray-600 mb-4">Add an extra layer of security to your account</p>
                  <button className="bg-green-600 hover:bg-green-700 text-white px-6 py-2 rounded-lg transition">
                    Enable 2FA
                  </button>
                </div>
              </div>
            </div>
          )}

          {/* Notifications Tab */}
          {activeTab === 'notifications' && (
            <div className="bg-white rounded-lg shadow p-6">
              <h2 className="text-2xl font-bold text-gray-800 mb-6">Notifications</h2>
              <div className="space-y-4">
                <div className="p-4 border rounded-lg hover:bg-gray-50 transition">
                  <div className="flex justify-between items-center">
                    <div>
                      <h3 className="font-semibold text-gray-800">Trip Updates</h3>
                      <p className="text-sm text-gray-600 mt-1">Get notified about your trip status</p>
                    </div>
                    <input type="checkbox" className="w-5 h-5 cursor-pointer" defaultChecked />
                  </div>
                </div>
                <div className="p-4 border rounded-lg hover:bg-gray-50 transition">
                  <div className="flex justify-between items-center">
                    <div>
                      <h3 className="font-semibold text-gray-800">Maintenance Alerts</h3>
                      <p className="text-sm text-gray-600 mt-1">Receive vehicle maintenance reminders</p>
                    </div>
                    <input type="checkbox" className="w-5 h-5 cursor-pointer" defaultChecked />
                  </div>
                </div>
                <div className="p-4 border rounded-lg hover:bg-gray-50 transition">
                  <div className="flex justify-between items-center">
                    <div>
                      <h3 className="font-semibold text-gray-800">Billing Notifications</h3>
                      <p className="text-sm text-gray-600 mt-1">Notifications about payments and invoices</p>
                    </div>
                    <input type="checkbox" className="w-5 h-5 cursor-pointer" defaultChecked />
                  </div>
                </div>
                <div className="p-4 border rounded-lg hover:bg-gray-50 transition">
                  <div className="flex justify-between items-center">
                    <div>
                      <h3 className="font-semibold text-gray-800">Promotional Offers</h3>
                      <p className="text-sm text-gray-600 mt-1">Special deals and promotional updates</p>
                    </div>
                    <input type="checkbox" className="w-5 h-5 cursor-pointer" />
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* Help Tab */}
          {activeTab === 'help' && (
            <div className="bg-white rounded-lg shadow p-6">
              <h2 className="text-2xl font-bold text-gray-800 mb-6">Help & Support</h2>
              <div className="space-y-4">
                <div className="p-4 bg-blue-50 border border-blue-200 rounded-lg hover:shadow-md transition cursor-pointer">
                  <h3 className="font-semibold text-blue-900 mb-2">❓ FAQ</h3>
                  <p className="text-sm text-blue-800">Find answers to common questions about our EV platform</p>
                </div>
                <div className="p-4 bg-green-50 border border-green-200 rounded-lg hover:shadow-md transition cursor-pointer">
                  <h3 className="font-semibold text-green-900 mb-2">📧 Contact Support</h3>
                  <p className="text-sm text-green-800">Email: support@evapp.com | Phone: +1 (555) 123-4567</p>
                </div>
                <div className="p-4 bg-purple-50 border border-purple-200 rounded-lg hover:shadow-md transition cursor-pointer">
                  <h3 className="font-semibold text-purple-900 mb-2">📚 Documentation</h3>
                  <p className="text-sm text-purple-800">Browse our comprehensive guides and tutorials</p>
                </div>
                <div className="p-4 bg-orange-50 border border-orange-200 rounded-lg hover:shadow-md transition cursor-pointer">
                  <h3 className="font-semibold text-orange-900 mb-2">🐛 Report Issue</h3>
                  <p className="text-sm text-orange-800">Report a bug or technical issue</p>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default EVUserProfile;