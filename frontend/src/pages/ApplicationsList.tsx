import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import ApiService from '../services/api.service';
import type { ApplicationResponse } from '../types/api';
import {
  getApplicationRejectionReasons,
  getApplicationStatusColor,
  getApplicationStatusLabel,
  isRejectedApplicationStatus,
} from '../utils/applicationStatus';
import './Application.css';

const ApplicationsList: React.FC = () => {
  const navigate = useNavigate();
  const [applications, setApplications] = useState<ApplicationResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const loadApplications = async () => {
      setLoading(true);
      setError(null);
      try {
        const result = await ApiService.getApplications();
        setApplications(result);
      } catch {
        setError('Не удалось загрузить список заявок');
      } finally {
        setLoading(false);
      }
    };

    void loadApplications();
  }, []);

  const formatMoney = (value: number) =>
    new Intl.NumberFormat('ru-RU', { style: 'currency', currency: 'RUB', maximumFractionDigits: 0 }).format(value);

  const formatDate = (dateStr: string) => {
    const date = new Date(dateStr);
    return date.toLocaleDateString('ru-RU', { day: '2-digit', month: '2-digit', year: 'numeric' });
  };

  return (
    <div className="page-container">
      <div className="card">
        <h2>Мои заявки</h2>
        {loading && <p>Загрузка...</p>}
        {error && <div className="error-banner">{error}</div>}

        {!loading && !error && applications.length === 0 && (
          <p>У вас пока нет заявок. Создайте первую заявку.</p>
        )}

        {!loading && !error && applications.length > 0 && (
          <div className="applications-grid">
            {applications.map((application) => {
              const rejectionReasons = getApplicationRejectionReasons(application.status);

              return (
                <div
                  key={application.applicationId}
                  className="application-card"
                  onClick={() => navigate(`/applications/${application.applicationId}`)}
                >
                  <div className="app-card-header">
                    <span className="app-card-id">Номер заявки: {application.applicationId.slice(0, 8)}</span>
                    <span
                      className="status-badge"
                      style={{ backgroundColor: getApplicationStatusColor(application.status, application.applicationId) }}
                    >
                      {getApplicationStatusLabel(application.status, application.applicationId)}
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
                  {isRejectedApplicationStatus(application.status) && rejectionReasons.length > 0 && (
                    <div className="error-banner app-card-rejection">
                      Причина отказа: {rejectionReasons[0]}
                    </div>
                  )}
                  <div className="app-card-footer">
                    <span className="link-text">Подробнее &rarr;</span>
                  </div>
                </div>
              );
            })}
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
