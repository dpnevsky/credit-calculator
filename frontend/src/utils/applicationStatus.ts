const SIGNED_APPLICATIONS_STORAGE_KEY = 'cc_signed_applications';

const rejectionReasonLabels: Record<string, string> = {
  AGE_OUT_OF_RANGE: 'Возраст не соответствует требованиям банка.',
  UNEMPLOYED: 'Отсутствует подтверждённая занятость.',
  AMOUNT_EXCEEDS_24_MONTHS_OF_SALARY: 'Запрошенная сумма слишком велика относительно дохода.',
  TOTAL_WORK_EXPERIENCE_TOO_LOW: 'Недостаточный общий трудовой стаж.',
  CURRENT_WORK_EXPERIENCE_TOO_LOW: 'Недостаточный стаж на текущем месте работы.',
};

const statusConfig: Record<string, { label: string; color: string }> = {
  DRAFT: { label: 'Предварительно одобрено', color: '#9ca3af' },
  PRESCORING_REJECTED: { label: 'Отказано', color: '#dc2626' },
  SUBMITTED: { label: 'Отправлена', color: '#0ea5e9' },
  SCORING_COMPLETED: { label: 'Предварительно одобрено', color: '#9ca3af' },
  SCORING_APPROVED: { label: 'Предварительно одобрено', color: '#9ca3af' },
  SCORING_REJECTED: { label: 'Отказано', color: '#dc2626' },
  OFFER_SELECTED: { label: 'Одобрено', color: '#ca8a04' },
  DOCUMENTS_REQUESTED: { label: 'Одобрено', color: '#ca8a04' },
  DOCUMENTS_READY: { label: 'Одобрено', color: '#ca8a04' },
  SIGNED: { label: 'Подписано', color: '#16a34a' },
};

type SignedApplicationsMap = Record<string, string>;

const readSignedApplications = (): SignedApplicationsMap => {
  if (typeof window === 'undefined') {
    return {};
  }

  const raw = window.localStorage.getItem(SIGNED_APPLICATIONS_STORAGE_KEY);
  if (!raw) {
    return {};
  }

  try {
    const parsed = JSON.parse(raw) as SignedApplicationsMap;
    return parsed && typeof parsed === 'object' ? parsed : {};
  } catch {
    return {};
  }
};

const writeSignedApplications = (value: SignedApplicationsMap) => {
  if (typeof window === 'undefined') {
    return;
  }

  window.localStorage.setItem(SIGNED_APPLICATIONS_STORAGE_KEY, JSON.stringify(value));
};

export const normalizeApplicationStatus = (status: string) => {
  const normalizedStatus = status.toUpperCase();

  if (normalizedStatus === 'PRESCORING_FAILED') {
    return 'PRESCORING_REJECTED';
  }

  return normalizedStatus;
};

export const isApplicationSigned = (applicationId?: string) => {
  if (!applicationId) {
    return false;
  }

  return Boolean(readSignedApplications()[applicationId]);
};

export const markApplicationAsSigned = (applicationId: string) => {
  const signedApplications = readSignedApplications();
  signedApplications[applicationId] = new Date().toISOString();
  writeSignedApplications(signedApplications);
};

export const getEffectiveApplicationStatus = (status: string, applicationId?: string) => {
  if (applicationId && isApplicationSigned(applicationId)) {
    return 'SIGNED';
  }

  return normalizeApplicationStatus(status);
};

export const getApplicationStatusLabel = (status: string, applicationId?: string) =>
  statusConfig[getEffectiveApplicationStatus(status, applicationId)]?.label ?? normalizeApplicationStatus(status);

export const getApplicationStatusColor = (status: string, applicationId?: string) =>
  statusConfig[getEffectiveApplicationStatus(status, applicationId)]?.color ?? '#6b7280';

export const isRejectedApplicationStatus = (status: string, applicationId?: string) => {
  const effectiveStatus = getEffectiveApplicationStatus(status, applicationId);
  return effectiveStatus === 'PRESCORING_REJECTED' || effectiveStatus === 'SCORING_REJECTED';
};

const formatRejectionReason = (reason: string) => {
  const mappedReason = rejectionReasonLabels[reason];
  if (mappedReason) {
    return mappedReason;
  }

  const humanizedReason = reason.replaceAll('_', ' ').toLowerCase();
  return `${humanizedReason.charAt(0).toUpperCase()}${humanizedReason.slice(1)}.`;
};

export const getApplicationRejectionReasons = (status: string, reasons: string[] = []) => {
  const normalizedStatus = normalizeApplicationStatus(status);

  if (reasons.length > 0) {
    return reasons.map(formatRejectionReason);
  }

  if (normalizedStatus === 'PRESCORING_REJECTED') {
    return ['Заявка не прошла предварительную проверку банка.'];
  }

  if (normalizedStatus === 'SCORING_REJECTED') {
    return ['Банк отклонил заявку по результатам скоринга.'];
  }

  return [];
};
