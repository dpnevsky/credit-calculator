import React from 'react';
import { useAuth } from '../context/AuthContext';
import { Link } from 'react-router-dom';
import './Auth.css';

const Profile: React.FC = () => {
  const { user } = useAuth();

  return (
    <div className="profile-page">
      <div className="profile-card">
        <h2>Профиль пользователя</h2>
        {user && (
          <div className="profile-info">
            <div className="profile-field">
              <span className="profile-label">Email</span>
              <span className="profile-value">{user.email}</span>
            </div>
            <div className="profile-field">
              <span className="profile-label">Имя</span>
              <span className="profile-value">{user.firstName || user.name || 'Не указано'}</span>
            </div>
            <div className="profile-field">
              <span className="profile-label">Фамилия</span>
              <span className="profile-value">{user.lastName || 'Не указано'}</span>
            </div>
            <div className="profile-field">
              <span className="profile-label">Отчество</span>
              <span className="profile-value">{user.middleName || 'Не указано'}</span>
            </div>
            <div style={{ marginTop: '16px' }}>
              <Link to="/applications" className="auth-button" style={{ display: 'block', textAlign: 'center', textDecoration: 'none' }}>
                Мои заявки
              </Link>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default Profile;
