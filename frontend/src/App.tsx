import React, { useState } from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { AuthProvider } from './context/AuthProvider';
import Navbar from './components/Navbar';
import PrivateRoute from './components/PrivateRoute';
import Login from './pages/Login';
import Register from './pages/Register';
import Profile from './pages/Profile';
import LoanForm from './components/LoanForm';
import Result from './components/Result';
import './App.css';

interface CalculationResult {
  monthlyPayment: number;
  totalPayment: number;
  overpayment: number;
}

// Главная страница с калькулятором
const CalculatorPage: React.FC = () => {
  const [result, setResult] = useState<CalculationResult | null>(null);

  const mockCalculate = (data: {
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
    <div className="App">
      <h1>Кредитный калькулятор</h1>
      <LoanForm onCalculate={mockCalculate} />
      {result && <Result {...result} />}
    </div>
  );
};

function App() {
  return (
    <Router>
      <AuthProvider>
        <Navbar />
        <Routes>
          <Route path="/" element={<CalculatorPage />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route
            path="/profile"
            element={
              <PrivateRoute>
                <Profile />
              </PrivateRoute>
            }
          />
        </Routes>
      </AuthProvider>
    </Router>
  );
}

export default App;