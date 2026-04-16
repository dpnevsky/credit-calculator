import React, { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import ApiService from '../services/api.service';
import type { ApplicationResponse, DocumentResponse, OfferResponse } from '../types/api';
import { isApplicationSigned, markApplicationAsSigned } from '../utils/applicationStatus';
import './Application.css';

const ContractPage: React.FC = () => {
  const { applicationId } = useParams<{ applicationId: string }>();
  const navigate = useNavigate();

  const [application, setApplication] = useState<ApplicationResponse | null>(null);
  const [documents, setDocuments] = useState<DocumentResponse[]>([]);
  const [offers, setOffers] = useState<OfferResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [actionLoading, setActionLoading] = useState(false);
  const [isSigned, setIsSigned] = useState(() => isApplicationSigned(applicationId));

  useEffect(() => {
    setIsSigned(isApplicationSigned(applicationId));
  }, [applicationId]);

  useEffect(() => {
    const loadData = async () => {
      if (!applicationId) {
        setError('Заявка не найдена');
        setLoading(false);
        return;
      }

      setLoading(true);
      setError(null);

      try {
        const [applicationData, documentsData, offersData] = await Promise.all([
          ApiService.getApplication(applicationId),
          ApiService.getDocuments(applicationId).catch(() => []),
          ApiService.getOffers(applicationId).catch(() => []),
        ]);

        setApplication(applicationData);
        setDocuments(documentsData);
        setOffers(offersData);
      } catch (loadError: unknown) {
        if (loadError instanceof Error) {
          setError(loadError.message);
        } else {
          setError('Не удалось загрузить договор');
        }
      } finally {
        setLoading(false);
      }
    };

    void loadData();
  }, [applicationId]);

  const selectedOffer = useMemo(
    () => offers.find((offer) => offer.selected) ?? offers[0] ?? null,
    [offers],
  );

  const contractDocument = useMemo(
    () =>
      documents.find((doc) => doc.format === 'PDF' && doc.documentType === 'CREDIT_AGREEMENT') ??
      documents.find((doc) => doc.format === 'PDF') ??
      null,
    [documents],
  );

  const paymentType = (applicationId && localStorage.getItem(`cc_payment_type_${applicationId}`)) || 'ANNUITY';

  const formatMoney = (value: number) =>
    new Intl.NumberFormat('ru-RU', {
      style: 'currency',
      currency: 'RUB',
      maximumFractionDigits: 0,
    }).format(value);

  const paymentTypeLabel = paymentType === 'DIFFERENTIAL' ? 'Дифференцированный' : 'Аннуитетный';

  const handleDownloadContract = async () => {
    if (!contractDocument) {
      return;
    }

    setActionLoading(true);
    try {
      const { blob, fileName } = await ApiService.downloadDocument(contractDocument.documentId);
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = fileName || contractDocument.fileName || `credit-contract-${contractDocument.documentId}.pdf`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(url);
    } catch (downloadError: unknown) {
      if (downloadError instanceof Error) {
        setError(downloadError.message);
      } else {
        setError('Не удалось скачать договор');
      }
    } finally {
      setActionLoading(false);
    }
  };

  const handleSignContract = () => {
    if (!applicationId) {
      return;
    }

    markApplicationAsSigned(applicationId);
    setIsSigned(true);
  };

  if (loading) {
    return (
      <div className="page-container">
        <div className="loading-spinner">
          <div className="spinner"></div>
          <p>Загрузка договора...</p>
        </div>
      </div>
    );
  }

  if (error || !application) {
    return (
      <div className="page-container">
        <div className="card error-card">
          <h2>Ошибка</h2>
          <p>{error || 'Договор не найден'}</p>
          <button className="btn btn-primary" onClick={() => navigate('/applications')}>
            К списку заявок
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="page-container">
      <div className="detail-header">
        <div>
          <h2>Договор по заявке #{application.applicationId.slice(0, 8)}</h2>
          <p className="hint-text">Подписание пока работает как интерфейсный шаг без реальной отправки в бэкенд.</p>
        </div>
        <div className="header-actions">
          <button className="btn btn-secondary" onClick={() => navigate(`/applications/${application.applicationId}`)}>
            Назад к заявке
          </button>
          {contractDocument && (
            <button className="btn btn-primary" onClick={() => void handleDownloadContract()} disabled={actionLoading}>
              Скачать договор
            </button>
          )}
        </div>
      </div>

      <div className="card">
        <h3>Основные условия</h3>
        <div className="info-grid">
          <div className="info-item">
            <span className="info-label">Заёмщик</span>
            <span className="info-value">
              {application.lastName} {application.firstName} {application.middleName || ''}
            </span>
          </div>
          <div className="info-item">
            <span className="info-label">Сумма кредита</span>
            <span className="info-value">{formatMoney(selectedOffer?.totalAmount ?? application.amount)}</span>
          </div>
          <div className="info-item">
            <span className="info-label">Срок</span>
            <span className="info-value">{selectedOffer?.termMonths ?? application.termMonths} мес.</span>
          </div>
          <div className="info-item">
            <span className="info-label">Ставка</span>
            <span className="info-value">{selectedOffer ? `${selectedOffer.rate}%` : '—'}</span>
          </div>
          <div className="info-item">
            <span className="info-label">Тип платежа</span>
            <span className="info-value">{paymentTypeLabel}</span>
          </div>
          <div className="info-item">
            <span className="info-label">Статус договора</span>
            <span className={`info-value ${isSigned ? 'text-success' : ''}`}>
              {isSigned ? 'Подписано' : 'Ожидает подписи'}
            </span>
          </div>
        </div>
      </div>

      <div className="card">
        <h3>Текст договора</h3>
        <div className="contract-preview">
          <p>
            Настоящий договор подтверждает согласие клиента на оформление кредита по выбранным условиям
            и графику платежей, сформированному банком.
          </p>
          <p>
            После нажатия на кнопку подписи интерфейс пометит договор как подписанный. Реальная интеграция
            электронной подписи пока не подключена.
          </p>
          <p>Полную версию договора можно скачать в формате PDF.</p>
        </div>
        <div className="contract-actions">
          <button className="btn btn-primary" onClick={handleSignContract} disabled={isSigned}>
            {isSigned ? 'Договор подписан' : 'Подписать'}
          </button>
        </div>
      </div>
    </div>
  );
};

export default ContractPage;
