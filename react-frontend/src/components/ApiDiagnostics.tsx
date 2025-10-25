import React, { useState } from 'react';

const ApiDiagnostics: React.FC = () => {
  const [testResults, setTestResults] = useState<any>(null);
  const [testing, setTesting] = useState(false);

  const runDiagnostics = async () => {
    setTesting(true);
    const results: any = {
      timestamp: new Date().toISOString(),
      baseURL: 'http://localhost:5105/api',
      endpoints: []
    };

    // Test 1: Check if token exists
    const token = localStorage.getItem('token');
    results.hasToken = !!token;
    results.tokenPreview = token ? `${token.substring(0, 20)}...` : 'No token';

    // Test 2: Check user info
    const user = localStorage.getItem('user');
    results.user = user ? JSON.parse(user) : null;

    // Test 3: Test various endpoint paths
    const endpointsToTest = [
      { path: '/api/auth/users', method: 'GET', description: 'Get All Users' },
      { path: '/api/users', method: 'GET', description: 'Get All Users (alt)' },
      { path: '/auth/users', method: 'GET', description: 'Get All Users (alt 2)' },
      { path: '/users', method: 'GET', description: 'Get All Users (alt 3)' }
    ];

    for (const endpoint of endpointsToTest) {
      try {
        const response = await fetch(`http://localhost:5105${endpoint.path}`, {
          method: endpoint.method,
          headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
          }
        });

        results.endpoints.push({
          endpoint: `${endpoint.method} ${endpoint.path}`,
          description: endpoint.description,
          status: response.status,
          statusText: response.statusText,
          success: response.ok
        });
      } catch (error: any) {
        results.endpoints.push({
          endpoint: `${endpoint.method} ${endpoint.path}`,
          description: endpoint.description,
          status: 'ERROR',
          statusText: error.message,
          success: false
        });
      }
    }

    setTestResults(results);
    setTesting(false);
  };

  return (
    <div className="bg-yellow-50 border border-yellow-300 rounded-lg p-4 mb-6">
      <div className="flex items-start">
        <svg className="h-6 w-6 text-yellow-600 mr-2 mt-0.5" fill="currentColor" viewBox="0 0 20 20">
          <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clipRule="evenodd" />
        </svg>
        <div className="flex-1">
          <h3 className="text-lg font-semibold text-yellow-900 mb-2">API Diagnostics</h3>
          <p className="text-sm text-yellow-800 mb-3">
            The endpoint is returning 404. Click below to run diagnostics and find the correct endpoint.
          </p>
          
          <button
            onClick={runDiagnostics}
            disabled={testing}
            className="bg-yellow-600 hover:bg-yellow-700 text-white px-4 py-2 rounded-md text-sm font-medium disabled:opacity-50"
          >
            {testing ? 'Testing Endpoints...' : 'Run API Diagnostics'}
          </button>

          {testResults && (
            <div className="mt-4 p-3 bg-white rounded border border-yellow-200">
              <h4 className="font-semibold text-gray-900 mb-2">Test Results:</h4>
              
              <div className="space-y-2 text-sm">
                <div>
                  <span className="font-medium">Token Status:</span>{' '}
                  <span className={testResults.hasToken ? 'text-green-600' : 'text-red-600'}>
                    {testResults.hasToken ? '✓ Present' : '✗ Missing'}
                  </span>
                </div>
                
                {testResults.user && (
                  <div>
                    <span className="font-medium">User Role:</span> {testResults.user.role}
                  </div>
                )}

                <div className="mt-3">
                  <span className="font-medium">Endpoint Tests:</span>
                  <div className="mt-2 space-y-1">
                    {testResults.endpoints.map((ep: any, idx: number) => (
                      <div key={idx} className="text-xs">
                        <div className="flex items-center font-mono">
                          <span className={ep.success ? 'text-green-600' : 'text-red-600'} style={{ width: '60px' }}>
                            {ep.status}
                          </span>
                          <span className="ml-2 flex-1">{ep.endpoint}</span>
                          {ep.success && <span className="ml-2 text-green-600 font-bold">✓ WORKING!</span>}
                        </div>
                        {ep.description && (
                          <div className="ml-16 text-gray-500 text-xs">{ep.description}</div>
                        )}
                      </div>
                    ))}
                  </div>
                </div>
              </div>

              <div className="mt-3 p-2 bg-gray-100 rounded text-xs">
                <p className="font-semibold text-gray-700">Instructions for Backend Developer:</p>
                <p className="text-gray-600 mt-1">
                  Based on test results, implement or verify the endpoint that returned 200 OK.
                  The frontend expects: <code className="bg-gray-200 px-1">GET /api/auth/users</code>
                </p>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default ApiDiagnostics;