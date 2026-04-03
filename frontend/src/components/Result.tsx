import React from 'react';
import './Result.css';

interface ResultProps {
  monthlyPayment?: number;
  totalPayment?: number;
  overpayment?: number;
}

const formatMoney = (value: number) =>
  new Intl.NumberFormat('ru-RU', { style: 'currency', currency: 'RUB', maximumFractionDigits: 2 }).format(value);

const Result: React.FC<ResultProps> = ({
  monthlyPayment,
  totalPayment,
  overpayment,
}) => {
  if (monthlyPayment === undefined) return null;

  return (
    <div className="result-card">
      <h3 className="result-title">Результаты расчёта</h3>
      <div className="result-grid">
        <div className="result-item result-highlight">
          <span className="result-label">Ежемесячный платёж</span>
          <span className="result-value">{formatMoney(monthlyPayment)}</span>
        </div>
        <div className="result-item">
          <span className="result-label">Общая сумма выплат</span>
          <span className="result-value">{totalPayment !== undefined ? formatMoney(totalPayment) : '—'}</span>
        </div>
        <div className="result-item">
          <span className="result-label">Переплата</span>
          <span className="result-value result-overpayment">{overpayment !== undefined ? formatMoney(overpayment) : '—'}</span>
        </div>
      </div>
    </div>
  );
};

export default Result;
