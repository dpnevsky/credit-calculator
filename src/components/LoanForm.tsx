import React, { useState } from 'react';

// Описываем типы данных, которые будут приходить из формы
interface FormData {
  amount: number;        // сумма кредита
  months: number;        // срок в месяцах
  rate: number;          // процентная ставка
  paymentType: 'annuity' | 'differentiated'; // тип платежа
}

// Описываем props, которые компонент получает от родителя
interface LoanFormProps {
  onCalculate: (data: FormData) => void; // функция, которую вызовем при сабмите
}

// Сам компонент
const LoanForm: React.FC<LoanFormProps> = ({ onCalculate }) => {
  // useState — это хук, который хранит состояние формы
  const [formData, setFormData] = useState<FormData>({
    amount: 1000000,
    months: 12,
    rate: 15.5,
    paymentType: 'annuity',
  });

  // Обработчик изменения любого поля
  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value, type } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: type === 'number' ? parseFloat(value) || 0 : value,
    }));
  };

  // Обработчик отправки формы
  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault(); // не даём странице перезагрузиться
    onCalculate(formData); // вызываем функцию, переданную из App
  };

  // Очистка формы
  const handleClear = () => {
    setFormData({
      amount: 0,
      months: 0,
      rate: 0,
      paymentType: 'annuity',
    });
  };

  return (
    <form onSubmit={handleSubmit}>
      <div>
        <label htmlFor="amount">Сумма кредита (₽):</label>
        <input
          type="number"
          id="amount"
          name="amount"
          value={formData.amount}
          onChange={handleChange}
          min={0}
          step={1000}
          required
        />
      </div>

      <div>
        <label htmlFor="months">Срок (мес.):</label>
        <input
          type="number"
          id="months"
          name="months"
          value={formData.months}
          onChange={handleChange}
          min={1}
          required
        />
      </div>

      <div>
        <label htmlFor="rate">Процентная ставка (% годовых):</label>
        <input
          type="number"
          id="rate"
          name="rate"
          value={formData.rate}
          onChange={handleChange}
          min={0}
          step={0.1}
          required
        />
      </div>

      <div>
        <label>Тип платежа:</label>
        <label>
          <input
            type="radio"
            name="paymentType"
            value="annuity"
            checked={formData.paymentType === 'annuity'}
            onChange={handleChange}
          />
          Аннуитетный
        </label>
        <label>
          <input
            type="radio"
            name="paymentType"
            value="differentiated"
            checked={formData.paymentType === 'differentiated'}
            onChange={handleChange}
          />
          Дифференцированный
        </label>
      </div>

      <button type="submit">Рассчитать</button>
      <button type="button" onClick={handleClear}>
        Очистить
      </button>
    </form>
  );
};

export default LoanForm;