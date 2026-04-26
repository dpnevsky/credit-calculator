import type { User } from '../services/auth.service';
import type { CreateApplicationRequest, SubmitApplicationRequest } from '../types/api';

export const APPLICATION_FORM_DRAFT_KEY = 'cc_create_application_form_draft';
export const SCORING_FORM_DRAFT_KEY = 'cc_create_application_scoring_draft';
export const PASSPORT_ISSUE_BRANCH_ERROR = 'Код подразделения должен быть в формате 000-000';
export const PASSPORT_ISSUE_BRANCH_PATTERN = /^\d{3}-\d{3}$/;

export interface ApplicationFormState {
  amount: string;
  termMonths: string;
  firstName: string;
  lastName: string;
  middleName: string;
  birthDate: string;
  passportSeries: string;
  passportNumber: string;
}

export interface ScoringFormState {
  gender: SubmitApplicationRequest['gender'];
  passportIssueDate: string;
  passportIssueBranch: string;
  maritalStatus: SubmitApplicationRequest['maritalStatus'];
  dependentAmount: string;
  employmentStatus: SubmitApplicationRequest['employmentStatus'];
  employerInn: string;
  salary: string;
  position: SubmitApplicationRequest['position'];
  workExperienceTotal: string;
  workExperienceCurrent: string;
  accountNumber: string;
  insuranceEnabled: boolean;
  salaryClient: boolean;
}

export const toLocalDateInputValue = (date: Date): string => {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
};

export const getTodayDate = (): string => toLocalDateInputValue(new Date());

export const getDefaultPassportIssueDate = (): string => {
  const date = new Date();
  date.setDate(date.getDate() - 1);
  return toLocalDateInputValue(date);
};

export const formatPassportIssueBranch = (value: string): string => {
  const digits = value.replace(/\D/g, '').slice(0, 6);
  if (digits.length <= 3) {
    return digits;
  }

  return `${digits.slice(0, 3)}-${digits.slice(3)}`;
};

export const isPassportIssueBranchValid = (value: string): boolean => (
  PASSPORT_ISSUE_BRANCH_PATTERN.test(value)
);

export const createInitialApplicationFormState = (): ApplicationFormState => ({
  amount: '500000',
  termMonths: '12',
  firstName: '',
  lastName: '',
  middleName: '',
  birthDate: '',
  passportSeries: '',
  passportNumber: '',
});

export const createClearedApplicationFormState = (): ApplicationFormState => ({
  amount: '',
  termMonths: '',
  firstName: '',
  lastName: '',
  middleName: '',
  birthDate: '',
  passportSeries: '',
  passportNumber: '',
});

export const createInitialScoringFormState = (): ScoringFormState => ({
  gender: 'MALE',
  passportIssueDate: getDefaultPassportIssueDate(),
  passportIssueBranch: '',
  maritalStatus: 'SINGLE',
  dependentAmount: '0',
  employmentStatus: 'EMPLOYED',
  employerInn: '',
  salary: '50000',
  position: 'OTHER',
  workExperienceTotal: '18',
  workExperienceCurrent: '3',
  accountNumber: '',
  insuranceEnabled: false,
  salaryClient: false,
});

export const createClearedScoringFormState = (): ScoringFormState => ({
  gender: 'MALE',
  passportIssueDate: getDefaultPassportIssueDate(),
  passportIssueBranch: '',
  maritalStatus: 'SINGLE',
  dependentAmount: '',
  employmentStatus: 'EMPLOYED',
  employerInn: '',
  salary: '',
  position: 'OTHER',
  workExperienceTotal: '',
  workExperienceCurrent: '',
  accountNumber: '',
  insuranceEnabled: false,
  salaryClient: false,
});

export const readDraft = <T,>(key: string, warningMessage: string): Partial<T> | null => {
  const rawValue = localStorage.getItem(key);
  if (!rawValue) {
    return null;
  }

  try {
    return JSON.parse(rawValue) as Partial<T>;
  } catch (parseError) {
    console.warn(warningMessage, parseError);
    return null;
  }
};

export const saveDraft = <T,>(key: string, value: T): void => {
  localStorage.setItem(key, JSON.stringify(value));
};

export const clearApplicationDrafts = (): void => {
  localStorage.removeItem(APPLICATION_FORM_DRAFT_KEY);
  localStorage.removeItem(SCORING_FORM_DRAFT_KEY);
};

export const applyUserPrefill = (
  form: ApplicationFormState,
  user: User | null,
): ApplicationFormState => {
  if (!user) {
    return form;
  }

  return {
    ...form,
    firstName: form.firstName || user.firstName || '',
    lastName: form.lastName || user.lastName || '',
    middleName: form.middleName || user.middleName || '',
    birthDate: form.birthDate || user.birthDate || '',
  };
};

const toOptionalNumber = (value: string, fallback: number): number => (
  value.trim() === '' ? fallback : Number(value)
);

const parseRequiredNumber = (value: string, label: string): number => {
  const trimmedValue = value.trim();
  if (trimmedValue === '') {
    throw new Error(`Поле "${label}" обязательно`);
  }

  const parsedValue = Number(trimmedValue);
  if (Number.isNaN(parsedValue)) {
    throw new Error(`Поле "${label}" заполнено некорректно`);
  }

  return parsedValue;
};

export const buildCreateApplicationPayload = (
  form: ApplicationFormState,
): CreateApplicationRequest => ({
  amount: parseRequiredNumber(form.amount, 'Сумма кредита'),
  termMonths: parseRequiredNumber(form.termMonths, 'Срок кредита'),
  firstName: form.firstName.trim(),
  lastName: form.lastName.trim(),
  middleName: form.middleName.trim(),
  birthDate: form.birthDate,
  passportSeries: form.passportSeries.trim(),
  passportNumber: form.passportNumber.trim(),
});

export const buildSubmitApplicationPayload = (
  form: ScoringFormState,
): SubmitApplicationRequest => {
  const employerInn = form.employerInn.trim();
  const passportIssueBranch = form.passportIssueBranch.trim();

  if (!isPassportIssueBranchValid(passportIssueBranch)) {
    throw new Error(PASSPORT_ISSUE_BRANCH_ERROR);
  }

  if (form.employmentStatus !== 'UNEMPLOYED' && !/^(\d{10}|\d{12})$/.test(employerInn)) {
    throw new Error('ИНН работодателя должен содержать 10 или 12 цифр');
  }

  return {
    gender: form.gender,
    passportIssueDate: form.passportIssueDate,
    passportIssueBranch,
    maritalStatus: form.maritalStatus,
    dependentAmount: toOptionalNumber(form.dependentAmount, 0),
    employmentStatus: form.employmentStatus,
    employerInn,
    salary: parseRequiredNumber(form.salary, 'Зарплата'),
    position: form.position,
    workExperienceTotal: parseRequiredNumber(form.workExperienceTotal, 'Общий стаж'),
    workExperienceCurrent: parseRequiredNumber(form.workExperienceCurrent, 'Текущий стаж'),
    accountNumber: form.accountNumber.trim(),
    insuranceEnabled: false,
    salaryClient: false,
  };
};
