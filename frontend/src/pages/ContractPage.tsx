import axios from 'axios';
import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import ApiService from '../services/api.service';
import { getApplicationStatusLabel, getContractStatusLabel } from '../utils/applicationStatus';
import type { ContractPageData } from './applicationDetails.helpers';
import {
  downloadApplicationDocumentFile,
  formatDateTime,
  formatMoney,
  getContractDocument,
  getPaymentTypeLabel,
  getSelectedOffer,
  loadContractPageData,
  resolveContractTerms,
} from './applicationDetails.helpers';
import './Application.css';

const INITIAL_STATE: ContractPageData | null = null;

const ContractPage: React.FC = () => {
  const { applicationId } = useParams<{ applicationId: string }>();
  const navigate = useNavigate();

  const [state, setState] = useState<ContractPageData | null>(INITIAL_STATE);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [actionLoading, setActionLoading] = useState(false);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const loadData = useCallback(async () => {
    if (!applicationId) {
      setError('Заявка не найдена');
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const data = await loadContractPageData(applicationId);
      setState(data);
    } catch (loadError) {
      setError(extractErrorMessage(loadError, 'Не удалось загрузить договор'));
    } finally {
      setLoading(false);
    }
  }, [applicationId]);

  useEffect(() => {
    void loadData();
  }, [loadData]);

  const application = state?.application ?? null;
  const contract = state?.contract ?? null;
  const documents = state?.documents ?? [];
  const offers = state?.offers ?? [];
  const selectedOffer = getSelectedOffer(offers);
  const contractDocument = getContractDocument(documents);
  const paymentType = application?.paymentType ?? 'ANNUITY';
  const contractTerms = application
    ? resolveContractTerms(application, selectedOffer)
    : null;
  const isSigned = contract?.signed ?? false;
  const canSign = contract?.contractStatus === 'READY_TO_SIGN';
  const signDisabledMessage = getSignDisabledMessage(contract?.contractStatus ?? null);

  const handleDownloadContract = async () => {
    if (!contractDocument) {
      return;
    }

    setActionLoading(true);
    setError(null);

    try {
      await downloadApplicationDocumentFile(contractDocument);
    } catch (downloadError) {
      setError(extractErrorMessage(downloadError, 'Не удалось скачать договор'));
    } finally {
      setActionLoading(false);
    }
  };

  const handleSignContract = async () => {
    if (!applicationId) {
      return;
    }

    setActionLoading(true);
    setError(null);
    setSuccessMessage(null);

    try {
      await ApiService.signContract(applicationId);
      const data = await loadContractPageData(applicationId);
      setState(data);
      setSuccessMessage('Договор успешно подписан.');
    } catch (signError) {
      setError(extractErrorMessage(signError, 'Не удалось подписать договор'));
    } finally {
      setActionLoading(false);
    }
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

  if (error || !application || !contract || !contractTerms) {
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
          <p className="hint-text">Номер договора: {contract.contractNumber}</p>
        </div>
        <div className="header-actions">
          <button
            className="btn btn-secondary"
            onClick={() => navigate(`/applications/${application.applicationId}`)}
          >
            Назад к заявке
          </button>
          {contractDocument && (
            <button
              className="btn btn-primary"
              onClick={() => void handleDownloadContract()}
              disabled={actionLoading}
            >
              Скачать договор
            </button>
          )}
        </div>
      </div>

      {successMessage && (
        <div className="card card-success">
          <p>{successMessage}</p>
        </div>
      )}

      <div className="card">
        <h3>Основные условия</h3>
        <div className="info-grid">
          <div className="info-item">
            <span className="info-label">Заемщик</span>
            <span className="info-value">
              {application.lastName} {application.firstName} {application.middleName || ''}
            </span>
          </div>
          <div className="info-item">
            <span className="info-label">Сумма кредита</span>
            <span className="info-value">{formatMoney(contractTerms.amount)}</span>
          </div>
          <div className="info-item">
            <span className="info-label">Срок</span>
            <span className="info-value">{contractTerms.termMonths} мес.</span>
          </div>
          <div className="info-item">
            <span className="info-label">Ставка</span>
            <span className="info-value">
              {contractTerms.rate !== null ? `${contractTerms.rate}%` : 'Не указано'}
            </span>
          </div>
          <div className="info-item">
            <span className="info-label">Тип платежа</span>
            <span className="info-value">{getPaymentTypeLabel(paymentType)}</span>
          </div>
          <div className="info-item">
            <span className="info-label">Статус договора</span>
            <span className={`info-value ${isSigned ? 'text-success' : ''}`}>
              {getContractStatusLabel(contract.contractStatus)}
            </span>
          </div>
          <div className="info-item">
            <span className="info-label">Статус заявки</span>
            <span className="info-value">
              {getApplicationStatusLabel(contract.applicationStatus)}
            </span>
          </div>
          {contract.signedAt && (
            <div className="info-item">
              <span className="info-label">Подписан</span>
              <span className="info-value text-success">{formatDateTime(contract.signedAt)}</span>
            </div>
          )}
          {contract.signatureId && (
            <div className="info-item">
              <span className="info-label">Signature ID</span>
              <span className="info-value">{contract.signatureId}</span>
            </div>
          )}
        </div>
      </div>

      <div className="card">
        <h3>Текст договора</h3>
        <div className="contract-preview">
          <p>
            Настоящий договор подтверждает согласие клиента на оформление кредита по выбранным
            условиям и графику платежей, сформированным банком.
          </p>
          <p>
            После подписания backend сохраняет signed-статус договора, дату подписания и
            уникальный идентификатор подписи.
          </p>
          <p>Полную версию договора можно скачать в формате PDF.</p>
          {!selectedOffer && (
            <p className="hint-text">
              Детали договора показаны по данным заявки. Выбранный оффер для этой заявки не найден.
            </p>
          )}
          {!contractDocument && (
            <p className="hint-text">
              PDF-версия договора пока недоступна. Вернитесь к заявке и запросите документы.
            </p>
          )}
        </div>
        <div className="contract-actions">
          <button
            className="btn btn-primary"
            onClick={() => void handleSignContract()}
            disabled={!canSign || actionLoading}
          >
            {actionLoading ? 'Подписание...' : isSigned ? 'Подписано' : 'Подписать'}
          </button>
        </div>
        {!canSign && signDisabledMessage && (
          <p className="hint-text">{signDisabledMessage}</p>
        )}
      </div>
    </div>
  );
};

const extractErrorMessage = (error: unknown, fallback: string): string => {
  if (axios.isAxiosError(error)) {
    const responseMessage = error.response?.data?.message;
    if (typeof responseMessage === 'string' && responseMessage.length > 0) {
      return responseMessage;
    }
  }

  if (error instanceof Error) {
    return error.message;
  }

  return fallback;
};

const getSignDisabledMessage = (contractStatus: string | null): string | null => {
  switch (contractStatus) {
    case 'SIGNED':
      return 'Договор уже подписан.';
    case 'NOT_CREATED':
      return 'Договор ещё не сформирован. Сначала запросите документы на странице заявки.';
    case 'EXPIRED':
      return 'Срок подписания договора истёк.';
    case 'CANCELLED':
      return 'Подписание договора недоступно.';
    default:
      return null;
  }
};

export default ContractPage;
