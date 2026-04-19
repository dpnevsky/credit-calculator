import ApiService from '../services/api.service';
import type {
  ApplicationResponse,
  ApplicationSubmitData,
  DocumentResponse,
  OfferResponse,
  PaymentType,
  ScoringResultResponse,
} from '../types/api';

export type PaymentRow = {
  month: number;
  payment: number;
  principal: number;
  interest: number;
  balance: number;
};

export type ApplicationDetailsData = {
  application: ApplicationResponse;
  scoringResult: ScoringResultResponse | null;
  offers: OfferResponse[];
  documents: DocumentResponse[];
};

export const ACCOUNT_NUMBER_LENGTH = 20;

export const formatMoney = (value: number): string =>
  new Intl.NumberFormat('ru-RU', {
    style: 'currency',
    currency: 'RUB',
    maximumFractionDigits: 0,
  }).format(value);

export const formatDate = (value: string): string => {
  const match = value.match(/^(\d{4})-(\d{2})-(\d{2})/);
  if (!match) {
    return value;
  }

  const [, year, month, day] = match;
  return `${day}.${month}.${year}`;
};

export const normalizeSalaryAccountNumber = (value: string): string =>
  value.replace(/\D/g, '').slice(0, ACCOUNT_NUMBER_LENGTH);

export const getGenderLabel = (value?: ApplicationSubmitData['gender']): string => {
  switch (value) {
    case 'MALE':
      return 'Мужской';
    case 'FEMALE':
      return 'Женский';
    case 'NON_BINARY':
      return 'Другой';
    default:
      return 'Не указано';
  }
};

export const getMaritalStatusLabel = (value?: ApplicationSubmitData['maritalStatus']): string => {
  switch (value) {
    case 'SINGLE':
      return 'Не в браке';
    case 'MARRIED':
      return 'В браке';
    case 'DIVORCED':
      return 'Разведён(а)';
    case 'WIDOWED':
      return 'Вдовец/Вдова';
    default:
      return 'Не указано';
  }
};

export const getEmploymentStatusLabel = (value?: ApplicationSubmitData['employmentStatus']): string => {
  switch (value) {
    case 'EMPLOYED':
      return 'Работаю';
    case 'UNEMPLOYED':
      return 'Не работаю';
    case 'SELF_EMPLOYED':
      return 'Самозанятый';
    case 'RETIRED':
      return 'Пенсионер';
    case 'BUSINESS_OWNER':
      return 'Владелец бизнеса';
    case 'STUDENT':
      return 'Студент';
    default:
      return 'Не указано';
  }
};

export const getPositionLabel = (value?: ApplicationSubmitData['position']): string => {
  switch (value) {
    case 'TOP_MANAGER':
      return 'Топ-менеджер';
    case 'MID_MANAGER':
      return 'Менеджер';
    case 'JUNIOR_MANAGER':
      return 'Младший менеджер';
    case 'DEVELOPER':
      return 'Разработчик';
    case 'SALES':
      return 'Продажи';
    case 'ACCOUNTANT':
      return 'Бухгалтер';
    case 'HR':
      return 'HR';
    case 'OTHER':
      return 'Другое';
    default:
      return 'Не указано';
  }
};

export const formatBoolean = (value?: boolean): string => {
  if (value === undefined) {
    return 'Не указано';
  }

  return value ? 'Да' : 'Нет';
};

export const getPaymentTypeLabel = (type: PaymentType): string =>
  type === 'ANNUITY' ? 'Аннуитетный' : 'Дифференцированный';

export const getInsuranceAmount = (offer: OfferResponse): number =>
  offer.insuranceEnabled ? Math.max(0, offer.totalAmount - offer.requestedAmount) : 0;

export const buildPaymentSchedule = (
  offer: OfferResponse,
  scheduleType: PaymentType,
): PaymentRow[] => {
  const rows: PaymentRow[] = [];
  const principal = offer.totalAmount;
  const months = offer.termMonths;
  const monthlyRate = offer.rate / 12 / 100;
  let balance = principal;

  if (scheduleType === 'ANNUITY') {
    const annuityPayment =
      monthlyRate === 0
        ? principal / months
        : principal * (monthlyRate / (1 - (1 + monthlyRate) ** (-months)));

    for (let month = 1; month <= months; month += 1) {
      const interest = balance * monthlyRate;
      const principalPart = annuityPayment - interest;
      balance = Math.max(0, balance - principalPart);
      rows.push({
        month,
        payment: annuityPayment,
        principal: principalPart,
        interest,
        balance,
      });
    }

    return rows;
  }

  const principalPart = principal / months;
  for (let month = 1; month <= months; month += 1) {
    const interest = balance * monthlyRate;
    const payment = principalPart + interest;
    balance = Math.max(0, balance - principalPart);
    rows.push({
      month,
      payment,
      principal: principalPart,
      interest,
      balance,
    });
  }

  return rows;
};

export const getContractDocument = (documents: DocumentResponse[]): DocumentResponse | null =>
  documents.find((doc) => doc.format === 'PDF' && doc.documentType === 'CREDIT_AGREEMENT') ??
  documents.find((doc) => doc.format === 'PDF') ??
  null;

export const downloadBlobAsFile = (blob: Blob, fileName: string): void => {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
};

const loadDocuments = async (applicationId: string): Promise<DocumentResponse[]> => {
  try {
    return await ApiService.getDocuments(applicationId);
  } catch {
    return [];
  }
};

export const loadApplicationDetailsData = async (
  applicationId: string,
): Promise<ApplicationDetailsData> => {
  const application = await ApiService.getApplication(applicationId);

  const [scoringResult, offers, documents] = await Promise.all([
    ApiService.getScoringResult(applicationId).catch(() => null),
    ApiService.getOffers(applicationId).catch(() => []),
    loadDocuments(applicationId),
  ]);

  return {
    application,
    scoringResult,
    offers,
    documents,
  };
};

export const refreshDocumentsAfterRequest = async (
  applicationId: string,
  paymentType: PaymentType,
): Promise<DocumentResponse[]> => {
  await ApiService.requestDocuments(applicationId, { paymentType });
  return ApiService.getDocuments(applicationId);
};
