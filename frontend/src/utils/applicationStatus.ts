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
  CONTRACT_SIGNED: { label: 'Подписано', color: '#16a34a' },
};

const contractStatusLabels: Record<string, string> = {
  NOT_CREATED: 'Не создан',
  READY_TO_SIGN: 'Готов к подписанию',
  SIGNED: 'Подписан',
  EXPIRED: 'Истёк',
  CANCELLED: 'Отменён',
};

export const normalizeApplicationStatus = (status: string) => {
  const normalizedStatus = status.toUpperCase();

  if (normalizedStatus === 'PRESCORING_FAILED') {
    return 'PRESCORING_REJECTED';
  }

  return normalizedStatus;
};

export const getEffectiveApplicationStatus = (status: string) => normalizeApplicationStatus(status);

export const getApplicationStatusLabel = (status: string, _applicationId?: string) =>
  statusConfig[getEffectiveApplicationStatus(status)]?.label ?? normalizeApplicationStatus(status);

export const getApplicationStatusColor = (status: string, _applicationId?: string) =>
  statusConfig[getEffectiveApplicationStatus(status)]?.color ?? '#6b7280';

export const getContractStatusLabel = (status: string) =>
  contractStatusLabels[status] ?? status;

export const isRejectedApplicationStatus = (status: string, _applicationId?: string) => {
  const effectiveStatus = getEffectiveApplicationStatus(status);
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
