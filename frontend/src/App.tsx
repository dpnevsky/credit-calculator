import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { AuthProvider } from './context/AuthProvider';
import Navbar from './components/Navbar';
import PrivateRoute from './components/PrivateRoute';
import Login from './pages/Login';
import Register from './pages/Register';
import Profile from './pages/Profile';
import CreateApplication from './pages/CreateApplication';
import ApplicationsList from './pages/ApplicationsList';
import ApplicationDetails from './pages/ApplicationDetails';
import ContractPage from './pages/ContractPage';
import CalculatorPage from './pages/CalculatorPage';
import './App.css';

function App() {
  return (
    <Router>
      <AuthProvider>
        <Navbar />
        <main className="main-content">
          <Routes>
            {/* Public routes */}
            <Route path="/" element={<CalculatorPage />} />
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />

            {/* Protected routes */}
            <Route path="/applications" element={<PrivateRoute><ApplicationsList /></PrivateRoute>} />
            <Route path="/applications/new" element={<PrivateRoute><CreateApplication /></PrivateRoute>} />
            <Route path="/applications/:applicationId" element={<PrivateRoute><ApplicationDetails /></PrivateRoute>} />
            <Route path="/applications/:applicationId/contract" element={<PrivateRoute><ContractPage /></PrivateRoute>} />
            <Route path="/profile" element={<PrivateRoute><Profile /></PrivateRoute>} />
          </Routes>
        </main>
      </AuthProvider>
    </Router>
  );
}

export default App;
