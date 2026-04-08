import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import ApiService from '../services/api.service';
import type { CreateApplicationRequest, PreliminaryOffer, SubmitApplicationRequest } from '../types/api';
import { useAuth } from '../context/AuthContext';
import './Application.css';

const APPLICATION_FORM_DRAFT_KEY = 'cc_create_application_form_draft';
const SCORING_FORM_DRAFT_KEY = 'cc_create_application_scoring_draft';

const CreateApplication: React.FC = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [offers, setOffers] = useState<PreliminaryOffer[] | null>(null);
  const [applicationId, setApplicationId] = useState<string | null>(null);
  const [scoringForm, setScoringForm] = useState<SubmitApplicationRequest>({
    gender: 'MALE',
    passportIssueDate: '',
    passportIssueBranch: '',
    maritalStatus: 'SINGLE',
    dependentAmount: 0,
    employmentStatus: 'EMPLOYED',
    employerInn: '',
    salary: 50000,
    position: 'OTHER',
    workExperienceTotal: 18,
    workExperienceCurrent: 3,
    accountNumber: '',
    insuranceEnabled: false,
    salaryClient: false,
  });

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

  useEffect(() => {
    const savedFormDraft = localStorage.getItem(APPLICATION_FORM_DRAFT_KEY);
    const savedScoringDraft = localStorage.getItem(SCORING_FORM_DRAFT_KEY);

    if (savedFormDraft) {
      try {
        const parsed = JSON.parse(savedFormDraft) as Partial<CreateApplicationRequest>;
        setForm((prev) => ({ ...prev, ...parsed }));
      } catch (parseError) {
        console.warn('Невозможно прочитать черновик формы заявки', parseError);
      }
    }

    if (savedScoringDraft) {
      try {
        const parsed = JSON.parse(savedScoringDraft) as Partial<SubmitApplicationRequest>;
        setScoringForm((prev) => ({ ...prev, ...parsed }));
      } catch (parseError) {
        console.warn('Невозможно прочитать черновик формы скоринга', parseError);
      }
    }
  }, []);

  useEffect(() => {
    if (!user) {
      return;
    }
    setForm((prev) => ({
      ...prev,
      email: prev.email || user.email || '',
      firstName: prev.firstName || user.firstName || user.name || '',
      lastName: prev.lastName || user.lastName || '',
      middleName: prev.middleName || user.middleName || '',
    }));
  }, [user]);

  useEffect(() => {
    localStorage.setItem(APPLICATION_FORM_DRAFT_KEY, JSON.stringify(form));
  }, [form]);

  useEffect(() => {
    localStorage.setItem(SCORING_FORM_DRAFT_KEY, JSON.stringify(scoringForm));
  }, [scoringForm]);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value, type } = e.target;
    setForm(prev => ({
      ...prev,
      [name]: type === 'number' ? (value === '' ? '' : Number(value)) : value,
    }));
  };

  const handleScoringChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const target = e.target;
    const { name } = target;
    let value: string | number | boolean;

    if (target instanceof HTMLInputElement && target.type === 'checkbox') {
      value = target.checked;
    } else if (target instanceof HTMLInputElement && target.type === 'number') {
      value = target.value === '' ? 0 : Number(target.value);
    } else {
      value = target.value;
    }

    setScoringForm(prev => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    try {
      const result = await ApiService.createApplication(form);
      setApplicationId(result.applicationId);
      setOffers(result.offers);
      sessionStorage.setItem(`cc_submit_draft_${result.applicationId}`, JSON.stringify(scoringForm));
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
          <p className="hint-text">Выберите одно из 4 предложений на следующем шаге в карточке заявки.</p>
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

          <fieldset>
            <legend>Данные для скоринга</legend>
            <div className="form-row">
              <div className="form-field">
                <label htmlFor="gender">Пол</label>
                <select id="gender" name="gender" value={scoringForm.gender} onChange={handleScoringChange}>
                  <option value="MALE">Мужской</option>
                  <option value="FEMALE">Женский</option>
                  <option value="NON_BINARY">Другой</option>
                </select>
              </div>
              <div className="form-field">
                <label htmlFor="maritalStatus">Семейное положение</label>
                <select id="maritalStatus" name="maritalStatus" value={scoringForm.maritalStatus} onChange={handleScoringChange}>
                  <option value="SINGLE">Не в браке</option>
                  <option value="MARRIED">В браке</option>
                  <option value="DIVORCED">Разведён(а)</option>
                  <option value="WIDOWED">Вдовец/Вдова</option>
                </select>
              </div>
              <div className="form-field">
                <label htmlFor="dependentAmount">Иждивенцы</label>
                <input type="number" id="dependentAmount" name="dependentAmount" value={scoringForm.dependentAmount} onChange={handleScoringChange} min={0} />
              </div>
            </div>
            <div className="form-row">
              <div className="form-field">
                <label htmlFor="passportIssueDate">Дата выдачи паспорта</label>
                <input type="date" id="passportIssueDate" name="passportIssueDate" value={scoringForm.passportIssueDate} onChange={handleScoringChange} required />
              </div>
              <div className="form-field">
                <label htmlFor="passportIssueBranch">Код подразделения</label>
                <input type="text" id="passportIssueBranch" name="passportIssueBranch" value={scoringForm.passportIssueBranch} onChange={handleScoringChange} required />
              </div>
            </div>
            <div className="form-row">
              <div className="form-field">
                <label htmlFor="employmentStatus">Статус занятости</label>
                <select id="employmentStatus" name="employmentStatus" value={scoringForm.employmentStatus} onChange={handleScoringChange}>
                  <option value="EMPLOYED">Работаю</option>
                  <option value="UNEMPLOYED">Не работаю</option>
                  <option value="SELF_EMPLOYED">Самозанятый</option>
                  <option value="RETIRED">Пенсионер</option>
                  <option value="BUSINESS_OWNER">Владелец бизнеса</option>
                  <option value="STUDENT">Студент</option>
                </select>
              </div>
              <div className="form-field">
                <label htmlFor="position">Должность</label>
                <select id="position" name="position" value={scoringForm.position} onChange={handleScoringChange}>
                  <option value="TOP_MANAGER">Топ-менеджер</option>
                  <option value="MID_MANAGER">Менеджер</option>
                  <option value="JUNIOR_MANAGER">Младший менеджер</option>
                  <option value="DEVELOPER">Разработчик</option>
                  <option value="SALES">Продажи</option>
                  <option value="ACCOUNTANT">Бухгалтер</option>
                  <option value="HR">HR</option>
                  <option value="OTHER">Другое</option>
                </select>
              </div>
            </div>
            <div className="form-row">
              <div className="form-field">
                <label htmlFor="employerInn">ИНН работодателя</label>
                <input type="text" id="employerInn" name="employerInn" value={scoringForm.employerInn} onChange={handleScoringChange} maxLength={12} required />
              </div>
              <div className="form-field">
                <label htmlFor="salary">Зарплата (₽)</label>
                <input type="number" id="salary" name="salary" value={scoringForm.salary} onChange={handleScoringChange} min={0} step={1000} required />
              </div>
              <div className="form-field">
                <label htmlFor="accountNumber">Номер счёта</label>
                <input type="text" id="accountNumber" name="accountNumber" value={scoringForm.accountNumber} onChange={handleScoringChange} maxLength={20} required />
              </div>
            </div>
            <div className="form-row">
              <div className="form-field">
                <label htmlFor="workExperienceTotal">Общий стаж (мес.)</label>
                <input type="number" id="workExperienceTotal" name="workExperienceTotal" value={scoringForm.workExperienceTotal} onChange={handleScoringChange} min={0} required />
              </div>
              <div className="form-field">
                <label htmlFor="workExperienceCurrent">Текущий стаж (мес.)</label>
                <input type="number" id="workExperienceCurrent" name="workExperienceCurrent" value={scoringForm.workExperienceCurrent} onChange={handleScoringChange} min={0} required />
              </div>
            </div>
            <div className="form-row checkbox-row">
              <label className="checkbox-label">
                <input type="checkbox" name="insuranceEnabled" checked={scoringForm.insuranceEnabled} onChange={handleScoringChange} />
                Страхование жизни
              </label>
              <label className="checkbox-label">
                <input type="checkbox" name="salaryClient" checked={scoringForm.salaryClient} onChange={handleScoringChange} />
                Зарплатный клиент
              </label>
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
