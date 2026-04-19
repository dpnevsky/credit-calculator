import React from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import './Auth.css';

const NOT_SPECIFIED_LABEL = 'Не указано';

const formatBirthDate = (value: string): string => {
  if (!value) {
    return NOT_SPECIFIED_LABEL;
  }

  const match = value.match(/^(\d{4})-(\d{2})-(\d{2})$/);
  if (!match) {
    return value;
  }

  const [, year, month, day] = match;
  return `${day}.${month}.${year}`;
};

const Profile: React.FC = () => {
  const { user } = useAuth();

  const fullName = [user?.lastName, user?.firstName, user?.middleName]
    .filter(Boolean)
    .join(' ');

  return (
    <div className="profile-page">
      <div className="profile-card">
        <h2>Профиль пользователя</h2>
        <div className="profile-info">
          <div className="profile-field">
            <span className="profile-label">ФИО</span>
            <span className="profile-value">{fullName || user?.name || NOT_SPECIFIED_LABEL}</span>
          </div>
          <div className="profile-field">
            <span className="profile-label">Email</span>
            <span className="profile-value">{user?.email || NOT_SPECIFIED_LABEL}</span>
          </div>
          <div className="profile-field">
            <span className="profile-label">Дата рождения</span>
            <span className="profile-value">{formatBirthDate(user?.birthDate || '')}</span>
          </div>
          <div className="profile-actions">
            <Link to="/applications" className="auth-button profile-link-button">
              Мои заявки
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Profile;
