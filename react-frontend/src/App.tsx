import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './contexts/AuthContext';
import LoginPage from './pages/LoginPage';
import Dashboard from './pages/Dashboard';
import StationsList from './pages/StationsList';
import CreateStation from './pages/CreateStation';
import EditStation from './pages/EditStation';
import BookingManagement from './pages/BookingManagement';
import ProtectedRoute from './components/ProtectedRoute';
import Layout from './components/Layout';
import RegisterPage from './pages/RegistrationPage';
import EVUserProfile from './pages/EVUserProfile';
import UserManagement from './pages/UserManagement';

function App() {
  return (
    <AuthProvider>
      <Router>
        <div className="min-h-screen bg-gray-50">
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route path='/register' element={<RegisterPage/>}/>
            <Route path='/profile' element={<EVUserProfile/>}/>
            <Route 
              path="/" 
              element={
                <ProtectedRoute>
                  <Layout>
                    <Dashboard />
                  </Layout>
                </ProtectedRoute>
              } 
            />
            <Route 
              path="/stations" 
              element={
                <ProtectedRoute>
                  <Layout>
                    <StationsList />
                  </Layout>
                </ProtectedRoute>
              } 
            />
            <Route 
              path="/stations/create" 
              element={
                <ProtectedRoute>
                  <Layout>
                    <CreateStation />
                  </Layout>
                </ProtectedRoute>
              } 
            />
            <Route 
              path="/stations/edit/:id" 
              element={
                <ProtectedRoute>
                  <Layout>
                    <EditStation />
                  </Layout>
                </ProtectedRoute>
              } 
            />
            <Route 
              path="/bookings" 
              element={
                <ProtectedRoute>
                  <Layout>
                    <BookingManagement />
                  </Layout>
                </ProtectedRoute>
              } 
            />
            <Route 
              path="/users" 
              element={
                <ProtectedRoute>
                  <Layout>
                    <UserManagement />
                  </Layout>
                </ProtectedRoute>
              } 
            />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </div>
      </Router>
    </AuthProvider>
  );
}

export default App;