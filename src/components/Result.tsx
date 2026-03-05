import React from 'react';

interface ResultProps {
  monthlyPayment?: number;
  totalPayment?: number;
  overpayment?: number;
}

const Result: React.FC<ResultProps> = ({
  monthlyPayment,
  totalPayment,
  overpayment,
}) => {
  // Если результат ещё не получен, не показываем ничего
  if (monthlyPayment === undefined) return null;

  return (
    <div style={{ marginTop: '20px', padding: '15px', border: '1px solid #ccc' }}>
      <h3>Результаты расчёта</h3>
      <p>
        <strong>Ежемесячный платёж:</strong>{' '}
        {monthlyPayment.toFixed(2)} ₽
      </p>
      <p>
        <strong>Общая сумма выплат:</strong> {totalPayment?.toFixed(2)} ₽
      </p>
      <p>
        <strong>Переплата:</strong> {overpayment?.toFixed(2)} ₽
      </p>
    </div>
  );
};

export default Result;