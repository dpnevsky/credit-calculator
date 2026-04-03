import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import ApiService from '../services/api.service';
import type { ApplicationResponse } from '../types/api';
import './Application.css';

const statusLabels: Record<string, string> = {
  DRAFT: 'Черновик',
  PRESCORING_FAILED: 'Прескоринг не пройден',
  SUBMITTED: 'Отправлена',
  SCORING_COMPLETED: 'Скоринг завершён',
  SCORING_APPROVED: 'Одобрена',
  SCORING_REJECTED: 'Отклонена',
  OFFER_SELECTED: 'Оффер выбран',
  DOCUMENTS_REQUESTED: 'Документы запрошены',
  DOCUMENTS_READY: 'Документы готовы',
};

const statusColors: Record<string, string> = {
  DRAFT: '#6c757d',
  PRESCORING_FAILED: '#dc3545',
  SUBMITTED: '#17a2b8',
  SCORING_COMPLETED: '#28a745',
  SCORING_APPROVED: '#28a745',
  SCORING_REJECTED: '#dc3545',
  OFFER_SELECTED: '#007bff',
  DOCUMENTS_REQUESTED: '#ffc107',
  DOCUMENTS_READY: '#28a745',
};

const ApplicationsList: React.FC = () => {
  const navigate = useNavigate();
  const [applicationId, setApplicationId] = useState('');
  const [application, setApplication] = useState<ApplicationResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const formatMoney = (value: number) =>
    new Intl.NumberFormat('ru-RU', { style: 'currency', currency: 'RUB', maximumFractionDigits: 0 }).format(value);

  const formatDate = (dateStr: string) => {
    const date = new Date(dateStr);
    return date.toLocaleDateString('ru-RU', { day: '2-digit', month: '2-digit', year: 'numeric' });
  };

  const handleSearch = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!applicationId.trim()) return;

    setLoading(true);
    setError(null);
    setApplication(null);

    try {
      const result = await ApiService.getApplication(applicationId.trim());
      setApplication(result);
    } catch {
      setError('Заявка не найдена. Проверьте ID и попробуйте снова.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="page-container">
      <div className="card">
        <h2>Поиск заявки</h2>
        <form onSubmit={handleSearch} className="search-form">
          <div className="search-row">
            <input
              type="text"
              placeholder="Введите ID заявки..."
              value={applicationId}
              onChange={e => setApplicationId(e.target.value)}
              className="search-input"
            />
            <button type="submit" className="btn btn-primary" disabled={loading}>
              {loading ? 'Поиск...' : 'Найти'}
            </button>
          </div>
        </form>

        {error && <div className="error-banner">{error}</div>}

        {application && (
          <div className="application-card" onClick={() => navigate(`/applications/${application.applicationId}`)}>
            <div className="app-card-header">
              <span className="app-card-id">#{application.applicationId.slice(0, 8)}</span>
              <span className="status-badge" style={{ backgroundColor: statusColors[application.status] || '#6c757d' }}>
                {statusLabels[application.status] || application.status}
              </span>
            </div>
            <div className="app-card-body">
              <div className="app-card-row">
                <span>ФИО</span>
                <strong>{application.lastName} {application.firstName} {application.middleName || ''}</strong>
              </div>
              <div className="app-card-row">
                <span>Сумма</span>
                <strong>{formatMoney(application.amount)}</strong>
              </div>
              <div className="app-card-row">
                <span>Срок</span>
                <strong>{application.termMonths} мес.</strong>
              </div>
              <div className="app-card-row">
                <span>Создана</span>
                <strong>{formatDate(application.createdAt)}</strong>
              </div>
            </div>
            <div className="app-card-footer">
              <span className="link-text">Подробнее &rarr;</span>
            </div>
          </div>
        )}
      </div>

      <div className="cta-section">
        <p>Хотите оформить кредит?</p>
        <button className="btn btn-primary" onClick={() => navigate('/applications/new')}>
          Создать заявку
        </button>
      </div>
    </div>
  );
};

export default ApplicationsList;
