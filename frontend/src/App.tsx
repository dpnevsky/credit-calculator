import React, { useState } from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { AuthProvider } from './context/AuthProvider';
import Navbar from './components/Navbar';
import PrivateRoute from './components/PrivateRoute';
import Login from './pages/Login';
import Register from './pages/Register';
import Profile from './pages/Profile';
import CreateApplication from './pages/CreateApplication';
import ApplicationsList from './pages/ApplicationsList';
import ApplicationDetails from './pages/ApplicationDetails';
import ContractPage from './pages/ContractPage';
import LoanForm from './components/LoanForm';
import Result from './components/Result';
import './App.css';

interface CalculationResult {
  monthlyPayment: number;
  totalPayment: number;
  overpayment: number;
  paymentSchedule: PaymentRow[];
}

interface PaymentRow {
  month: number;
  payment: number;
  principal: number;
  interest: number;
  balance: number;
}

const CalculatorPage: React.FC = () => {
  const [result, setResult] = useState<CalculationResult | null>(null);

  const buildPaymentSchedule = (
    amount: number,
    months: number,
    monthlyRate: number,
    paymentType: 'annuity' | 'differentiated',
  ): PaymentRow[] => {
    const rows: PaymentRow[] = [];
    let balance = amount;

    if (paymentType === 'annuity') {
      const annuityPayment =
        monthlyRate === 0
          ? amount / months
          : amount * (monthlyRate / (1 - (1 + monthlyRate) ** (-months)));

      for (let month = 1; month <= months; month += 1) {
        const interest = balance * monthlyRate;
        const principal = Math.min(balance, annuityPayment - interest);
        balance = Math.max(0, balance - principal);

        rows.push({
          month,
          payment: principal + interest,
          principal,
          interest,
          balance,
        });
      }

      return rows;
    }

    const principalPart = amount / months;

    for (let month = 1; month <= months; month += 1) {
      const interest = balance * monthlyRate;
      const principal = Math.min(balance, principalPart);
      const payment = principal + interest;
      balance = Math.max(0, balance - principal);

      rows.push({
        month,
        payment,
        principal,
        interest,
        balance,
      });
    }

    return rows;
  };

  const calculate = (data: {
    amount: number;
    months: number;
    rate: number;
    paymentType: 'annuity' | 'differentiated';
  }) => {
    const { amount, months, rate, paymentType } = data;
    const monthlyRate = rate / 100 / 12;
    const paymentSchedule = buildPaymentSchedule(amount, months, monthlyRate, paymentType);

    if (paymentType === 'annuity') {
      const i = monthlyRate;
      const n = months;
      const monthlyPayment =
        i === 0 ? amount / n : amount * ((i * Math.pow(1 + i, n)) / (Math.pow(1 + i, n) - 1));
      const totalPayment = monthlyPayment * n;
      const overpayment = totalPayment - amount;
      setResult({ monthlyPayment, totalPayment, overpayment, paymentSchedule });
    } else {
      const avgMonthlyPayment = amount / months + (amount * monthlyRate * (months + 1)) / (2 * months);
      const totalPayment = amount + (amount * monthlyRate * (months + 1)) / 2;
      const overpayment = totalPayment - amount;
      setResult({
        monthlyPayment: avgMonthlyPayment,
        totalPayment,
        overpayment,
        paymentSchedule,
      });
    }
  };

  const clearResult = () => {
    setResult(null);
  };

  return (
    <div className="calculator-page">
      <div className="calculator-hero">
        <h1>Кредитный калькулятор</h1>
        <p className="subtitle">Рассчитайте ежемесячный платёж и переплату по кредиту</p>
      </div>
      <div className="calculator-content">
        <LoanForm onCalculate={calculate} onClear={clearResult} />
        {result && <Result {...result} />}
      </div>
    </div>
  );
};

function App() {
  return (
    <Router>
      <AuthProvider>
        <Navbar />
        <main className="main-content">
          <Routes>
            {/* Публичные маршруты — доступны без аутентификации */}
            <Route path="/" element={<CalculatorPage />} />
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />

            {/* Защищённые маршруты — требуют аутентификации */}
            <Route path="/applications" element={<PrivateRoute><ApplicationsList /></PrivateRoute>} />
            <Route path="/applications/new" element={<PrivateRoute><CreateApplication /></PrivateRoute>} />
            <Route path="/applications/:applicationId" element={<PrivateRoute><ApplicationDetails /></PrivateRoute>} />
            <Route path="/applications/:applicationId/contract" element={<PrivateRoute><ContractPage /></PrivateRoute>} />
            <Route path="/profile" element={<PrivateRoute><Profile /></PrivateRoute>} />
          </Routes>
        </main>
      </AuthProvider>
    </Router>
  );
}

export default App;
