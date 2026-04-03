import React, { useEffect, useState, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import ApiService from '../services/api.service';
import type {
  ApplicationResponse,
  ScoringResultResponse,
  OfferResponse,
  DocumentResponse,
} from '../types/api';
import SubmitApplicationModal from '../components/SubmitApplicationModal';
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

const ApplicationDetails: React.FC = () => {
  const { applicationId } = useParams<{ applicationId: string }>();
  const navigate = useNavigate();

  const [application, setApplication] = useState<ApplicationResponse | null>(null);
  const [scoringResult, setScoringResult] = useState<ScoringResultResponse | null>(null);
  const [offers, setOffers] = useState<OfferResponse[]>([]);
  const [documents, setDocuments] = useState<DocumentResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [actionLoading, setActionLoading] = useState(false);
  const [showSubmitModal, setShowSubmitModal] = useState(false);

  const loadData = useCallback(async () => {
    if (!applicationId) return;
    try {
      const app = await ApiService.getApplication(applicationId);
      setApplication(app);

      try {
        const scoring = await ApiService.getScoringResult(applicationId);
        setScoringResult(scoring);
      } catch {
        /* no scoring yet */
      }

      try {
        const offersData = await ApiService.getOffers(applicationId);
        setOffers(offersData);
      } catch {
        /* no offers yet */
      }

      try {
        const docs = await ApiService.getDocuments(applicationId);
        setDocuments(docs);
      } catch {
        /* no documents yet */
      }
    } catch (err: unknown) {
      if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('Ошибка загрузки заявки');
      }
    } finally {
      setLoading(false);
    }
  }, [applicationId]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const handleSelectOffer = async (offerId: string) => {
    if (!applicationId) return;
    setActionLoading(true);
    try {
      await ApiService.selectOffer(applicationId, offerId);
      await loadData();
    } catch (err: unknown) {
      if (err instanceof Error) {
        setError(err.message);
      }
    } finally {
      setActionLoading(false);
    }
  };

  const handleRequestDocuments = async () => {
    if (!applicationId) return;
    setActionLoading(true);
    try {
      await ApiService.requestDocuments(applicationId);
      await loadData();
    } catch (err: unknown) {
      if (err instanceof Error) {
        setError(err.message);
      }
    } finally {
      setActionLoading(false);
    }
  };

  const handleSubmitComplete = async () => {
    setShowSubmitModal(false);
    setLoading(true);
    await loadData();
  };

  const formatMoney = (value: number) =>
    new Intl.NumberFormat('ru-RU', { style: 'currency', currency: 'RUB', maximumFractionDigits: 0 }).format(value);

  const formatDate = (dateStr: string) => {
    const date = new Date(dateStr);
    return date.toLocaleDateString('ru-RU', { day: '2-digit', month: '2-digit', year: 'numeric' });
  };

  if (loading) {
    return (
      <div className="page-container">
        <div className="loading-spinner">
          <div className="spinner"></div>
          <p>Загрузка...</p>
        </div>
      </div>
    );
  }

  if (error || !application) {
    return (
      <div className="page-container">
        <div className="card error-card">
          <h2>Ошибка</h2>
          <p>{error || 'Заявка не найдена'}</p>
          <button className="btn btn-primary" onClick={() => navigate('/applications')}>К списку заявок</button>
        </div>
      </div>
    );
  }

  return (
    <div className="page-container">
      {showSubmitModal && applicationId && (
        <SubmitApplicationModal
          applicationId={applicationId}
          onClose={() => setShowSubmitModal(false)}
          onSuccess={handleSubmitComplete}
        />
      )}

      <div className="detail-header">
        <div>
          <h2>Заявка #{applicationId?.slice(0, 8)}</h2>
          <span className="status-badge" style={{ backgroundColor: statusColors[application.status] || '#6c757d' }}>
            {statusLabels[application.status] || application.status}
          </span>
        </div>
        <div className="header-actions">
          {application.status === 'DRAFT' && (
            <button className="btn btn-primary" onClick={() => setShowSubmitModal(true)}>
              Отправить на скоринг
            </button>
          )}
          {(application.status === 'SCORING_COMPLETED' || application.status === 'SCORING_APPROVED') && offers.length > 0 && !offers.some(o => o.selected) && (
            <span className="hint-text">Выберите подходящее предложение ниже</span>
          )}
          {application.status === 'OFFER_SELECTED' && (
            <button className="btn btn-primary" onClick={handleRequestDocuments} disabled={actionLoading}>
              {actionLoading ? 'Запрос...' : 'Запросить документы'}
            </button>
          )}
        </div>
      </div>

      {/* Application Info */}
      <div className="card">
        <h3>Данные заявки</h3>
        <div className="info-grid">
          <div className="info-item">
            <span className="info-label">ФИО</span>
            <span className="info-value">{application.lastName} {application.firstName} {application.middleName || ''}</span>
          </div>
          <div className="info-item">
            <span className="info-label">Email</span>
            <span className="info-value">{application.email}</span>
          </div>
          <div className="info-item">
            <span className="info-label">Дата рождения</span>
            <span className="info-value">{formatDate(application.birthDate)}</span>
          </div>
          <div className="info-item">
            <span className="info-label">Паспорт</span>
            <span className="info-value">{application.passportSeries} {application.passportNumber}</span>
          </div>
          <div className="info-item">
            <span className="info-label">Сумма</span>
            <span className="info-value">{formatMoney(application.amount)}</span>
          </div>
          <div className="info-item">
            <span className="info-label">Срок</span>
            <span className="info-value">{application.termMonths} мес.</span>
          </div>
          <div className="info-item">
            <span className="info-label">Создана</span>
            <span className="info-value">{formatDate(application.createdAt)}</span>
          </div>
        </div>
      </div>

      {/* Scoring Result */}
      {scoringResult && (
        <div className={`card ${scoringResult.scoringDecision === 'APPROVED' ? 'card-success' : scoringResult.scoringDecision === 'REJECTED' ? 'card-danger' : ''}`}>
          <h3>Результат скоринга</h3>
          <div className="info-grid">
            <div className="info-item">
              <span className="info-label">Решение</span>
              <span className={`info-value ${scoringResult.scoringDecision === 'APPROVED' ? 'text-success' : 'text-danger'}`}>
                {scoringResult.scoringDecision === 'APPROVED' ? 'Одобрено' : 'Отклонено'}
              </span>
            </div>
            {scoringResult.approvedRate !== null && (
              <div className="info-item">
                <span className="info-label">Одобренная ставка</span>
                <span className="info-value">{scoringResult.approvedRate}%</span>
              </div>
            )}
            {scoringResult.approvedAmount !== null && (
              <div className="info-item">
                <span className="info-label">Одобренная сумма</span>
                <span className="info-value">{formatMoney(scoringResult.approvedAmount)}</span>
              </div>
            )}
            {scoringResult.monthlyPayment !== null && (
              <div className="info-item">
                <span className="info-label">Ежемесячный платёж</span>
                <span className="info-value">{formatMoney(scoringResult.monthlyPayment)}</span>
              </div>
            )}
            {scoringResult.riskGrade && (
              <div className="info-item">
                <span className="info-label">Класс риска</span>
                <span className="info-value">{scoringResult.riskGrade}</span>
              </div>
            )}
            {scoringResult.rejectionReasons.length > 0 && (
              <div className="info-item info-item-full">
                <span className="info-label">Причины отказа</span>
                <ul className="rejection-list">
                  {scoringResult.rejectionReasons.map((reason, i) => (
                    <li key={i}>{reason}</li>
                  ))}
                </ul>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Offers */}
      {offers.length > 0 && (
        <div className="card">
          <h3>Кредитные предложения</h3>
          <div className="offers-grid">
            {offers.map((offer) => (
              <div key={offer.offerId} className={`offer-card ${offer.selected ? 'offer-selected' : ''}`}>
                <div className="offer-header">
                  <span className="offer-rate">{offer.rate}%</span>
                  <span className="offer-label">годовых</span>
                  {offer.selected && <span className="selected-badge">Выбран</span>}
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
                {!offer.selected && (application.status === 'SCORING_COMPLETED' || application.status === 'SCORING_APPROVED' || application.status === 'DRAFT') && (
                  <button
                    className="btn btn-primary btn-full"
                    onClick={() => handleSelectOffer(offer.offerId)}
                    disabled={actionLoading}
                  >
                    Выбрать
                  </button>
                )}
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Documents */}
      {documents.length > 0 && (
        <div className="card">
          <h3>Документы</h3>
          <div className="documents-list">
            {documents.map((doc) => (
              <div key={doc.documentId} className="document-item">
                <div className="document-info">
                  <span className="document-name">{doc.fileName}</span>
                  <span className="document-meta">{doc.format} &middot; {doc.documentType} &middot; {formatDate(doc.generatedAt)}</span>
                </div>
                <a
                  href={ApiService.getDocumentDownloadUrl(doc.documentId)}
                  className="btn btn-secondary btn-sm"
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  Скачать
                </a>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};

export default ApplicationDetails;
