import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import ApiService from '../services/api.service';
import { useAuth } from '../context/AuthContext';
import './Application.css';
import {
  APPLICATION_FORM_DRAFT_KEY,
  SCORING_FORM_DRAFT_KEY,
  type ApplicationFormState,
  type ScoringFormState,
  applyUserPrefill,
  buildCreateApplicationPayload,
  buildSubmitApplicationPayload,
  clearApplicationDrafts,
  createClearedApplicationFormState,
  createClearedScoringFormState,
  createInitialApplicationFormState,
  createInitialScoringFormState,
  readDraft,
  saveDraft,
} from './createApplication.helpers';

const CreateApplication: React.FC = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [form, setForm] = useState<ApplicationFormState>(createInitialApplicationFormState);
  const [scoringForm, setScoringForm] = useState<ScoringFormState>(createInitialScoringFormState);

  const isEmployerInnRequired = scoringForm.employmentStatus !== 'UNEMPLOYED';

  useEffect(() => {
    const applicationDraft = readDraft<ApplicationFormState>(
      APPLICATION_FORM_DRAFT_KEY,
      'Не удалось прочитать черновик формы заявки',
    );
    const scoringDraft = readDraft<ScoringFormState>(
      SCORING_FORM_DRAFT_KEY,
      'Не удалось прочитать черновик формы скоринга',
    );

    if (applicationDraft) {
      setForm((prev) => ({ ...prev, ...applicationDraft }));
    }

    if (scoringDraft) {
      setScoringForm((prev) => ({ ...prev, ...scoringDraft }));
    }
  }, []);

  useEffect(() => {
    if (!user) {
      return;
    }

    setForm((prev) => applyUserPrefill(prev, user));
  }, [user]);

  useEffect(() => {
    saveDraft(APPLICATION_FORM_DRAFT_KEY, form);
  }, [form]);

  useEffect(() => {
    saveDraft(SCORING_FORM_DRAFT_KEY, scoringForm);
  }, [scoringForm]);

  const handleApplicationChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = event.target;
    setForm((prev) => ({
      ...prev,
      [name]: value,
    }));
  };

  const handleScoringChange = (event: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const target = event.target;
    const { name } = target;

    setScoringForm((prev) => ({
      ...prev,
      [name]:
        target instanceof HTMLInputElement && target.type === 'checkbox'
          ? target.checked
          : target.value,
    }));
  };

  const handleClear = () => {
    setError(null);
    setForm(applyUserPrefill(createClearedApplicationFormState(), user));
    setScoringForm(createClearedScoringFormState());
  };

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    setLoading(true);
    setError(null);

    try {
      const createdApplication = await ApiService.createApplication(
        buildCreateApplicationPayload(form),
      );

      if (createdApplication.status !== 'PRESCORING_REJECTED') {
        await ApiService.submitApplication(
          createdApplication.applicationId,
          buildSubmitApplicationPayload(scoringForm),
        );
      }

      clearApplicationDrafts();
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
                <input
                  type="number"
                  id="amount"
                  name="amount"
                  value={form.amount}
                  onChange={handleApplicationChange}
                  min={20000}
                  step={10000}
                  required
                />
              </div>
              <div className="form-field">
                <label htmlFor="termMonths">Срок (месяцев)</label>
                <input
                  type="number"
                  id="termMonths"
                  name="termMonths"
                  value={form.termMonths}
                  onChange={handleApplicationChange}
                  min={6}
                  max={360}
                  required
                />
              </div>
            </div>
          </fieldset>

          <fieldset>
            <legend>Персональные данные</legend>
            <div className="form-row">
              <div className="form-field">
                <label htmlFor="lastName">Фамилия</label>
                <input type="text" id="lastName" name="lastName" value={form.lastName} onChange={handleApplicationChange} required />
              </div>
              <div className="form-field">
                <label htmlFor="firstName">Имя</label>
                <input type="text" id="firstName" name="firstName" value={form.firstName} onChange={handleApplicationChange} required />
              </div>
              <div className="form-field">
                <label htmlFor="middleName">Отчество</label>
                <input type="text" id="middleName" name="middleName" value={form.middleName} onChange={handleApplicationChange} />
              </div>
            </div>
            <div className="form-row">
              <div className="form-field">
                <label htmlFor="email">Email</label>
                <input type="email" id="email" name="email" value={form.email} onChange={handleApplicationChange} required />
              </div>
              <div className="form-field">
                <label htmlFor="birthDate">Дата рождения</label>
                <input type="date" id="birthDate" name="birthDate" value={form.birthDate} onChange={handleApplicationChange} required />
              </div>
            </div>
          </fieldset>

          <fieldset>
            <legend>Паспортные данные</legend>
            <div className="form-row">
              <div className="form-field">
                <label htmlFor="passportSeries">Серия</label>
                <input
                  type="text"
                  id="passportSeries"
                  name="passportSeries"
                  value={form.passportSeries}
                  onChange={handleApplicationChange}
                  maxLength={4}
                  pattern="\d{4}"
                  required
                />
              </div>
              <div className="form-field">
                <label htmlFor="passportNumber">Номер</label>
                <input
                  type="text"
                  id="passportNumber"
                  name="passportNumber"
                  value={form.passportNumber}
                  onChange={handleApplicationChange}
                  maxLength={6}
                  pattern="\d{6}"
                  required
                />
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
                <input
                  type="number"
                  id="dependentAmount"
                  name="dependentAmount"
                  value={scoringForm.dependentAmount}
                  onChange={handleScoringChange}
                  min={0}
                />
              </div>
              <div className="form-field">
                <label htmlFor="passportIssueDate">Дата выдачи паспорта</label>
                <input
                  type="date"
                  id="passportIssueDate"
                  name="passportIssueDate"
                  value={scoringForm.passportIssueDate}
                  onChange={handleScoringChange}
                  required
                />
              </div>
              <div className="form-field">
                <label htmlFor="passportIssueBranch">Код подразделения</label>
                <input
                  type="text"
                  id="passportIssueBranch"
                  name="passportIssueBranch"
                  value={scoringForm.passportIssueBranch}
                  onChange={handleScoringChange}
                  required
                />
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
                <input
                  type="number"
                  id="salary"
                  name="salary"
                  value={scoringForm.salary}
                  onChange={handleScoringChange}
                  min={0}
                  step={1000}
                  required
                />
              </div>
            </div>
            <div className="form-row">
              <div className="form-field">
                <label htmlFor="workExperienceTotal">Общий стаж (мес.)</label>
                <input
                  type="number"
                  id="workExperienceTotal"
                  name="workExperienceTotal"
                  value={scoringForm.workExperienceTotal}
                  onChange={handleScoringChange}
                  min={0}
                  required
                />
              </div>
              <div className="form-field">
                <label htmlFor="workExperienceCurrent">Текущий стаж (мес.)</label>
                <input
                  type="number"
                  id="workExperienceCurrent"
                  name="workExperienceCurrent"
                  value={scoringForm.workExperienceCurrent}
                  onChange={handleScoringChange}
                  min={0}
                  required
                />
              </div>
            </div>
          </fieldset>

          <div className="action-buttons">
            <button type="button" className="btn btn-secondary" onClick={handleClear} disabled={loading}>
              Очистить
            </button>
            <button type="submit" className="btn btn-primary" disabled={loading}>
              {loading ? 'Отправка...' : 'Создать заявку'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default CreateApplication;
