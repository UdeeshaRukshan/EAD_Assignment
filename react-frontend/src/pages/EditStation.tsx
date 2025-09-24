import React from 'react';
import { useParams } from 'react-router-dom';

const EditStation: React.FC = () => {
  const { id } = useParams<{ id: string }>();

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="bg-white rounded-lg shadow p-6">
          <h1 className="text-2xl font-bold text-gray-900 mb-4">Edit Charging Station</h1>
          <p className="text-gray-600">Station ID: {id}</p>
          <div className="mt-6 p-4 bg-yellow-50 border border-yellow-200 rounded-md">
            <p className="text-yellow-800">
              Edit station functionality is coming soon! For now, you can view and manage station status from the stations list.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default EditStation;