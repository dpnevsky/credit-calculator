import React from 'react';
import { useAuth } from '../context/AuthContext';
import { Link } from 'react-router-dom';
import './Auth.css';

const REGISTRATION_PROFILE_KEY = 'cc_registration_profile';

type StoredProfile = {
  email?: string;
  firstName?: string;
  lastName?: string;
  middleName?: string;
  birthDate?: string;
};

const Profile: React.FC = () => {
  const { user } = useAuth();
  let storedProfile: StoredProfile | null = null;

  try {
    const rawProfile = localStorage.getItem(REGISTRATION_PROFILE_KEY);
    storedProfile = rawProfile ? (JSON.parse(rawProfile) as StoredProfile) : null;
  } catch {
    storedProfile = null;
  }

  const sameUserProfile =
    storedProfile && user?.email && storedProfile.email && storedProfile.email === user.email
      ? storedProfile
      : null;

  const fullName = [
    user?.lastName || sameUserProfile?.lastName,
    user?.firstName || sameUserProfile?.firstName,
    user?.middleName || sameUserProfile?.middleName,
  ]
    .filter(Boolean)
    .join(' ');

  const birthDate = user?.birthDate || sameUserProfile?.birthDate || '';

  const formatBirthDate = (value: string) => {
    if (!value) {
      return 'Не указано';
    }

    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
      return value;
    }

    return date.toLocaleDateString('ru-RU', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
    });
  };

  return (
    <div className="profile-page">
      <div className="profile-card">
        <h2>Профиль пользователя</h2>
        {user && (
          <div className="profile-info">
            <div className="profile-field">
              <span className="profile-label">ФИО</span>
              <span className="profile-value">{fullName || user.name || 'Не указано'}</span>
            </div>
            <div className="profile-field">
              <span className="profile-label">Email</span>
              <span className="profile-value">{user.email}</span>
            </div>
            <div className="profile-field">
              <span className="profile-label">Дата рождения</span>
              <span className="profile-value">{formatBirthDate(birthDate)}</span>
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
