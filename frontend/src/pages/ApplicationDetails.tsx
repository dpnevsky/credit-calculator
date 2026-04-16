import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import ApiService from '../services/api.service';
import type {
  ApplicationResponse,
  DocumentResponse,
  OfferResponse,
  ScoringResultResponse,
  SubmitApplicationRequest,
} from '../types/api';
import {
  getApplicationRejectionReasons,
  getApplicationStatusColor,
  getApplicationStatusLabel,
  isRejectedApplicationStatus,
} from '../utils/applicationStatus';
import './Application.css';

type PaymentRow = {
  month: number;
  payment: number;
  principal: number;
  interest: number;
  balance: number;
};

const ACCOUNT_NUMBER_LENGTH = 20;

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
  const [submitDraft, setSubmitDraft] = useState<Partial<SubmitApplicationRequest> | undefined>(undefined);
  const [submittedScoring, setSubmittedScoring] = useState<Partial<SubmitApplicationRequest> | null>(null);
  const [previewOfferId, setPreviewOfferId] = useState<string | null>(null);
  const [paymentType, setPaymentType] = useState<'ANNUITY' | 'DIFFERENTIAL'>('ANNUITY');
  const [isPaymentTypeHydrated, setIsPaymentTypeHydrated] = useState(false);
  const [salaryAccountNumber, setSalaryAccountNumber] = useState('');

  const loadData = useCallback(async () => {
    if (!applicationId) {
      return;
    }

    try {
      const app = await ApiService.getApplication(applicationId);
      setApplication(app);

      try {
        const scoring = await ApiService.getScoringResult(applicationId);
        setScoringResult(scoring);
      } catch {
        setScoringResult(null);
      }

      try {
        const offersData = await ApiService.getOffers(applicationId);
        setOffers(offersData);
      } catch {
        setOffers([]);
      }

      try {
        let docs = await ApiService.getDocuments(applicationId);
        if (app.status === 'OFFER_SELECTED' && docs.length === 0) {
          try {
            await ApiService.requestDocuments(applicationId);
            docs = await ApiService.getDocuments(applicationId);
          } catch {
            // Documents may be generated asynchronously.
          }
        }
        setDocuments(docs);
      } catch {
        setDocuments([]);
      }
    } catch (loadError: unknown) {
      if (loadError instanceof Error) {
        setError(loadError.message);
      } else {
        setError('Ошибка загрузки заявки');
      }
    } finally {
      setLoading(false);
    }
  }, [applicationId]);

  useEffect(() => {
    void loadData();
  }, [loadData]);

  useEffect(() => {
    if (!applicationId) {
      return;
    }

    const draft = sessionStorage.getItem(`cc_submit_draft_${applicationId}`);
    if (!draft) {
      return;
    }

    try {
      setSubmitDraft(JSON.parse(draft) as Partial<SubmitApplicationRequest>);
    } catch {
      setSubmitDraft(undefined);
    }
  }, [applicationId]);

  useEffect(() => {
    if (!applicationId) {
      return;
    }

    const submittedRaw = localStorage.getItem(`cc_submitted_scoring_${applicationId}`);
    if (!submittedRaw) {
      setSubmittedScoring(null);
      return;
    }

    try {
      setSubmittedScoring(JSON.parse(submittedRaw) as Partial<SubmitApplicationRequest>);
    } catch {
      setSubmittedScoring(null);
    }
  }, [applicationId]);

  useEffect(() => {
    if (!applicationId) {
      return;
    }

    const paymentTypeRaw = localStorage.getItem(`cc_payment_type_${applicationId}`);
    if (paymentTypeRaw === 'DIFFERENTIAL' || paymentTypeRaw === 'ANNUITY') {
      setPaymentType(paymentTypeRaw);
    }

    setIsPaymentTypeHydrated(true);
  }, [applicationId]);

  useEffect(() => {
    if (!applicationId) {
      return;
    }

    const accountRaw = localStorage.getItem(`cc_salary_account_${applicationId}`);
    if (accountRaw !== null) {
      setSalaryAccountNumber(accountRaw.replace(/\D/g, '').slice(0, ACCOUNT_NUMBER_LENGTH));
      return;
    }

    const fallbackAccount = submittedScoring?.accountNumber ?? submitDraft?.accountNumber ?? '';
    setSalaryAccountNumber(fallbackAccount.replace(/\D/g, '').slice(0, ACCOUNT_NUMBER_LENGTH));
  }, [applicationId, submitDraft, submittedScoring]);

  useEffect(() => {
    if (!applicationId || !isPaymentTypeHydrated) {
      return;
    }

    localStorage.setItem(`cc_payment_type_${applicationId}`, paymentType);
  }, [applicationId, isPaymentTypeHydrated, paymentType]);

  useEffect(() => {
    if (!applicationId) {
      return;
    }

    localStorage.setItem(`cc_salary_account_${applicationId}`, salaryAccountNumber);
  }, [applicationId, salaryAccountNumber]);

  useEffect(() => {
    if (!offers.length) {
      return;
    }

    const selected = offers.find((offer) => offer.selected);
    setPreviewOfferId((prev) => prev ?? selected?.offerId ?? offers[0].offerId);
  }, [offers]);

  const normalizedSalaryAccountNumber = salaryAccountNumber.replace(/\D/g, '').slice(0, ACCOUNT_NUMBER_LENGTH);
  const isSalaryAccountNumberValid = normalizedSalaryAccountNumber.length === ACCOUNT_NUMBER_LENGTH;

  const handleSelectOffer = async (offerId: string) => {
    if (!applicationId) {
      return;
    }

    const selected = offers.find((offer) => offer.offerId === offerId);
    if (!selected) {
      return;
    }

    if (selected.salaryClient && !isSalaryAccountNumberValid) {
      setError(`Введите ровно ${ACCOUNT_NUMBER_LENGTH} цифр банковского счёта для зарплатного предложения.`);
      return;
    }

    const isConfirmed = window.confirm(
      `Подтвердите выбор предложения ${selected.rate}% на ${selected.termMonths} мес.`,
    );
    if (!isConfirmed) {
      return;
    }

    setActionLoading(true);
    try {
      await ApiService.selectOffer(applicationId, offerId);

      try {
        await ApiService.requestDocuments(applicationId, { paymentType });
      } catch {
        // Generation may take time; refreshed on next reload.
      }

      await loadData();
    } catch (selectError: unknown) {
      if (selectError instanceof Error) {
        setError(selectError.message);
      }
    } finally {
      setActionLoading(false);
    }
  };

  const formatMoney = (value: number) =>
    new Intl.NumberFormat('ru-RU', { style: 'currency', currency: 'RUB', maximumFractionDigits: 0 }).format(value);

  const formatDate = (dateStr: string) => {
    const date = new Date(dateStr);
    return date.toLocaleDateString('ru-RU', { day: '2-digit', month: '2-digit', year: 'numeric' });
  };

  const getGenderLabel = (value?: SubmitApplicationRequest['gender']) => {
    switch (value) {
      case 'MALE':
        return 'Мужской';
      case 'FEMALE':
        return 'Женский';
      case 'NON_BINARY':
        return 'Другой';
      default:
        return 'Не указано';
    }
  };

  const getMaritalStatusLabel = (value?: SubmitApplicationRequest['maritalStatus']) => {
    switch (value) {
      case 'SINGLE':
        return 'Не в браке';
      case 'MARRIED':
        return 'В браке';
      case 'DIVORCED':
        return 'Разведён(а)';
      case 'WIDOWED':
        return 'Вдовец/Вдова';
      default:
        return 'Не указано';
    }
  };

  const getEmploymentStatusLabel = (value?: SubmitApplicationRequest['employmentStatus']) => {
    switch (value) {
      case 'EMPLOYED':
        return 'Работаю';
      case 'UNEMPLOYED':
        return 'Не работаю';
      case 'SELF_EMPLOYED':
        return 'Самозанятый';
      case 'RETIRED':
        return 'Пенсионер';
      case 'BUSINESS_OWNER':
        return 'Владелец бизнеса';
      case 'STUDENT':
        return 'Студент';
      default:
        return 'Не указано';
    }
  };

  const getPositionLabel = (value?: SubmitApplicationRequest['position']) => {
    switch (value) {
      case 'TOP_MANAGER':
        return 'Топ-менеджер';
      case 'MID_MANAGER':
        return 'Менеджер';
      case 'JUNIOR_MANAGER':
        return 'Младший менеджер';
      case 'DEVELOPER':
        return 'Разработчик';
      case 'SALES':
        return 'Продажи';
      case 'ACCOUNTANT':
        return 'Бухгалтер';
      case 'HR':
        return 'HR';
      case 'OTHER':
        return 'Другое';
      default:
        return 'Не указано';
    }
  };

  const formatBoolean = (value?: boolean) => {
    if (value === undefined) {
      return 'Не указано';
    }

    return value ? 'Да' : 'Нет';
  };

  const getPaymentTypeLabel = (type: 'ANNUITY' | 'DIFFERENTIAL') =>
    type === 'ANNUITY' ? 'Аннуитетный' : 'Дифференцированный';

  const getInsuranceAmount = (offer: OfferResponse) =>
    offer.insuranceEnabled ? Math.max(0, offer.totalAmount - offer.requestedAmount) : 0;

  const buildPaymentSchedule = (offer: OfferResponse, scheduleType: 'ANNUITY' | 'DIFFERENTIAL'): PaymentRow[] => {
    const rows: PaymentRow[] = [];
    const principal = offer.totalAmount;
    const months = offer.termMonths;
    const monthlyRate = offer.rate / 12 / 100;
    let balance = principal;

    if (scheduleType === 'ANNUITY') {
      const annuityPayment =
        monthlyRate === 0
          ? principal / months
          : principal * (monthlyRate / (1 - (1 + monthlyRate) ** (-months)));

      for (let month = 1; month <= months; month += 1) {
        const interest = balance * monthlyRate;
        const principalPart = annuityPayment - interest;
        balance = Math.max(0, balance - principalPart);
        rows.push({
          month,
          payment: annuityPayment,
          principal: principalPart,
          interest,
          balance,
        });
      }

      return rows;
    }

    const principalPart = principal / months;
    for (let month = 1; month <= months; month += 1) {
      const interest = balance * monthlyRate;
      const payment = principalPart + interest;
      balance = Math.max(0, balance - principalPart);
      rows.push({
        month,
        payment,
        principal: principalPart,
        interest,
        balance,
      });
    }

    return rows;
  };

  const getContractDocument = (docs: DocumentResponse[]) =>
    docs.find((doc) => doc.format === 'PDF' && doc.documentType === 'CREDIT_AGREEMENT') ??
    docs.find((doc) => doc.format === 'PDF') ??
    null;

  const previewOffer = offers.find((offer) => offer.offerId === previewOfferId) ?? null;
  const selectedOffer = offers.find((offer) => offer.selected) ?? null;
  const previewSchedule = previewOffer ? buildPaymentSchedule(previewOffer, paymentType) : [];
  const contractDocument = getContractDocument(documents);
  const scoringProfile = submittedScoring ?? submitDraft ?? null;
  const isPaymentTypeLocked = selectedOffer !== null || application?.status === 'OFFER_SELECTED';
  const rejectionReasons = getApplicationRejectionReasons(
    application?.status ?? '',
    scoringResult?.rejectionReasons ?? [],
  );

  const downloadContractPdf = async () => {
    if (!applicationId || !contractDocument) {
      return;
    }

    setActionLoading(true);
    try {
      let documentToDownload = contractDocument;

      try {
        await ApiService.requestDocuments(applicationId, { paymentType });
        const refreshedDocuments = await ApiService.getDocuments(applicationId);
        setDocuments(refreshedDocuments);
        documentToDownload = getContractDocument(refreshedDocuments) ?? documentToDownload;
      } catch {
        // If regeneration fails, use the latest available PDF.
      }

      const { blob, fileName } = await ApiService.downloadDocument(documentToDownload.documentId);
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = fileName || documentToDownload.fileName || `credit-contract-${documentToDownload.documentId}.pdf`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(url);
    } catch (downloadError: unknown) {
      if (downloadError instanceof Error) {
        setError(downloadError.message);
      } else {
        setError('Не удалось скачать документ');
      }
    } finally {
      setActionLoading(false);
    }
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
          <h2>Заявка #{applicationId?.slice(0, 8)}</h2>
          <span
            className="status-badge"
            style={{ backgroundColor: getApplicationStatusColor(application.status, application.applicationId) }}
          >
            {getApplicationStatusLabel(application.status, application.applicationId)}
          </span>
        </div>
        <div className="header-actions">
          {(application.status === 'SCORING_COMPLETED' || application.status === 'SCORING_APPROVED') &&
            offers.length > 0 &&
            selectedOffer === null && (
              <span className="hint-text">Выберите подходящее предложение ниже</span>
            )}
        </div>
      </div>

      {isRejectedApplicationStatus(application.status, application.applicationId) && rejectionReasons.length > 0 && (
        <div className="card card-danger">
          <h3>Причина отказа</h3>
          <ul className="rejection-list">
            {rejectionReasons.map((reason) => (
              <li key={reason}>{reason}</li>
            ))}
          </ul>
        </div>
      )}

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
          {scoringProfile && (
            <>
              <div className="info-item">
                <span className="info-label">Пол</span>
                <span className="info-value">{getGenderLabel(scoringProfile.gender)}</span>
              </div>
              <div className="info-item">
                <span className="info-label">Семейное положение</span>
                <span className="info-value">{getMaritalStatusLabel(scoringProfile.maritalStatus)}</span>
              </div>
              <div className="info-item">
                <span className="info-label">Иждивенцы</span>
                <span className="info-value">{scoringProfile.dependentAmount ?? 'Не указано'}</span>
              </div>
              <div className="info-item">
                <span className="info-label">Дата выдачи паспорта</span>
                <span className="info-value">
                  {scoringProfile.passportIssueDate ? formatDate(scoringProfile.passportIssueDate) : 'Не указано'}
                </span>
              </div>
              <div className="info-item">
                <span className="info-label">Код подразделения</span>
                <span className="info-value">{scoringProfile.passportIssueBranch || 'Не указано'}</span>
              </div>
              <div className="info-item">
                <span className="info-label">Статус занятости</span>
                <span className="info-value">{getEmploymentStatusLabel(scoringProfile.employmentStatus)}</span>
              </div>
              <div className="info-item">
                <span className="info-label">Должность</span>
                <span className="info-value">{getPositionLabel(scoringProfile.position)}</span>
              </div>
              <div className="info-item">
                <span className="info-label">ИНН работодателя</span>
                <span className="info-value">{scoringProfile.employerInn || 'Не указано'}</span>
              </div>
              <div className="info-item">
                <span className="info-label">Зарплата</span>
                <span className="info-value">
                  {scoringProfile.salary ? formatMoney(scoringProfile.salary) : 'Не указано'}
                </span>
              </div>
              <div className="info-item">
                <span className="info-label">Общий стаж</span>
                <span className="info-value">
                  {scoringProfile.workExperienceTotal !== undefined ? `${scoringProfile.workExperienceTotal} мес.` : 'Не указано'}
                </span>
              </div>
              <div className="info-item">
                <span className="info-label">Текущий стаж</span>
                <span className="info-value">
                  {scoringProfile.workExperienceCurrent !== undefined ? `${scoringProfile.workExperienceCurrent} мес.` : 'Не указано'}
                </span>
              </div>
              {selectedOffer && (
                <div className="info-item">
                  <span className="info-label">Страхование жизни</span>
                  <span className="info-value">{formatBoolean(selectedOffer.insuranceEnabled)}</span>
                </div>
              )}
              {selectedOffer && (
                <div className="info-item">
                  <span className="info-label">Зарплатный клиент</span>
                  <span className="info-value">{formatBoolean(selectedOffer.salaryClient)}</span>
                </div>
              )}
              {selectedOffer?.salaryClient && (
                <div className="info-item">
                  <span className="info-label">Номер счёта</span>
                  <span className="info-value">{normalizedSalaryAccountNumber || scoringProfile.accountNumber || 'Не указано'}</span>
                </div>
              )}
              <div className="info-item">
                <span className="info-label">Тип платежа</span>
                <span className="info-value">{getPaymentTypeLabel(paymentType)}</span>
              </div>
            </>
          )}
        </div>
      </div>

      {scoringResult && (
        <div
          className={`card ${
            scoringResult.scoringDecision === 'APPROVED'
              ? 'card-success'
              : scoringResult.scoringDecision === 'REJECTED'
                ? 'card-danger'
                : ''
          }`}
        >
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
          </div>
        </div>
      )}

      {offers.length > 0 && (
        <div className="card">
          <h3>Кредитные предложения</h3>
          <div className="form-row">
            <div className="form-field">
              <label htmlFor="paymentType">Вид платежа</label>
              <select
                id="paymentType"
                value={paymentType}
                onChange={(event) => setPaymentType(event.target.value as 'ANNUITY' | 'DIFFERENTIAL')}
                disabled={isPaymentTypeLocked}
              >
                <option value="ANNUITY">Аннуитетный</option>
                <option value="DIFFERENTIAL">Дифференцированный</option>
              </select>
            </div>
          </div>
          <div className="form-row">
            <div className="form-field">
              <label htmlFor="salaryAccountNumber">Банковский счёт зарплатного клиента</label>
              <input
                type="text"
                id="salaryAccountNumber"
                value={salaryAccountNumber}
                onChange={(event) => {
                  const sanitizedValue = event.target.value.replace(/\D/g, '').slice(0, ACCOUNT_NUMBER_LENGTH);
                  setSalaryAccountNumber(sanitizedValue);
                }}
                maxLength={ACCOUNT_NUMBER_LENGTH}
                inputMode="numeric"
                pattern="\d{20}"
                disabled={selectedOffer !== null}
              />
            </div>
          </div>
          {!isSalaryAccountNumberValid && normalizedSalaryAccountNumber.length > 0 && (
            <div className="hint-text" style={{ marginBottom: '12px' }}>
              Введите ровно 20 цифр банковского счёта.
            </div>
          )}
          <div className="offers-grid">
            {offers.map((offer) => {
              const salaryOfferBlocked = offer.salaryClient && !isSalaryAccountNumberValid;

              return (
                <div
                  key={offer.offerId}
                  className={`offer-card ${offer.selected ? 'offer-selected' : ''} ${previewOfferId === offer.offerId ? 'offer-preview' : ''}`}
                  onClick={() => setPreviewOfferId(offer.offerId)}
                >
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
                      <span>Страховка</span>
                      <strong>{formatMoney(getInsuranceAmount(offer))}</strong>
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
                  {!offer.selected &&
                    (application.status === 'SCORING_COMPLETED' ||
                      application.status === 'SCORING_APPROVED' ||
                      application.status === 'DRAFT') && (
                      <>
                        {salaryOfferBlocked && (
                          <div className="hint-text" style={{ marginBottom: '10px' }}>
                            Для зарплатного предложения введите 20 цифр банковского счёта.
                          </div>
                        )}
                        <button
                          className="btn btn-primary btn-full"
                          onClick={(event) => {
                            event.stopPropagation();
                            void handleSelectOffer(offer.offerId);
                          }}
                          disabled={actionLoading || salaryOfferBlocked}
                        >
                          Выбрать
                        </button>
                      </>
                    )}
                </div>
              );
            })}
          </div>
          {previewOffer && (
            <div className="card" style={{ marginTop: '16px', marginBottom: 0 }}>
              <h3>График платежей: {getPaymentTypeLabel(paymentType)}</h3>
              <div className="schedule-table-wrapper">
                <table className="schedule-table">
                  <thead>
                    <tr>
                      <th>Месяц</th>
                      <th>Платёж</th>
                      <th>Проценты</th>
                      <th>Тело кредита</th>
                      <th>Остаток</th>
                    </tr>
                  </thead>
                  <tbody>
                    {previewSchedule.map((row) => (
                      <tr key={row.month}>
                        <td>{row.month}</td>
                        <td>{formatMoney(row.payment)}</td>
                        <td>{formatMoney(row.interest)}</td>
                        <td>{formatMoney(row.principal)}</td>
                        <td>{formatMoney(row.balance)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </div>
      )}

      {contractDocument && (
        <div className="card">
          <h3>Документы</h3>
          <div className="contract-actions">
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => navigate(`/applications/${application.applicationId}/contract`)}
            >
              Ознакомиться с договором
            </button>
            <button type="button" className="btn btn-primary" onClick={() => void downloadContractPdf()} disabled={actionLoading}>
              Скачать договор
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

export default ApplicationDetails;
