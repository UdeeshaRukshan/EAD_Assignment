import React, { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { apiService } from "../services/apiService";
import { ChargingStation, CreateStationRequest } from "../types";

const EditStation: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [station, setStation] = useState<ChargingStation | null>(null);
  const [formData, setFormData] = useState<Record<string, any>>({});
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    const loadStation = async () => {
      try {
        setLoading(true);
        const data = await apiService.getChargingStation(id!);
        setStation(data);
        setFormData({
          name: data.name,
          description: data.description,
          location: data.location,
          address: data.address,
          connectors: data.connectors,
          status: data.status,
          amenities: data.amenities,
          openingHours: data.openingHours,
          pricePerKWh: data.pricePerKWh,
          imageUrls: data.imageUrls,
        });
      } catch (err) {
        setError("Failed to load station details");
        console.error(err);
      } finally {
        setLoading(false);
      }
    };

    if (id) loadStation();
  }, [id]);

  const handleChange = (
    e: React.ChangeEvent<
      HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement
    >
  ) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!id) return;

    try {
      setSaving(true);
      setError("");

      // Create payload with only modified fields
      const payload: Record<string, any> = {};

      // Only include changed fields
      if (formData.name !== undefined && formData.name !== station?.name) {
        payload.name = formData.name;
      }
      if (formData.description !== undefined && formData.description !== station?.description) {
        payload.description = formData.description;
      }
      if (formData.address !== undefined && formData.address !== station?.address) {
        payload.address = formData.address;
      }
      if (formData.openingHours !== undefined && formData.openingHours !== station?.openingHours) {
        payload.openingHours = formData.openingHours;
      }

      // Convert numeric fields
      if (formData.pricePerKWh !== undefined && formData.pricePerKWh !== station?.pricePerKWh) {
        payload.pricePerKWh = Number(formData.pricePerKWh);
      }

      // Convert status to number if changed
      if (formData.status !== undefined && formData.status !== station?.status) {
        payload.status = Number(formData.status);
      }

      // Handle amenities - convert string to array if needed
      if (formData.amenities !== undefined && formData.amenities !== null) {
        if (typeof formData.amenities === "string" && formData.amenities.length > 0) {
          payload.amenities = formData.amenities
            .split(",")
            .map((a: string) => a.trim())
            .filter((a: string) => a.length > 0);
        } else if (Array.isArray(formData.amenities)) {
          payload.amenities = formData.amenities;
        }
      }

      // Only send if there are changes
      if (Object.keys(payload).length === 0) {
        setError("No changes detected");
        setSaving(false);
        return;
      }

      console.log("Sending PATCH request with payload:", payload);

      await apiService.patchChargingStation(id, payload);
      navigate("/stations");
    } catch (err: any) {
      console.error("Update failed:", err);
      const errorMsg = err.response?.data?.message || err.message || "Failed to update charging station.";
      setError(errorMsg);
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="animate-spin rounded-full h-16 w-16 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  if (!station) {
    return (
      <div className="min-h-screen flex items-center justify-center text-gray-600">
        Station not found.
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="bg-white rounded-lg shadow p-6">
          <h1 className="text-2xl font-bold text-gray-900 mb-6">
            Edit Charging Station
          </h1>

          {error && (
            <div className="mb-4 bg-red-50 text-red-700 px-4 py-2 rounded-md border border-red-200">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-6">
            <div>
              <label className="block text-sm font-medium text-gray-700">
                Name
              </label>
              <input
                type="text"
                name="name"
                value={formData.name || ""}
                onChange={handleChange}
                className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500"
                required
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700">
                Description
              </label>
              <textarea
                name="description"
                value={formData.description || ""}
                onChange={handleChange}
                className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500"
                rows={3}
              ></textarea>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700">
                Address
              </label>
              <input
                type="text"
                name="address"
                value={formData.address || ""}
                onChange={handleChange}
                className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500"
                required
              />
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700">
                  Price per kWh
                </label>
                <input
                  type="number"
                  name="pricePerKWh"
                  value={formData.pricePerKWh ?? ""}
                  onChange={handleChange}
                  step="0.01"
                  min="0"
                  className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700">
                  Opening Hours
                </label>
                <input
                  type="text"
                  name="openingHours"
                  value={formData.openingHours || ""}
                  onChange={handleChange}
                  placeholder="e.g., 24/7 or 9:00 AM - 6:00 PM"
                  className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500"
                />
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700">
                Amenities (comma-separated)
              </label>
              <input
                type="text"
                name="amenities"
                value={
                  Array.isArray(formData.amenities) 
                    ? formData.amenities.join(", ") 
                    : formData.amenities || ""
                }
                onChange={handleChange}
                placeholder="WiFi, Restroom, Parking"
                className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700">
                Status
              </label>
              <select
                name="status"
                value={formData.status ?? ""}
                onChange={handleChange}
                className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500"
              >
                <option value="0">Inactive</option>
                <option value="1">Active</option>
                <option value="2">Maintenance</option>
              </select>
            </div>

            <div className="flex justify-end space-x-3 pt-4">
              <button
                type="button"
                onClick={() => navigate("/stations")}
                className="px-4 py-2 bg-gray-100 text-gray-700 rounded-md hover:bg-gray-200"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={saving}
                className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50"
              >
                {saving ? "Saving..." : "Update Station"}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};

export default EditStation;