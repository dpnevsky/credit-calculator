export type Gender = 'MALE' | 'FEMALE' | 'NON_BINARY';
export type MaritalStatus = 'SINGLE' | 'MARRIED' | 'DIVORCED' | 'WIDOWED';
export type EmploymentStatus = 'EMPLOYED' | 'UNEMPLOYED' | 'SELF_EMPLOYED' | 'RETIRED' | 'BUSINESS_OWNER' | 'STUDENT';
export type Position = 'MID_MANAGER' | 'TOP_MANAGER' | 'JUNIOR_MANAGER' | 'DEVELOPER' | 'SALES' | 'ACCOUNTANT' | 'HR' | 'OTHER';
export type PaymentType = 'ANNUITY' | 'DIFFERENTIAL';

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

export interface CreateApplicationResponse {
  applicationId: string;
  status: string;
  amount: number;
  termMonths: number;
  prescoringReasons: string[];
  preliminaryOffers: PreliminaryOffer[];
}

export interface SubmitApplicationRequest {
  gender: Gender;
  passportIssueDate: string;
  passportIssueBranch: string;
  maritalStatus: MaritalStatus;
  dependentAmount: number;
  employmentStatus: EmploymentStatus;
  employerInn: string;
  salary: number;
  position: Position;
  workExperienceTotal: number;
  workExperienceCurrent: number;
  accountNumber: string;
  insuranceEnabled: boolean;
  salaryClient: boolean;
}

export interface ApplicationSubmitData {
  gender: Gender;
  passportIssueDate: string;
  passportIssueBranch: string;
  maritalStatus: MaritalStatus;
  dependentAmount: number;
  employmentStatus: EmploymentStatus;
  employerInn: string | null;
  salary: number;
  position: Position;
  workExperienceTotal: number;
  workExperienceCurrent: number;
  accountNumber: string | null;
  insuranceEnabled: boolean;
  salaryClient: boolean;
  submittedAt: string;
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
  paymentType: PaymentType;
  submitData: ApplicationSubmitData | null;
}

export interface SubmitApplicationResponse {
  applicationId: string;
  scoringStatus: string;
  scoringDecision: string;
  scoreValue: number;
  riskGrade: string | null;
  rulesVersion: string;
  approvedAmount: number | null;
  approvedTermMonths: number | null;
  approvedRate: number | null;
  rejectionReasons: string[];
}

export interface ScoringResultResponse {
  applicationId: string;
  scoringDecision: string;
  scoreValue: number;
  riskGrade: string | null;
  rulesVersion: string;
  approvedAmount: number | null;
  approvedTermMonths: number | null;
  approvedRate: number | null;
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

export interface SelectOfferRequest {
  paymentType?: PaymentType;
}

export interface DocumentResponse {
  documentId: string;
  documentType: string;
  format: string;
  fileName: string;
  mimeType: string;
  status: string;
  generatedAt: string;
}

export interface RequestDocumentsResponse {
  applicationId: string;
  status: string;
  message: string;
}

export interface RequestDocumentsRequest {
  paymentType?: PaymentType;
}
