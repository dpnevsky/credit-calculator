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
import LoanForm from './components/LoanForm';
import Result from './components/Result';
import './App.css';

interface CalculationResult {
  monthlyPayment: number;
  totalPayment: number;
  overpayment: number;
}

const CalculatorPage: React.FC = () => {
  const [result, setResult] = useState<CalculationResult | null>(null);

  const calculate = (data: {
    amount: number;
    months: number;
    rate: number;
    paymentType: 'annuity' | 'differentiated';
  }) => {
    const { amount, months, rate, paymentType } = data;
    const monthlyRate = rate / 100 / 12;

    if (paymentType === 'annuity') {
      const i = monthlyRate;
      const n = months;
      const coefficient = (i * Math.pow(1 + i, n)) / (Math.pow(1 + i, n) - 1);
      const monthlyPayment = amount * coefficient;
      const totalPayment = monthlyPayment * n;
      const overpayment = totalPayment - amount;
      setResult({ monthlyPayment, totalPayment, overpayment });
    } else {
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
    <div className="calculator-page">
      <div className="calculator-hero">
        <h1>Кредитный калькулятор</h1>
        <p className="subtitle">Рассчитайте ежемесячный платёж и переплату по кредиту</p>
      </div>
      <div className="calculator-content">
        <LoanForm onCalculate={calculate} />
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
            <Route path="/" element={<CalculatorPage />} />
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />
            <Route path="/applications" element={<ApplicationsList />} />
            <Route path="/applications/new" element={<CreateApplication />} />
            <Route path="/applications/:applicationId" element={<ApplicationDetails />} />
            <Route
              path="/profile"
              element={
                <PrivateRoute>
                  <Profile />
                </PrivateRoute>
              }
            />
          </Routes>
        </main>
      </AuthProvider>
    </Router>
  );
}

export default App;
