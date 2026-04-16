import React from 'react';
import './Result.css';

interface ResultProps {
  monthlyPayment?: number;
  totalPayment?: number;
  overpayment?: number;
  paymentSchedule?: Array<{
    month: number;
    payment: number;
    principal: number;
    interest: number;
    balance: number;
  }>;
}

const formatMoney = (value: number) =>
  new Intl.NumberFormat('ru-RU', { style: 'currency', currency: 'RUB', maximumFractionDigits: 2 }).format(value);

const Result: React.FC<ResultProps> = ({
  monthlyPayment,
  totalPayment,
  overpayment,
  paymentSchedule,
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
      {paymentSchedule && paymentSchedule.length > 0 && (
        <div className="result-schedule">
          <h4 className="result-schedule-title">График платежей по месяцам</h4>
          <div className="result-table-wrapper">
            <table className="result-table">
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
                {paymentSchedule.map((row) => (
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
  );
};

export default Result;
