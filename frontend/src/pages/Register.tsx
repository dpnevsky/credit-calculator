import React, { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import './Auth.css';

const Register: React.FC = () => {
  const { register, isAuthenticated, loading } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    if (isAuthenticated) {
      navigate('/');
    }
  }, [isAuthenticated, navigate]);

  useEffect(() => {
    if (!loading && !isAuthenticated) {
      register();
    }
  }, [loading, isAuthenticated, register]);

  return (
    <div className="auth-container">
      <div className="auth-card">
        <h2>Регистрация</h2>
        <p style={{ textAlign: 'center', color: '#6b7280' }}>
          Перенаправление на страницу регистрации...
        </p>
        <div className="loading-spinner" style={{ display: 'flex', justifyContent: 'center', marginTop: '20px' }}>
          <div className="spinner"></div>
        </div>
      </div>
    </div>
  );
};

export default Register;
