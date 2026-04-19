import type { CalculationResult, LoanCalculationInput, PaymentRow } from './types';

const buildPaymentSchedule = (
  amount: number,
  months: number,
  monthlyRate: number,
  paymentType: LoanCalculationInput['paymentType'],
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

export const calculateLoan = ({
  amount,
  months,
  rate,
  paymentType,
}: LoanCalculationInput): CalculationResult => {
  const monthlyRate = rate / 100 / 12;
  const paymentSchedule = buildPaymentSchedule(amount, months, monthlyRate, paymentType);

  if (paymentType === 'annuity') {
    const i = monthlyRate;
    const n = months;
    const monthlyPayment =
      i === 0 ? amount / n : amount * ((i * Math.pow(1 + i, n)) / (Math.pow(1 + i, n) - 1));
    const totalPayment = monthlyPayment * n;
    const overpayment = totalPayment - amount;

    return { monthlyPayment, totalPayment, overpayment, paymentSchedule };
  }

  const avgMonthlyPayment = amount / months + (amount * monthlyRate * (months + 1)) / (2 * months);
  const totalPayment = amount + (amount * monthlyRate * (months + 1)) / 2;
  const overpayment = totalPayment - amount;

  return {
    monthlyPayment: avgMonthlyPayment,
    totalPayment,
    overpayment,
    paymentSchedule,
  };
};
