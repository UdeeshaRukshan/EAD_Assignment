import React, { useState } from 'react';
import { Link, Navigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import RegisterPage from './RegistrationPage';

const LoginPage: React.FC = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const { login, user } = useAuth();

  // Redirect if already logged in
  if (user) {
    return <Navigate to="/" replace />;
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setIsLoading(true);

    const success = await login(email, password);
    
    if (!success) {
      setError('Invalid email or password');
    }
    
    setIsLoading(false);
  };

  const fillDemoCredentials = (role: 'admin' | 'operator' | 'user') => {
    const credentials = {
      admin: { email: 'admin@evstation.com', password: 'Admin123!' },
      operator: { email: 'operator@evstation.com', password: 'Operator123!' },
      user: { email: 'user@evstation.com', password: 'User123!' },
    };
    
    setEmail(credentials[role].email);
    setPassword(credentials[role].password);
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-50 via-white to-green-50">
      <div className="max-w-md w-full space-y-8 p-8 bg-white rounded-xl shadow-lg">
        <div className="text-center">
          <div className="flex justify-center mb-4">
            <div className="p-3 bg-blue-100 rounded-full">
              <svg className="h-12 w-12 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
              </svg>
            </div>
          </div>
          <h1 className="text-3xl font-bold text-gray-900">Welcome Back</h1>
          <p className="mt-2 text-gray-600">Sign in to EV Charging Station System</p>
        </div>

        <div className="bg-blue-50 p-4 rounded-md mb-4">
          <h3 className="text-sm font-semibold text-blue-900 mb-2">Demo Accounts</h3>
          <div className="space-y-2 text-sm">
            <button
              type="button"
              onClick={() => fillDemoCredentials('admin')}
              className="w-full text-left p-2 hover:bg-blue-100 rounded text-blue-800"
            >
              <span className="font-medium">Admin:</span> admin@evstation.com
            </button>
            <button
              type="button"
              onClick={() => fillDemoCredentials('operator')}
              className="w-full text-left p-2 hover:bg-blue-100 rounded text-blue-800"
            >
              <span className="font-medium">Operator:</span> operator@evstation.com
            </button>
            <button
              type="button"
              onClick={() => fillDemoCredentials('user')}
              className="w-full text-left p-2 hover:bg-blue-100 rounded text-blue-800"
            >
              <span className="font-medium">User:</span> user@evstation.com
            </button>
          </div>
        </div>

        <form className="space-y-6" onSubmit={handleSubmit}>
          {error && (
            <div className="bg-red-50 border border-red-200 text-red-600 px-4 py-3 rounded-md">
              {error}
            </div>
          )}

          <div>
            <label htmlFor="email" className="block text-sm font-medium text-gray-700 mb-2">
              Email Address
            </label>
            <input
              id="email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              placeholder="Enter your email"
            />
          </div>

          <div>
            <label htmlFor="password" className="block text-sm font-medium text-gray-700 mb-2">
              Password
            </label>
            <input
              id="password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              placeholder="Enter your password"
            />
          </div>

          <button
            type="submit"
            disabled={isLoading}
            className="w-full flex justify-center py-3 px-4 border border-transparent text-sm font-medium rounded-md text-white bg-gradient-to-r from-blue-600 to-blue-700 hover:from-blue-700 hover:to-blue-800 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50 disabled:cursor-not-allowed transition-all duration-200"
          >
            {isLoading ? (
              <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-white"></div>
            ) : (
              'Sign in'
            )}
          </button>
        </form>
        <div className='flex justify-center'>
          Don't have an Account. 
          <Link to="/register" className='text-blue-400 hover:underline'>Sign Up</Link>
        </div>
      </div>
    </div>
  );
};

export default LoginPage;