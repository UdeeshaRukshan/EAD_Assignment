import React, { useEffect, useState } from 'react';
import { apiService } from '../services/apiService';
import { useAuth } from '../contexts/AuthContext';
import { FiUser, FiMail, FiPhone, FiEdit, FiTrash2, FiToggleLeft, FiToggleRight, FiCreditCard } from 'react-icons/fi';

const EVUserProfile: React.FC = () => {
  const { logout } = useAuth();
  const [profile, setProfile] = useState<any>(null);
  const [editMode, setEditMode] = useState(false);
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
    } catch {
      setError('Update failed ❌');
    }
  };

  const handleActivate = async () => {
    try {
      await apiService.activateUser(profile.nic);
      setProfile({ ...profile, isActive: true });
      setMessage('Account activated ✅');
    } catch {
      setError('Activation failed');
    }
  };

  const handleDeactivate = async () => {
    try {
      await apiService.deactivateUser(profile.nic);
      setProfile({ ...profile, isActive: false });
      setMessage('Account deactivated ❌');
      logout();
    } catch {
      setError('Deactivation failed');
    }
  };

  const handleDelete = async () => {
    try {
      await apiService.deleteUser(profile.nic);
      logout();
    } catch {
      setError('Delete failed');
    }
  };

  if (!profile) return <p className="text-center mt-10">Loading profile...</p>;

  return (
    <div className="min-h-screen bg-gradient-to-br from-green-50 via-white to-blue-50">
      {/* Banner */}
      <div className="bg-gradient-to-r from-green-600 to-blue-600 h-48 flex items-center justify-center">
        <h1 className="text-4xl font-bold text-white">My Profile</h1>
      </div>

      {/* Content */}
      <div className="max-w-4xl mx-auto -mt-20 bg-white rounded-xl shadow-xl p-8">
        <div className="flex flex-col items-center">
          <div className="w-28 h-28 rounded-full bg-green-100 flex items-center justify-center shadow-md">
            <FiUser className="text-green-600 text-6xl" />
          </div>
          <h2 className="mt-4 text-2xl font-bold text-gray-800">
            {profile.firstName} {profile.lastName}
          </h2>
          <p className="text-gray-500">{profile.role}</p>
        </div>

        {error && <div className="bg-red-100 text-red-600 p-2 rounded mt-4">{error}</div>}
        {message && <div className="bg-green-100 text-green-700 p-2 rounded mt-4">{message}</div>}

        <div className="mt-8 space-y-6">
          {!editMode ? (
            <>
              <div className="flex items-center gap-3">
                <FiCreditCard className="text-gray-500" />
                <span><strong>NIC:</strong> {profile.nic || 'Not provided'}</span>
              </div>
              <div className="flex items-center gap-3">
                <FiMail className="text-gray-500" />
                <span>{profile.email}</span>
              </div>
              <div className="flex items-center gap-3">
                <FiPhone className="text-gray-500" />
                <span>{profile.phoneNumber}</span>
              </div>
              <div>
                <strong>Status:</strong>{' '}
                {profile.isActive ? (
                  <span className="text-green-600 font-semibold">Active ✅</span>
                ) : (
                  <span className="text-red-600 font-semibold">Inactive ❌</span>
                )}
              </div>

              <div className="flex flex-wrap justify-center gap-4 mt-6">
                <button
                  onClick={() => setEditMode(true)}
                  className="flex items-center gap-2 bg-blue-600 text-white px-5 py-2 rounded-lg hover:bg-blue-700"
                >
                  <FiEdit /> Edit
                </button>
                {profile.isActive ? (
                  <button
                    onClick={handleDeactivate}
                    className="flex items-center gap-2 bg-yellow-500 text-white px-5 py-2 rounded-lg hover:bg-yellow-600"
                  >
                    <FiToggleLeft /> Deactivate
                  </button>
                ) : (
                  <button
                    onClick={handleActivate}
                    className="flex items-center gap-2 bg-green-600 text-white px-5 py-2 rounded-lg hover:bg-green-700"
                  >
                    <FiToggleRight /> Activate
                  </button>
                )}
                <button
                  onClick={handleDelete}
                  className="flex items-center gap-2 bg-red-600 text-white px-5 py-2 rounded-lg hover:bg-red-700"
                >
                  <FiTrash2 /> Delete
                </button>
              </div>
            </>
          ) : (
            <div className="space-y-4">
              <input
                name="firstName"
                value={formData.firstName}
                onChange={handleChange}
                className="w-full border p-2 rounded-lg"
                placeholder="First Name"
              />
              <input
                name="lastName"
                value={formData.lastName}
                onChange={handleChange}
                className="w-full border p-2 rounded-lg"
                placeholder="Last Name"
              />
              <input
                name="email"
                value={formData.email}
                onChange={handleChange}
                className="w-full border p-2 rounded-lg"
                placeholder="Email"
              />
              <input
                name="phoneNumber"
                value={formData.phoneNumber}
                onChange={handleChange}
                className="w-full border p-2 rounded-lg"
                placeholder="Phone Number"
              />

              <div className="flex gap-3 mt-6 justify-center">
                <button
                  onClick={handleUpdate}
                  className="bg-green-600 text-white px-5 py-2 rounded-lg hover:bg-green-700"
                >
                  Save
                </button>
                <button
                  onClick={() => setEditMode(false)}
                  className="bg-gray-400 text-white px-5 py-2 rounded-lg hover:bg-gray-500"
                >
                  Cancel
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default EVUserProfile;
