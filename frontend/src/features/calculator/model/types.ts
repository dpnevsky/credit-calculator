export interface PaymentRow {
  month: number;
  payment: number;
  principal: number;
  interest: number;
  balance: number;
}

export interface CalculationResult {
  monthlyPayment: number;
  totalPayment: number;
  overpayment: number;
  paymentSchedule: PaymentRow[];
}

export interface LoanCalculationInput {
  amount: number;
  months: number;
  rate: number;
  paymentType: 'annuity' | 'differentiated';
}
