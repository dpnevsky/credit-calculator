import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import ApiService from '../services/api.service';
import type { CreateApplicationRequest, SubmitApplicationRequest } from '../types/api';
import { useAuth } from '../context/AuthContext';
import './Application.css';

const APPLICATION_FORM_DRAFT_KEY = 'cc_create_application_form_draft';
const SCORING_FORM_DRAFT_KEY = 'cc_create_application_scoring_draft';

const getDefaultPassportIssueDate = (): string => {
  const date = new Date();
  date.setDate(date.getDate() - 1);
  return date.toISOString().slice(0, 10);
};

const CreateApplication: React.FC = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [scoringForm, setScoringForm] = useState<SubmitApplicationRequest>({
    gender: 'MALE',
    passportIssueDate: getDefaultPassportIssueDate(),
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

  const isEmployerInnRequired = scoringForm.employmentStatus !== 'UNEMPLOYED';

  useEffect(() => {
    const savedFormDraft = localStorage.getItem(APPLICATION_FORM_DRAFT_KEY);
    const savedScoringDraft = localStorage.getItem(SCORING_FORM_DRAFT_KEY);

    if (savedFormDraft) {
      try {
        const parsed = JSON.parse(savedFormDraft) as Partial<CreateApplicationRequest>;
        setForm((prev) => ({ ...prev, ...parsed }));
      } catch (parseError) {
        console.warn('Не удалось прочитать черновик формы заявки', parseError);
      }
    }

    if (savedScoringDraft) {
      try {
        const parsed = JSON.parse(savedScoringDraft) as Partial<SubmitApplicationRequest>;
        setScoringForm((prev) => ({ ...prev, ...parsed }));
      } catch (parseError) {
        console.warn('Не удалось прочитать черновик формы скоринга', parseError);
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
      birthDate: prev.birthDate || user.birthDate || '',
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
    setForm((prev) => ({
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

    setScoringForm((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    try {
      const createdApplication = await ApiService.createApplication(form);

      if (createdApplication.status !== 'PRESCORING_REJECTED') {
        await ApiService.submitApplication(createdApplication.applicationId, {
          ...scoringForm,
          insuranceEnabled: false,
          salaryClient: false,
          accountNumber: '',
        });
      }

      localStorage.removeItem(APPLICATION_FORM_DRAFT_KEY);
      localStorage.removeItem(SCORING_FORM_DRAFT_KEY);
      navigate(`/applications/${createdApplication.applicationId}`);
    } catch (submitError: unknown) {
      if (submitError instanceof Error) {
        setError(submitError.message);
      } else {
        setError('Ошибка при создании или отправке заявки');
      }
    } finally {
      setLoading(false);
    }
  };

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
                <label htmlFor="amount">Сумма кредита (₽)</label>
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
            </div>
            <div className="form-row">
              <div className="form-field">
                <label htmlFor="dependentAmount">Иждивенцы</label>
                <input type="number" id="dependentAmount" name="dependentAmount" value={scoringForm.dependentAmount} onChange={handleScoringChange} min={0} />
              </div>
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
                <input
                  type="text"
                  id="employerInn"
                  name="employerInn"
                  value={scoringForm.employerInn}
                  onChange={handleScoringChange}
                  maxLength={12}
                  required={isEmployerInnRequired}
                  disabled={!isEmployerInnRequired}
                />
              </div>
              <div className="form-field">
                <label htmlFor="salary">Зарплата (₽)</label>
                <input type="number" id="salary" name="salary" value={scoringForm.salary} onChange={handleScoringChange} min={0} step={1000} required />
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
