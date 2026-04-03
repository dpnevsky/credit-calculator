export interface CreateApplicationRequest {
  amount: number;
  termMonths: number;
  firstName: string;
  lastName: string;
  middleName?: string;
  email: string;
  birthDate: string;
  passportSeries: string;
  passportNumber: string;
}

export interface CreateApplicationResponse {
  applicationId: string;
  status: string;
  createdAt: string;
  offers: PreliminaryOffer[];
}

export interface PreliminaryOffer {
  applicationId: string;
  requestedAmount: number;
  totalAmount: number;
  termMonths: number;
  monthlyPayment: number;
  rate: number;
  insuranceEnabled: boolean;
  salaryClient: boolean;
}

export interface ApplicationResponse {
  applicationId: string;
  status: string;
  amount: number;
  termMonths: number;
  firstName: string;
  lastName: string;
  middleName: string | null;
  email: string;
  birthDate: string;
  passportSeries: string;
  passportNumber: string;
  createdAt: string;
  updatedAt: string;
}

export interface SubmitApplicationRequest {
  gender: 'MALE' | 'FEMALE' | 'NON_BINARY';
  passportIssueDate: string;
  passportIssueBranch: string;
  maritalStatus: 'SINGLE' | 'MARRIED' | 'DIVORCED' | 'WIDOWED';
  dependentAmount: number;
  employmentStatus: 'EMPLOYED' | 'UNEMPLOYED' | 'SELF_EMPLOYED' | 'RETIRED' | 'BUSINESS_OWNER' | 'STUDENT';
  employerInn: string;
  salary: number;
  position: 'MID_MANAGER' | 'TOP_MANAGER' | 'JUNIOR_MANAGER' | 'DEVELOPER' | 'SALES' | 'ACCOUNTANT' | 'HR' | 'OTHER';
  workExperienceTotal: number;
  workExperienceCurrent: number;
  accountNumber: string;
  insuranceEnabled: boolean;
  salaryClient: boolean;
}

export interface SubmitApplicationResponse {
  applicationId: string;
  scoringDecision: string;
  scoringStatus: string;
  approvedRate: number | null;
  approvedAmount: number | null;
  monthlyPayment: number | null;
  riskGrade: string | null;
  rejectionReasons: string[];
}

export interface ScoringResultResponse {
  applicationId: string;
  scoringDecision: string;
  scoringStatus: string;
  approvedRate: number | null;
  approvedAmount: number | null;
  monthlyPayment: number | null;
  riskGrade: string | null;
  rejectionReasons: string[];
  evaluatedAt: string;
}

export interface OfferResponse {
  offerId: string;
  applicationId: string;
  requestedAmount: number;
  totalAmount: number;
  termMonths: number;
  monthlyPayment: number;
  rate: number;
  insuranceEnabled: boolean;
  salaryClient: boolean;
  selected: boolean;
  createdAt: string;
}

export interface SelectOfferResponse {
  applicationId: string;
  offerId: string;
  applicationStatus: string;
  message: string;
}

export interface DocumentResponse {
  documentId: string;
  applicationId: string;
  requestId: string;
  documentType: string;
  format: string;
  fileName: string;
  mimeType: string;
  status: string;
  generatedAt: string;
  receivedAt: string;
}

export interface RequestDocumentsResponse {
  applicationId: string;
  requestId: string;
  message: string;
}
