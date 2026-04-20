import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import ApiService from '../services/api.service';
import type {
  ApplicationResponse,
  DocumentResponse,
  OfferResponse,
  PaymentType,
  ScoringResultResponse,
} from '../types/api';
import {
  getApplicationRejectionReasons,
  getApplicationStatusColor,
  getApplicationStatusLabel,
  isRejectedApplicationStatus,
} from '../utils/applicationStatus';
import {
  ACCOUNT_NUMBER_LENGTH,
  buildPaymentSchedule,
  downloadApplicationDocumentFile,
  formatBoolean,
  formatDate,
  formatMoney,
  getContractDocument,
  getEmploymentStatusLabel,
  getGenderLabel,
  getInsuranceAmount,
  getMaritalStatusLabel,
  getPaymentTypeLabel,
  getPositionLabel,
  loadApplicationDetailsData,
  normalizeSalaryAccountNumber,
  refreshDocumentsAfterRequest,
} from './applicationDetails.helpers';
import './Application.css';

const SALARY_ACCOUNT_STORAGE_KEY = 'cc_salary_account_';

type ApplicationDetailsState = {
  application: ApplicationResponse | null;
  scoringResult: ScoringResultResponse | null;
  offers: OfferResponse[];
  documents: DocumentResponse[];
};

const INITIAL_STATE: ApplicationDetailsState = {
  application: null,
  scoringResult: null,
  offers: [],
  documents: [],
};

