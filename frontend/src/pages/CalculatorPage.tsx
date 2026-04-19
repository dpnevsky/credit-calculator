import { useState, type FC } from 'react';
import LoanForm from '../components/LoanForm';
import Result from '../components/Result';
import { calculateLoan } from '../features/calculator/model/calculateLoan';
import type { CalculationResult, LoanCalculationInput } from '../features/calculator/model/types';

const CalculatorPage: FC = () => {
  const [result, setResult] = useState<CalculationResult | null>(null);

  const handleCalculate = (data: LoanCalculationInput) => {
    setResult(calculateLoan(data));
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
        <LoanForm onCalculate={handleCalculate} onClear={clearResult} />
        {result && <Result {...result} />}
      </div>
    </div>
  );
};

export default CalculatorPage;
