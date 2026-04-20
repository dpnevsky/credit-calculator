import React, { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { isApplicationSigned, markApplicationAsSigned } from '../utils/applicationStatus';
import type { ContractPageData } from './applicationDetails.helpers';
import {
  downloadApplicationDocumentFile,
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
        const data = await loadContractPageData(applicationId);
        setState(data);
      } catch (loadError) {
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

  const application = state?.application ?? null;
  const documents = state?.documents ?? [];
  const offers = state?.offers ?? [];
  const selectedOffer = useMemo(() => getSelectedOffer(offers), [offers]);
  const contractDocument = useMemo(() => getContractDocument(documents), [documents]);
  const paymentType = application?.paymentType ?? 'ANNUITY';
  const contractTerms = application
    ? resolveContractTerms(application, selectedOffer)
    : null;

  const handleDownloadContract = async () => {
    if (!contractDocument) {
      return;
    }

    setActionLoading(true);
    setError(null);

    try {
      await downloadApplicationDocumentFile(contractDocument);
    } catch (downloadError) {
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

    // TODO: This is a temporary frontend-only contract signing stub.
    // Replace it with backend-driven signing truth once the real signing flow exists.
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

  if (error || !application || !contractTerms) {
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
          <p className="hint-text">
            Подписание пока работает как временный интерфейсный шаг без реальной отправки в backend.
          </p>
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
              {isSigned ? 'Подписано' : 'Ожидает подписи'}
            </span>
          </div>
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
            После нажатия на кнопку подписи интерфейс пометит договор как подписанный. Реальная
            интеграция электронной подписи пока не подключена.
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
          <button className="btn btn-primary" onClick={handleSignContract} disabled={isSigned}>
            {isSigned ? 'Договор подписан' : 'Подписать'}
          </button>
        </div>
      </div>
    </div>
  );
};

export default ContractPage;
