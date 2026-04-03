import React, { useState } from 'react';
import ApiService from '../services/api.service';
import type { SubmitApplicationRequest } from '../types/api';
import './SubmitApplicationModal.css';

interface Props {
  applicationId: string;
  onClose: () => void;
  onSuccess: () => void;
}

const SubmitApplicationModal: React.FC<Props> = ({ applicationId, onClose, onSuccess }) => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [form, setForm] = useState<SubmitApplicationRequest>({
    gender: 'MALE',
    passportIssueDate: '',
    passportIssueBranch: '',
    maritalStatus: 'SINGLE',
    dependentAmount: 0,
    employmentStatus: 'EMPLOYED',
    employerInn: '',
    salary: 50000,
    position: 'OTHER',
    workExperienceTotal: 12,
    workExperienceCurrent: 6,
    accountNumber: '',
    insuranceEnabled: false,
    salaryClient: false,
  });

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
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

    setForm(prev => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    try {
      await ApiService.submitApplication(applicationId, form);
      onSuccess();
    } catch (err: unknown) {
      if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('Ошибка при отправке заявки');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={e => e.stopPropagation()}>
        <div className="modal-header">
          <h3>Заполните анкету</h3>
          <button className="modal-close" onClick={onClose}>&times;</button>
        </div>

        {error && <div className="error-banner">{error}</div>}

        <form onSubmit={handleSubmit} className="modal-form">
          <fieldset>
            <legend>Личные данные</legend>
            <div className="form-row">
              <div className="form-field">
                <label htmlFor="gender">Пол</label>
                <select id="gender" name="gender" value={form.gender} onChange={handleChange}>
                  <option value="MALE">Мужской</option>
                  <option value="FEMALE">Женский</option>
                  <option value="NON_BINARY">Другой</option>
                </select>
              </div>
              <div className="form-field">
                <label htmlFor="maritalStatus">Семейное положение</label>
                <select id="maritalStatus" name="maritalStatus" value={form.maritalStatus} onChange={handleChange}>
                  <option value="SINGLE">Не в браке</option>
                  <option value="MARRIED">В браке</option>
                  <option value="DIVORCED">Разведён(а)</option>
                  <option value="WIDOWED">Вдовец/Вдова</option>
                </select>
              </div>
              <div className="form-field">
                <label htmlFor="dependentAmount">Иждивенцы</label>
                <input type="number" id="dependentAmount" name="dependentAmount" value={form.dependentAmount} onChange={handleChange} min={0} />
              </div>
            </div>
          </fieldset>

          <fieldset>
            <legend>Паспортные данные</legend>
            <div className="form-row">
              <div className="form-field">
                <label htmlFor="passportIssueDate">Дата выдачи</label>
                <input type="date" id="passportIssueDate" name="passportIssueDate" value={form.passportIssueDate} onChange={handleChange} required />
              </div>
              <div className="form-field">
                <label htmlFor="passportIssueBranch">Код подразделения</label>
                <input type="text" id="passportIssueBranch" name="passportIssueBranch" value={form.passportIssueBranch} onChange={handleChange} required />
              </div>
            </div>
          </fieldset>

          <fieldset>
            <legend>Занятость</legend>
            <div className="form-row">
              <div className="form-field">
                <label htmlFor="employmentStatus">Статус занятости</label>
                <select id="employmentStatus" name="employmentStatus" value={form.employmentStatus} onChange={handleChange}>
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
                <select id="position" name="position" value={form.position} onChange={handleChange}>
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
                <input type="text" id="employerInn" name="employerInn" value={form.employerInn} onChange={handleChange} maxLength={12} required />
              </div>
              <div className="form-field">
                <label htmlFor="salary">Зарплата (&#8381;)</label>
                <input type="number" id="salary" name="salary" value={form.salary} onChange={handleChange} min={0} step={1000} required />
              </div>
            </div>
            <div className="form-row">
              <div className="form-field">
                <label htmlFor="workExperienceTotal">Общий стаж (мес.)</label>
                <input type="number" id="workExperienceTotal" name="workExperienceTotal" value={form.workExperienceTotal} onChange={handleChange} min={0} required />
              </div>
              <div className="form-field">
                <label htmlFor="workExperienceCurrent">Текущий стаж (мес.)</label>
                <input type="number" id="workExperienceCurrent" name="workExperienceCurrent" value={form.workExperienceCurrent} onChange={handleChange} min={0} required />
              </div>
            </div>
          </fieldset>

          <fieldset>
            <legend>Банковские данные</legend>
            <div className="form-row">
              <div className="form-field">
                <label htmlFor="accountNumber">Номер счёта</label>
                <input type="text" id="accountNumber" name="accountNumber" value={form.accountNumber} onChange={handleChange} maxLength={20} required />
              </div>
            </div>
          </fieldset>

          <fieldset>
            <legend>Дополнительно</legend>
            <div className="form-row checkbox-row">
              <label className="checkbox-label">
                <input type="checkbox" name="insuranceEnabled" checked={form.insuranceEnabled} onChange={handleChange} />
                Страхование жизни
              </label>
              <label className="checkbox-label">
                <input type="checkbox" name="salaryClient" checked={form.salaryClient} onChange={handleChange} />
                Зарплатный клиент
              </label>
            </div>
          </fieldset>

          <div className="modal-actions">
            <button type="button" className="btn btn-secondary" onClick={onClose}>Отмена</button>
            <button type="submit" className="btn btn-primary" disabled={loading}>
              {loading ? 'Отправка...' : 'Отправить на скоринг'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default SubmitApplicationModal;
