import React, { useState } from 'react';
import LoanForm from './components/LoanForm';
import Result from './components/Result';
import './App.css';

// Тип для результата расчёта
interface CalculationResult {
  monthlyPayment: number;
  totalPayment: number;
  overpayment: number;
}

function App() {
  const [result, setResult] = useState<CalculationResult | null>(null);

  // Временная функция расчёта (позже заменим на вызов бэкенда)
  const mockCalculate = (data: {
    amount: number;
    months: number;
    rate: number;
    paymentType: 'annuity' | 'differentiated';
  }) => {
    const { amount, months, rate, paymentType } = data;
    const monthlyRate = rate / 100 / 12; // месячная процентная ставка

    if (paymentType === 'annuity') {
      // Аннуитетный платёж: P = S * (i * (1 + i)^n) / ((1 + i)^n - 1)
      const i = monthlyRate;
      const n = months; // для краткости, можно оставить и так
      const coefficient = (i * Math.pow(1 + i, n)) / (Math.pow(1 + i, n) - 1);
      const monthlyPayment = amount * coefficient;
      const totalPayment = monthlyPayment * n;
      const overpayment = totalPayment - amount;
      setResult({ monthlyPayment, totalPayment, overpayment });
    } else {
      // Дифференцированный платёж (упрощённый расчёт для демонстрации интерфейса)
      // В реальном проекте лучше делать точный расчёт на бэкенде
      const avgMonthlyPayment = amount / months + (amount * monthlyRate * (months + 1)) / (2 * months);
      const totalPayment = amount + (amount * monthlyRate * (months + 1)) / 2;
      const overpayment = totalPayment - amount;
      setResult({
        monthlyPayment: avgMonthlyPayment,
        totalPayment,
        overpayment,
      });
    }
  };

  return (
    <div className="App">
      <h1>Кредитный калькулятор</h1>
      <LoanForm onCalculate={mockCalculate} />
      {result && <Result {...result} />}
    </div>
  );
}

export default App;