const ApplicationDetails: React.FC = () => {
  const { applicationId } = useParams<{ applicationId: string }>();
  const navigate = useNavigate();
  const [state, setState] = useState(INITIAL_STATE);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [actionLoading, setActionLoading] = useState(false);
  const [previewOfferId, setPreviewOfferId] = useState<string | null>(null);
  const [paymentType, setPaymentType] = useState<PaymentType>('ANNUITY');
  const [salaryAccountNumber, setSalaryAccountNumber] = useState('');

  const { application, scoringResult, offers, documents } = state;

  const loadData = useCallback(async () => {
    if (!applicationId) {
      return;
    }

    setError(null);

    try {
      const data = await loadApplicationDetailsData(applicationId);
      setState(data);
    } catch (loadError) {
      if (loadError instanceof Error) {
        setError(loadError.message);
      } else {
        setError('РћС€РёР±РєР° Р·Р°РіСЂСѓР·РєРё Р·Р°СЏРІРєРё');
      }
    } finally {
      setLoading(false);
    }
  }, [applicationId]);

  useEffect(() => {
    void loadData();
  }, [loadData]);

  useEffect(() => {
    if (!application) {
      return;
    }

    setPaymentType(application.paymentType);
  }, [application]);

  useEffect(() => {
    if (!applicationId || !application) {
      return;
    }

    const accountRaw = localStorage.getItem(`${SALARY_ACCOUNT_STORAGE_KEY}${applicationId}`);
    if (accountRaw !== null) {
      setSalaryAccountNumber(normalizeSalaryAccountNumber(accountRaw));
      return;
    }

    const backendAccount = application.submitData?.accountNumber ?? '';
    setSalaryAccountNumber(normalizeSalaryAccountNumber(backendAccount));
  }, [applicationId, application]);

  useEffect(() => {
    if (!applicationId) {
      return;
    }

    localStorage.setItem(`${SALARY_ACCOUNT_STORAGE_KEY}${applicationId}`, salaryAccountNumber);
  }, [applicationId, salaryAccountNumber]);

  useEffect(() => {
    if (!offers.length) {
      setPreviewOfferId(null);
      return;
    }

    const selected = offers.find((offer) => offer.selected);
    const previewStillExists = previewOfferId && offers.some((offer) => offer.offerId === previewOfferId);
    if (!previewStillExists) {
      setPreviewOfferId(selected?.offerId ?? offers[0].offerId);
    }
  }, [offers, previewOfferId]);

  const normalizedSalaryAccountNumber = normalizeSalaryAccountNumber(salaryAccountNumber);
  const isSalaryAccountNumberValid = normalizedSalaryAccountNumber.length === ACCOUNT_NUMBER_LENGTH;
  const previewOffer = offers.find((offer) => offer.offerId === previewOfferId) ?? null;
  const selectedOffer = offers.find((offer) => offer.selected) ?? null;
  const previewSchedule = previewOffer ? buildPaymentSchedule(previewOffer, paymentType) : [];
  const contractDocument = getContractDocument(documents);
  const scoringProfile = application?.submitData ?? null;
  const canRequestDocuments =
    application?.status === 'OFFER_SELECTED' ||
    application?.status === 'DOCUMENTS_REQUESTED' ||
    application?.status === 'DOCUMENTS_READY';
  const isPaymentTypeLocked =
    selectedOffer !== null ||
    application?.status === 'OFFER_SELECTED' ||
    application?.status === 'DOCUMENTS_REQUESTED' ||
    application?.status === 'DOCUMENTS_READY';
  const rejectionReasons = getApplicationRejectionReasons(
    application?.status ?? '',
    scoringResult?.rejectionReasons ?? [],
  );

  const handleSelectOffer = async (offerId: string) => {
    if (!applicationId) {
      return;
    }

    const selected = offers.find((offer) => offer.offerId === offerId);
    if (!selected) {
      return;
    }

    if (selected.salaryClient && !isSalaryAccountNumberValid) {
      setError(`Р’РІРµРґРёС‚Рµ СЂРѕРІРЅРѕ ${ACCOUNT_NUMBER_LENGTH} С†РёС„СЂ Р±Р°РЅРєРѕРІСЃРєРѕРіРѕ СЃС‡С‘С‚Р° РґР»СЏ Р·Р°СЂРїР»Р°С‚РЅРѕРіРѕ РїСЂРµРґР»РѕР¶РµРЅРёСЏ.`);
      return;
    }

    const isConfirmed = window.confirm(
      `РџРѕРґС‚РІРµСЂРґРёС‚Рµ РІС‹Р±РѕСЂ РїСЂРµРґР»РѕР¶РµРЅРёСЏ ${selected.rate}% РЅР° ${selected.termMonths} РјРµСЃ.`,
    );
    if (!isConfirmed) {
      return;
    }

    setActionLoading(true);
    setError(null);

    try {
      await ApiService.selectOffer(applicationId, offerId, { paymentType });
      await loadData();
    } catch (selectError) {
      if (selectError instanceof Error) {
        setError(selectError.message);
      } else {
        setError('РќРµ СѓРґР°Р»РѕСЃСЊ РІС‹Р±СЂР°С‚СЊ РїСЂРµРґР»РѕР¶РµРЅРёРµ');
      }
    } finally {
      setActionLoading(false);
    }
  };

  const handleRequestDocuments = async () => {
    if (!applicationId) {
      return;
    }

    setActionLoading(true);
    setError(null);

    try {
      await refreshDocumentsAfterRequest(applicationId);
      await loadData();
    } catch (requestError) {
      if (requestError instanceof Error) {
        setError(requestError.message);
      } else {
        setError('РќРµ СѓРґР°Р»РѕСЃСЊ Р·Р°РїСЂРѕСЃРёС‚СЊ РґРѕРєСѓРјРµРЅС‚С‹');
      }
    } finally {
      setActionLoading(false);
    }
  };

  const handleDownloadContractPdf = async () => {
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
        setError('РќРµ СѓРґР°Р»РѕСЃСЊ СЃРєР°С‡Р°С‚СЊ РґРѕРіРѕРІРѕСЂ');
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
          <p>Р—Р°РіСЂСѓР·РєР°...</p>
        </div>
      </div>
    );
  }

  if (error || !application) {
    return (
      <div className="page-container">
        <div className="card error-card">
          <h2>РћС€РёР±РєР°</h2>
          <p>{error || 'Р—Р°СЏРІРєР° РЅРµ РЅР°Р№РґРµРЅР°'}</p>
          <button className="btn btn-primary" onClick={() => navigate('/applications')}>
            Рљ СЃРїРёСЃРєСѓ Р·Р°СЏРІРѕРє
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="page-container">
      <div className="detail-header">
        <div>
          <h2>Р—Р°СЏРІРєР° #{applicationId?.slice(0, 8)}</h2>
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
              <span className="hint-text">Р’С‹Р±РµСЂРёС‚Рµ РїРѕРґС…РѕРґСЏС‰РµРµ РїСЂРµРґР»РѕР¶РµРЅРёРµ РЅРёР¶Рµ</span>
            )}
        </div>
      </div>

      {isRejectedApplicationStatus(application.status, application.applicationId) && rejectionReasons.length > 0 && (
        <div className="card card-danger">
          <h3>РџСЂРёС‡РёРЅР° РѕС‚РєР°Р·Р°</h3>
          <ul className="rejection-list">
            {rejectionReasons.map((reason) => (
              <li key={reason}>{reason}</li>
            ))}
          </ul>
        </div>
      )}

      <div className="card">
        <h3>Р”Р°РЅРЅС‹Рµ Р·Р°СЏРІРєРё</h3>
        <div className="info-grid">
          <div className="info-item">
            <span className="info-label">Р¤РРћ</span>
            <span className="info-value">
              {application.lastName} {application.firstName} {application.middleName || ''}
            </span>
          </div>
          <div className="info-item">
            <span className="info-label">Email</span>
            <span className="info-value">{application.email}</span>
          </div>
          <div className="info-item">
            <span className="info-label">Р”Р°С‚Р° СЂРѕР¶РґРµРЅРёСЏ</span>
            <span className="info-value">{formatDate(application.birthDate)}</span>
          </div>
          <div className="info-item">
            <span className="info-label">РџР°СЃРїРѕСЂС‚</span>
            <span className="info-value">
              {application.passportSeries} {application.passportNumber}
            </span>
          </div>
          <div className="info-item">
            <span className="info-label">РЎСѓРјРјР°</span>
            <span className="info-value">{formatMoney(application.amount)}</span>
          </div>
          <div className="info-item">
            <span className="info-label">РЎСЂРѕРє</span>
            <span className="info-value">{application.termMonths} РјРµСЃ.</span>
          </div>
          <div className="info-item">
            <span className="info-label">РЎРѕР·РґР°РЅР°</span>
            <span className="info-value">{formatDate(application.createdAt)}</span>
          </div>
          {scoringProfile && (
            <>
              <div className="info-item">
                <span className="info-label">РџРѕР»</span>
                <span className="info-value">{getGenderLabel(scoringProfile.gender)}</span>
              </div>
              <div className="info-item">
                <span className="info-label">РЎРµРјРµР№РЅРѕРµ РїРѕР»РѕР¶РµРЅРёРµ</span>
                <span className="info-value">{getMaritalStatusLabel(scoringProfile.maritalStatus)}</span>
              </div>
              <div className="info-item">
                <span className="info-label">РР¶РґРёРІРµРЅС†С‹</span>
                <span className="info-value">{scoringProfile.dependentAmount}</span>
              </div>
              <div className="info-item">
                <span className="info-label">Р”Р°С‚Р° РІС‹РґР°С‡Рё РїР°СЃРїРѕСЂС‚Р°</span>
                <span className="info-value">{formatDate(scoringProfile.passportIssueDate)}</span>
              </div>
              <div className="info-item">
                <span className="info-label">РљРѕРґ РїРѕРґСЂР°Р·РґРµР»РµРЅРёСЏ</span>
                <span className="info-value">{scoringProfile.passportIssueBranch || 'РќРµ СѓРєР°Р·Р°РЅРѕ'}</span>
              </div>
              <div className="info-item">
                <span className="info-label">РЎС‚Р°С‚СѓСЃ Р·Р°РЅСЏС‚РѕСЃС‚Рё</span>
                <span className="info-value">{getEmploymentStatusLabel(scoringProfile.employmentStatus)}</span>
              </div>
              <div className="info-item">
                <span className="info-label">Р”РѕР»Р¶РЅРѕСЃС‚СЊ</span>
                <span className="info-value">{getPositionLabel(scoringProfile.position)}</span>
              </div>
              <div className="info-item">
                <span className="info-label">РРќРќ СЂР°Р±РѕС‚РѕРґР°С‚РµР»СЏ</span>
                <span className="info-value">{scoringProfile.employerInn || 'РќРµ СѓРєР°Р·Р°РЅРѕ'}</span>
              </div>
              <div className="info-item">
                <span className="info-label">Р—Р°СЂРїР»Р°С‚Р°</span>
                <span className="info-value">{formatMoney(scoringProfile.salary)}</span>
              </div>
              <div className="info-item">
                <span className="info-label">РћР±С‰РёР№ СЃС‚Р°Р¶</span>
                <span className="info-value">{scoringProfile.workExperienceTotal} РјРµСЃ.</span>
              </div>
              <div className="info-item">
                <span className="info-label">РўРµРєСѓС‰РёР№ СЃС‚Р°Р¶</span>
                <span className="info-value">{scoringProfile.workExperienceCurrent} РјРµСЃ.</span>
              </div>
              {selectedOffer && (
                <div className="info-item">
                  <span className="info-label">РЎС‚СЂР°С…РѕРІР°РЅРёРµ Р¶РёР·РЅРё</span>
                  <span className="info-value">{formatBoolean(selectedOffer.insuranceEnabled)}</span>
                </div>
              )}
              {selectedOffer && (
                <div className="info-item">
                  <span className="info-label">Р—Р°СЂРїР»Р°С‚РЅС‹Р№ РєР»РёРµРЅС‚</span>
                  <span className="info-value">{formatBoolean(selectedOffer.salaryClient)}</span>
                </div>
              )}
              {selectedOffer?.salaryClient && (
                <div className="info-item">
                  <span className="info-label">РќРѕРјРµСЂ СЃС‡С‘С‚Р°</span>
                  <span className="info-value">
                    {normalizedSalaryAccountNumber || scoringProfile.accountNumber || 'РќРµ СѓРєР°Р·Р°РЅРѕ'}
                  </span>
                </div>
              )}
              <div className="info-item">
                <span className="info-label">РўРёРї РїР»Р°С‚РµР¶Р°</span>
                <span className="info-value">{getPaymentTypeLabel(paymentType)}</span>
              </div>
            </>
          )}
        </div>
      </div>

      {scoringResult && (
        <div className="card scoring-result-card" style={{ borderLeftColor: getApplicationStatusColor(application.status, application.applicationId) }}>
          <h3>Р РµР·СѓР»СЊС‚Р°С‚ СЃРєРѕСЂРёРЅРіР°</h3>
          <div className="info-grid">
            <div className="info-item">
              <span className="info-label">Р РµС€РµРЅРёРµ</span>
              <span
                className="info-value scoring-result-status"
                style={{ color: getApplicationStatusColor(application.status, application.applicationId) }}
              >
                {getApplicationStatusLabel(application.status, application.applicationId)}
              </span>
            </div>
            <div className="info-item">
              <span className="info-label">РЎРєРѕСЂРёРЅРіРѕРІС‹Р№ Р±Р°Р»Р»</span>
              <span className="info-value">{scoringResult.scoreValue}</span>
            </div>
            {scoringResult.approvedRate !== null && (
              <div className="info-item">
                <span className="info-label">РћРґРѕР±СЂРµРЅРЅР°СЏ СЃС‚Р°РІРєР°</span>
                <span className="info-value">{scoringResult.approvedRate}%</span>
              </div>
            )}
            {scoringResult.approvedAmount !== null && (
              <div className="info-item">
                <span className="info-label">РћРґРѕР±СЂРµРЅРЅР°СЏ СЃСѓРјРјР°</span>
                <span className="info-value">{formatMoney(scoringResult.approvedAmount)}</span>
              </div>
            )}
            {scoringResult.approvedTermMonths !== null && (
              <div className="info-item">
                <span className="info-label">РћРґРѕР±СЂРµРЅРЅС‹Р№ СЃСЂРѕРє</span>
                <span className="info-value">{scoringResult.approvedTermMonths} РјРµСЃ.</span>
              </div>
            )}
            {scoringResult.riskGrade && (
              <div className="info-item">
                <span className="info-label">РљР»Р°СЃСЃ СЂРёСЃРєР°</span>
                <span className="info-value">{scoringResult.riskGrade}</span>
              </div>
            )}
          </div>
        </div>
      )}

      {offers.length > 0 && (
        <div className="card">
          <h3>РљСЂРµРґРёС‚РЅС‹Рµ РїСЂРµРґР»РѕР¶РµРЅРёСЏ</h3>
          <div className="form-row">
            <div className="form-field">
              <label htmlFor="paymentType">Р’РёРґ РїР»Р°С‚РµР¶Р°</label>
              <select
                id="paymentType"
                value={paymentType}
                onChange={(event) => setPaymentType(event.target.value as PaymentType)}
                disabled={isPaymentTypeLocked}
              >
                <option value="ANNUITY">РђРЅРЅСѓРёС‚РµС‚РЅС‹Р№</option>
                <option value="DIFFERENTIAL">Р”РёС„С„РµСЂРµРЅС†РёСЂРѕРІР°РЅРЅС‹Р№</option>
              </select>
            </div>
          </div>
          <div className="form-row">
            <div className="form-field">
              <label htmlFor="salaryAccountNumber">Р‘Р°РЅРєРѕРІСЃРєРёР№ СЃС‡С‘С‚ Р·Р°СЂРїР»Р°С‚РЅРѕРіРѕ РєР»РёРµРЅС‚Р°</label>
              <input
                type="text"
                id="salaryAccountNumber"
                value={salaryAccountNumber}
                onChange={(event) => setSalaryAccountNumber(normalizeSalaryAccountNumber(event.target.value))}
                maxLength={ACCOUNT_NUMBER_LENGTH}
                inputMode="numeric"
                pattern="\d{20}"
                disabled={selectedOffer !== null}
              />
            </div>
          </div>
          {!isSalaryAccountNumberValid && normalizedSalaryAccountNumber.length > 0 && (
            <div className="hint-text offer-warning-text">
              Р’РІРµРґРёС‚Рµ СЂРѕРІРЅРѕ 20 С†РёС„СЂ Р±Р°РЅРєРѕРІСЃРєРѕРіРѕ СЃС‡С‘С‚Р°.
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
                    <span className="offer-label">РіРѕРґРѕРІС‹С…</span>
                    {offer.selected && <span className="selected-badge">Р’С‹Р±СЂР°РЅ</span>}
                  </div>
                  <div className="offer-details">
                    <div className="offer-row">
                      <span>РЎСѓРјРјР° РєСЂРµРґРёС‚Р°</span>
                      <strong>{formatMoney(offer.totalAmount)}</strong>
                    </div>
                    <div className="offer-row">
                      <span>РЎС‚СЂР°С…РѕРІРєР°</span>
                      <strong>{formatMoney(getInsuranceAmount(offer))}</strong>
                    </div>
                    <div className="offer-row">
                      <span>Р•Р¶РµРјРµСЃСЏС‡РЅС‹Р№ РїР»Р°С‚С‘Р¶</span>
                      <strong>{formatMoney(offer.monthlyPayment)}</strong>
                    </div>
                    <div className="offer-row">
                      <span>РЎСЂРѕРє</span>
                      <strong>{offer.termMonths} РјРµСЃ.</strong>
                    </div>
                    <div className="offer-tags">
                      {offer.insuranceEnabled && <span className="tag tag-insurance">РЎС‚СЂР°С…РѕРІРєР°</span>}
                      {offer.salaryClient && <span className="tag tag-salary">Р—Р°СЂРїР»Р°С‚РЅС‹Р№ РєР»РёРµРЅС‚</span>}
                    </div>
                  </div>
                  {!offer.selected && selectedOffer === null && application.status === 'SCORING_COMPLETED' && (
                    <>
                      {salaryOfferBlocked && (
                        <div className="hint-text offer-warning-text">
                          Р”Р»СЏ Р·Р°СЂРїР»Р°С‚РЅРѕРіРѕ РїСЂРµРґР»РѕР¶РµРЅРёСЏ РІРІРµРґРёС‚Рµ 20 С†РёС„СЂ Р±Р°РЅРєРѕРІСЃРєРѕРіРѕ СЃС‡С‘С‚Р°.
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
                        Р’С‹Р±СЂР°С‚СЊ
                      </button>
                    </>
                  )}
                </div>
              );
            })}
          </div>

          {previewOffer && (
            <div className="card offer-preview-card">
              <h3>Р“СЂР°С„РёРє РїР»Р°С‚РµР¶РµР№: {getPaymentTypeLabel(paymentType)}</h3>
              <div className="schedule-table-wrapper">
                <table className="schedule-table">
                  <thead>
                    <tr>
                      <th>РњРµСЃСЏС†</th>
                      <th>РџР»Р°С‚С‘Р¶</th>
                      <th>РџСЂРѕС†РµРЅС‚С‹</th>
                      <th>РўРµР»Рѕ РєСЂРµРґРёС‚Р°</th>
                      <th>РћСЃС‚Р°С‚РѕРє</th>
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
          <h3>Р”РѕРєСѓРјРµРЅС‚С‹</h3>
          <div className="contract-actions">
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => navigate(`/applications/${application.applicationId}/contract`)}
            >
              РћР·РЅР°РєРѕРјРёС‚СЊСЃСЏ СЃ РґРѕРіРѕРІРѕСЂРѕРј
            </button>
            <button
              type="button"
              className="btn btn-primary"
              onClick={() => void handleDownloadContractPdf()}
              disabled={actionLoading}
            >
              РЎРєР°С‡Р°С‚СЊ РґРѕРіРѕРІРѕСЂ
            </button>
          </div>
        </div>
      )}

      {!contractDocument && canRequestDocuments && (
        <div className="card">
          <h3>Р”РѕРєСѓРјРµРЅС‚С‹</h3>
          <p className="hint-text">Р”РѕРєСѓРјРµРЅС‚С‹ РµС‰С‘ РЅРµ РіРѕС‚РѕРІС‹ РёР»Рё С‚СЂРµР±СѓСЋС‚ РїРѕРІС‚РѕСЂРЅРѕРіРѕ Р·Р°РїСЂРѕСЃР°.</p>
          <div className="contract-actions">
            <button
              type="button"
              className="btn btn-primary"
              onClick={() => void handleRequestDocuments()}
              disabled={actionLoading}
            >
              Р—Р°РїСЂРѕСЃРёС‚СЊ РґРѕРєСѓРјРµРЅС‚С‹
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

export default ApplicationDetails;
