import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import './Navbar.css';

const Navbar: React.FC = () => {
  const { user, logout, login, isAuthenticated } = useAuth();
  const [mobileOpen, setMobileOpen] = useState(false);

  const handleLogout = async () => {
    setMobileOpen(false);
    await logout();
  };

  const handleLogin = async () => {
    setMobileOpen(false);
    await login();
  };

  const closeMenu = () => setMobileOpen(false);

  return (
    <nav className="navbar">
      <div className="navbar-container">
        <Link to="/" className="navbar-logo" onClick={closeMenu}>
          <span className="logo-icon">&#8381;</span>
          Кредитный калькулятор
        </Link>

        <button className="menu-toggle" onClick={() => setMobileOpen(!mobileOpen)} aria-label="Меню">
          <span className={`hamburger ${mobileOpen ? 'open' : ''}`}></span>
        </button>

        <div className={`nav-links ${mobileOpen ? 'nav-open' : ''}`}>
          <Link to="/" className="nav-link" onClick={closeMenu}>Калькулятор</Link>
          {isAuthenticated && (
            <>
              <Link to="/applications" className="nav-link" onClick={closeMenu}>Мои заявки</Link>
              <Link to="/applications/new" className="nav-link nav-cta" onClick={closeMenu}>Оформить кредит</Link>
            </>
          )}
          <div className="nav-divider"></div>
          {isAuthenticated ? (
            <>
              <span className="welcome-text">{user?.name || user?.email}</span>
              <Link to="/profile" className="nav-link" onClick={closeMenu}>Профиль</Link>
              <button onClick={handleLogout} className="nav-button">Выйти</button>
            </>
          ) : (
            <button onClick={handleLogin} className="nav-button nav-login-btn">Войти</button>
          )}
        </div>
      </div>
    </nav>
  );
};

export default Navbar;
