import React from 'react';
import { useAuth } from '../context/AuthContext';

const Profile: React.FC = () => {
  const { user } = useAuth();

  return (
    <div className="App">
      <h1>Профиль пользователя</h1>
      {user && (
        <div>
          <p><strong>Email:</strong> {user.email}</p>
          <p><strong>Имя:</strong> {user.name || 'не указано'}</p>
          <p>Здесь будет история расчётов и другие данные</p>
        </div>
      )}
    </div>
  );
};

export default Profile;