import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import ApiService from '../services/api.service';
import type { CreateApplicationRequest, PreliminaryOffer } from '../types/api';
import './Application.css';

const CreateApplication: React.FC = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [offers, setOffers] = useState<PreliminaryOffer[] | null>(null);
  const [applicationId, setApplicationId] = useState<string | null>(null);

  const [form, setForm] = useState<CreateApplicationRequest>({
    amount: 500000,
    termMonths: 12,
    firstName: '',
    lastName: '',
    middleName: '',
    email: '',
    birthDate: '',
    passportSeries: '',
    passportNumber: '',
  });

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value, type } = e.target;
    setForm(prev => ({
      ...prev,
      [name]: type === 'number' ? (value === '' ? '' : Number(value)) : value,
    }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    try {
      const result = await ApiService.createApplication(form);
      setApplicationId(result.applicationId);
      setOffers(result.offers);
    } catch (err: unknown) {
      if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('Ошибка при создании заявки');
      }
    } finally {
      setLoading(false);
    }
  };

  const formatMoney = (value: number) =>
    new Intl.NumberFormat('ru-RU', { style: 'currency', currency: 'RUB', maximumFractionDigits: 0 }).format(value);

  if (offers && applicationId) {
    return (
      <div className="page-container">
        <div className="card success-card">
          <div className="success-icon">&#10003;</div>
          <h2>Заявка создана</h2>
          <p className="app-id">ID: {applicationId}</p>
          <h3>Предварительные предложения</h3>
          <div className="offers-grid">
            {offers.map((offer, index) => (
              <div key={index} className="offer-card">
                <div className="offer-header">
                  <span className="offer-rate">{offer.rate}%</span>
                  <span className="offer-label">годовых</span>
                </div>
                <div className="offer-details">
                  <div className="offer-row">
                    <span>Сумма кредита</span>
                    <strong>{formatMoney(offer.totalAmount)}</strong>
                  </div>
                  <div className="offer-row">
                    <span>Ежемесячный платёж</span>
                    <strong>{formatMoney(offer.monthlyPayment)}</strong>
                  </div>
                  <div className="offer-row">
                    <span>Срок</span>
                    <strong>{offer.termMonths} мес.</strong>
                  </div>
                  <div className="offer-tags">
                    {offer.insuranceEnabled && <span className="tag tag-insurance">Страховка</span>}
                    {offer.salaryClient && <span className="tag tag-salary">Зарплатный клиент</span>}
                  </div>
                </div>
              </div>
            ))}
          </div>
          <div className="action-buttons">
            <button className="btn btn-primary" onClick={() => navigate(`/applications/${applicationId}`)}>
              Перейти к заявке
            </button>
            <button className="btn btn-secondary" onClick={() => { setOffers(null); setApplicationId(null); }}>
              Создать ещё
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="page-container">
      <div className="card">
        <h2>Новая кредитная заявка</h2>
        {error && <div className="error-banner">{error}</div>}
        <form onSubmit={handleSubmit} className="application-form">
          <fieldset>
            <legend>Параметры кредита</legend>
            <div className="form-row">
              <div className="form-field">
                <label htmlFor="amount">Сумма кредита (&#8381;)</label>
                <input type="number" id="amount" name="amount" value={form.amount} onChange={handleChange} min={20000} step={10000} required />
              </div>
              <div className="form-field">
                <label htmlFor="termMonths">Срок (месяцев)</label>
                <input type="number" id="termMonths" name="termMonths" value={form.termMonths} onChange={handleChange} min={6} max={360} required />
              </div>
            </div>
          </fieldset>

          <fieldset>
            <legend>Персональные данные</legend>
            <div className="form-row">
              <div className="form-field">
                <label htmlFor="lastName">Фамилия</label>
                <input type="text" id="lastName" name="lastName" value={form.lastName} onChange={handleChange} required />
              </div>
              <div className="form-field">
                <label htmlFor="firstName">Имя</label>
                <input type="text" id="firstName" name="firstName" value={form.firstName} onChange={handleChange} required />
              </div>
              <div className="form-field">
                <label htmlFor="middleName">Отчество</label>
                <input type="text" id="middleName" name="middleName" value={form.middleName} onChange={handleChange} />
              </div>
            </div>
            <div className="form-row">
              <div className="form-field">
                <label htmlFor="email">Email</label>
                <input type="email" id="email" name="email" value={form.email} onChange={handleChange} required />
              </div>
              <div className="form-field">
                <label htmlFor="birthDate">Дата рождения</label>
                <input type="date" id="birthDate" name="birthDate" value={form.birthDate} onChange={handleChange} required />
              </div>
            </div>
          </fieldset>

          <fieldset>
            <legend>Паспортные данные</legend>
            <div className="form-row">
              <div className="form-field">
                <label htmlFor="passportSeries">Серия</label>
                <input type="text" id="passportSeries" name="passportSeries" value={form.passportSeries} onChange={handleChange} maxLength={4} pattern="\d{4}" required />
              </div>
              <div className="form-field">
                <label htmlFor="passportNumber">Номер</label>
                <input type="text" id="passportNumber" name="passportNumber" value={form.passportNumber} onChange={handleChange} maxLength={6} pattern="\d{6}" required />
              </div>
            </div>
          </fieldset>

          <button type="submit" className="btn btn-primary btn-full" disabled={loading}>
            {loading ? 'Отправка...' : 'Создать заявку'}
          </button>
        </form>
      </div>
    </div>
  );
};

export default CreateApplication;
