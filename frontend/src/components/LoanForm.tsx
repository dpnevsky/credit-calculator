import React, { useState } from 'react';
import './LoanForm.css';

interface FormData {
  amount: number;
  months: number;
  rate: number;
  paymentType: 'annuity' | 'differentiated';
}

interface LoanFormProps {
  onCalculate: (data: FormData) => void;
  onClear: () => void;
}

const LoanForm: React.FC<LoanFormProps> = ({ onCalculate, onClear }) => {
  const [formData, setFormData] = useState<FormData>({
    amount: 1000000,
    months: 12,
    rate: 15.5,
    paymentType: 'annuity',
  });

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value, type } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: type === 'number' ? parseFloat(value) || 0 : value,
    }));
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onCalculate(formData);
  };

  const handleClear = () => {
    setFormData({
      amount: 0,
      months: 0,
      rate: 0,
      paymentType: 'annuity',
    });
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
      </div>

      <div className="loan-form-actions">
        <button type="submit" className="calc-btn calc-btn-primary">Рассчитать</button>
        <button type="button" className="calc-btn calc-btn-secondary" onClick={handleClear}>Очистить</button>
      </div>
    </form>
  );
};

export default LoanForm;
