import React, { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import type { RegistrationPayload } from '../services/auth.service';
import './Auth.css';

interface RegisterFormState {
  email: string;
  firstName: string;
  lastName: string;
  middleName: string;
  birthDate: string;
  password: string;
  confirmPassword: string;
}

const INITIAL_FORM_STATE: RegisterFormState = {
  email: '',
  firstName: '',
  lastName: '',
  middleName: '',
  birthDate: '',
  password: '',
  confirmPassword: '',
};

const toLocalDateInputValue = (date: Date): string => {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
};

const getTodayDate = (): string => toLocalDateInputValue(new Date());

const buildRegistrationPayload = (form: RegisterFormState): RegistrationPayload => ({
  email: form.email.trim(),
  password: form.password,
  firstName: form.firstName.trim(),
  lastName: form.lastName.trim(),
  middleName: form.middleName.trim(),
  birthDate: form.birthDate.trim(),
});

const validateRegisterForm = (form: RegisterFormState): string | null => {
  if (form.birthDate && form.birthDate > getTodayDate()) {
    return 'Дата рождения не может быть в будущем';
  }

  if (form.password !== form.confirmPassword) {
    return 'Пароли не совпадают';
  }

  return null;
};

const Register: React.FC = () => {
  const { register, isAuthenticated, loading } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState<RegisterFormState>(INITIAL_FORM_STATE);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (isAuthenticated) {
      navigate('/', { replace: true });
    }
  }, [isAuthenticated, navigate]);

  const handleChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = event.target;
    setForm((prev) => ({
      ...prev,
      [name]: value,
    }));
  };

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError('');

    const validationError = validateRegisterForm(form);
    if (validationError) {
      setError(validationError);
      return;
    }

    setSubmitting(true);
    try {
      await register(buildRegistrationPayload(form));
    } catch (submitError) {
      setError(submitError instanceof Error ? submitError.message : 'Не удалось зарегистрироваться');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="auth-container">
      <div className="auth-card">
        <h2>Регистрация</h2>
        {error && <div className="error-message">{error}</div>}
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="register-email">Email</label>
            <input
              id="register-email"
              name="email"
              type="email"
              value={form.email}
              onChange={handleChange}
              required
              autoComplete="email"
            />
          </div>
          <div className="form-group">
            <label htmlFor="register-firstname">Имя</label>
            <input
              id="register-firstname"
              name="firstName"
              type="text"
              value={form.firstName}
              onChange={handleChange}
              required
              autoComplete="given-name"
            />
          </div>
          <div className="form-group">
            <label htmlFor="register-lastname">Фамилия</label>
            <input
              id="register-lastname"
              name="lastName"
              type="text"
              value={form.lastName}
              onChange={handleChange}
              required
              autoComplete="family-name"
            />
          </div>
          <div className="form-group">
            <label htmlFor="register-middlename">Отчество</label>
            <input
              id="register-middlename"
              name="middleName"
              type="text"
              value={form.middleName}
              onChange={handleChange}
              autoComplete="additional-name"
            />
          </div>
          <div className="form-group">
            <label htmlFor="register-birthdate">Дата рождения</label>
            <input
              id="register-birthdate"
              name="birthDate"
              type="date"
              value={form.birthDate}
              onChange={handleChange}
              max={getTodayDate()}
              required
              autoComplete="bday"
            />
          </div>
          <div className="form-group">
            <label htmlFor="register-password">Пароль</label>
            <input
              id="register-password"
              name="password"
              type="password"
              minLength={8}
              value={form.password}
              onChange={handleChange}
              required
              autoComplete="new-password"
            />
          </div>
          <div className="form-group">
            <label htmlFor="register-password-confirm">Повторите пароль</label>
            <input
              id="register-password-confirm"
              name="confirmPassword"
              type="password"
              minLength={8}
              value={form.confirmPassword}
              onChange={handleChange}
              required
              autoComplete="new-password"
            />
          </div>
          <button className="auth-button" type="submit" disabled={submitting || loading}>
            {submitting ? 'Создаем аккаунт...' : 'Зарегистрироваться'}
          </button>
        </form>
        <p className="auth-link">
          Уже есть аккаунт? <Link to="/login">Войти</Link>
        </p>
      </div>
    </div>
  );
};

export default Register;
