import { useState, type ChangeEvent, type FC, type FormEvent } from 'react';
import type { LoanCalculationInput } from '../features/calculator/model/types';
import './LoanForm.css';

interface LoanFormState {
  amount: string;
  months: string;
  rate: string;
  paymentType: LoanCalculationInput['paymentType'];
}

interface LoanFormProps {
  onCalculate: (data: LoanCalculationInput) => void;
  onClear: () => void;
}

const INITIAL_FORM_STATE: LoanFormState = {
  amount: '1000000',
  months: '12',
  rate: '15.5',
  paymentType: 'annuity',
};

const CLEARED_FORM_STATE: LoanFormState = {
  amount: '',
  months: '',
  rate: '',
  paymentType: 'annuity',
};

const LoanForm: FC<LoanFormProps> = ({ onCalculate, onClear }) => {
  const [formData, setFormData] = useState<LoanFormState>(INITIAL_FORM_STATE);

  const handleChange = (e: ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: value,
    }));
  };

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();

    if (formData.amount.trim() === '' || formData.months.trim() === '' || formData.rate.trim() === '') {
      return;
    }

    const amount = Number(formData.amount);
    const months = Number(formData.months);
    const rate = Number(formData.rate);

    if (
      !Number.isFinite(amount) ||
      !Number.isFinite(months) ||
      !Number.isFinite(rate) ||
      amount < 0 ||
      months < 1 ||
      rate < 0
    ) {
      return;
    }

    onCalculate({
      amount,
      months,
      rate,
      paymentType: formData.paymentType,
    });
  };

  const handleClear = () => {
    setFormData(CLEARED_FORM_STATE);
    onClear();
  };

  return (
    <form onSubmit={handleSubmit} className="loan-form">
      <div className="loan-form-field">
        <label htmlFor="amount">Сумма кредита (&#8381;)</label>
        <input
          type="number"
          id="amount"
          name="amount"
          value={formData.amount}
          onChange={handleChange}
          min={0}
          step={1000}
          required
        />
      </div>

      <div className="loan-form-row">
        <div className="loan-form-field">
          <label htmlFor="months">Срок (мес.)</label>
          <input
            type="number"
            id="months"
            name="months"
            value={formData.months}
            onChange={handleChange}
            min={1}
            required
          />
        </div>
        <div className="loan-form-field">
          <label htmlFor="rate">Ставка (% годовых)</label>
          <input
            type="number"
            id="rate"
            name="rate"
            value={formData.rate}
            onChange={handleChange}
            min={0}
            step={0.1}
            required
          />
        </div>
      </div>

      <div className="loan-form-field">
        <label>Тип платежа</label>
        <div className="payment-type-group">
          <label className="radio-label">
            <input
              type="radio"
              name="paymentType"
              value="annuity"
              checked={formData.paymentType === 'annuity'}
              onChange={handleChange}
            />
            <span className="radio-text">Аннуитетный</span>
          </label>
          <label className="radio-label">
            <input
              type="radio"
              name="paymentType"
              value="differentiated"
              checked={formData.paymentType === 'differentiated'}
              onChange={handleChange}
            />
            <span className="radio-text">Дифференцированный</span>
          </label>
        </div>
        <div className="payment-type-description" aria-label="Описание типов платежей">
          <div className="payment-type-card">
            <h3>Аннуитетный платеж</h3>
            <p>Ежемесячный платеж остается примерно одинаковым весь срок кредита. Это удобно, если важен предсказуемый платеж каждый месяц.</p>
          </div>
          <div className="payment-type-card">
            <h3>Дифференцированный платеж</h3>
            <p>Платеж в начале выше, а затем постепенно уменьшается. Такой вариант часто снижает переплату, но требует большей нагрузки в первые месяцы.</p>
          </div>
        </div>
      </div>

      <div className="loan-form-actions">
        <button type="submit" className="calc-btn calc-btn-primary">Рассчитать</button>
        <button type="button" className="calc-btn calc-btn-secondary" onClick={handleClear}>Очистить</button>
      </div>
    </form>
  );
};

export default LoanForm;
