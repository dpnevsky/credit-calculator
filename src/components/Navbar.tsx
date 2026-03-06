import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import './Navbar.css';

const Navbar: React.FC = () => {
  const { user, logout, isAuthenticated } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  return (
    <nav className="navbar">
      <div className="navbar-container">
        <Link to="/" className="navbar-logo">
          Кредитный калькулятор
        </Link>
        <div className="nav-links">
          {isAuthenticated ? (
            <>
              <span className="welcome-text">Привет, {user?.name || user?.email}</span>
              <Link to="/profile" className="nav-link">Профиль</Link>
              <button onClick={handleLogout} className="nav-button">Выйти</button>
            </>
          ) : (
            <>
              <Link to="/login" className="nav-link">Вход</Link>
              <Link to="/register" className="nav-link">Регистрация</Link>
            </>
          )}
        </div>
      </div>
    </nav>
  );
};

export default Navbar;