import axios from 'axios';
import AuthService from './auth.service';
import type {
  ApplicationResponse,
  ApplicationSubmitData,
  ContractResponse,
  ContractStatus,
  CreateApplicationRequest,
  CreateApplicationResponse,
  DocumentResponse,
  OfferResponse,
  RequestDocumentsRequest,
  RequestDocumentsResponse,
  ScoringResultResponse,
  SelectOfferRequest,
  SelectOfferResponse,
  SubmitApplicationRequest,
  SubmitApplicationResponse,
} from '../types/api';

interface BackendSubmitApplicationRequest {
  insuranceEnabled: boolean;
  salaryClient: boolean;
  gender: SubmitApplicationRequest['gender'];
  maritalStatus: SubmitApplicationRequest['maritalStatus'];
  dependentAmount: number;
  passportIssueDate: string;
  passportIssueBranch: string;
  accountNumber: string | null;
  employment: {
    employmentStatus: SubmitApplicationRequest['employmentStatus'];
    employerInn: string;
    salary: number;
    position: SubmitApplicationRequest['position'];
    workExperienceTotal: number;
    workExperienceCurrent: number;
  };
}

interface BackendApplicationSubmitData {
  insuranceEnabled: boolean;
  salaryClient: boolean;
  gender: ApplicationSubmitData['gender'];
  maritalStatus: ApplicationSubmitData['maritalStatus'];
  dependentAmount: number;
  passportIssueDate: string;
  passportIssueBranch: string;
  accountNumber: string | null;
  employment: {
    employmentStatus: ApplicationSubmitData['employmentStatus'];
    employerInn: string | null;
    salary: number;
    position: ApplicationSubmitData['position'];
    workExperienceTotal: number;
    workExperienceCurrent: number;
  };
  submittedAt: string;
}

interface BackendApplicationResponse {
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
  paymentType: ApplicationResponse['paymentType'];
  contractStatus: string;
  contractSignedAt: string | null;
  signatureId: string | null;
  submitData: BackendApplicationSubmitData | null;
}

interface BackendContractResponse {
  applicationId: string;
  contractNumber: string;
  contractStatus: string;
  applicationStatus: string;
  signed: boolean;
  signedAt: string | null;
  signatureId: string | null;
}

interface BackendSubmitApplicationResponse {
  applicationId: string;
  status: string;
  decision: string;
  scoreValue: number;
  riskGrade: string | null;
  rulesVersion: string;
  approvedAmount: number | null;
  approvedTermMonths: number | null;
  approvedRate: number | null;
  reasons: string[];
}

interface BackendScoringResultResponse {
  applicationId: string;
  decision: string;
  scoreValue: number;
  riskGrade: string | null;
  rulesVersion: string;
  approvedAmount: number | null;
  approvedTermMonths: number | null;
  approvedRate: number | null;
  reasons: string[];
  scoredAt: string;
}

interface BackendRequestDocumentsResponse {
  applicationId: string;
  status: string;
  message: string;
}

interface BackendErrorResponse {
  message?: string;
  validationErrors?: Array<{
    field?: string;
    message?: string;
  }>;
}

const api = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.request.use(async (config) => {
  const token = await AuthService.getToken(30);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error: unknown) => {
    if (axios.isAxiosError<BackendErrorResponse>(error)) {
      const validationMessage = error.response?.data?.validationErrors
        ?.map((validationError) => validationError.message)
        .filter((message): message is string => Boolean(message))
        .join('. ');
      const responseMessage = error.response?.data?.message;

      throw new Error(validationMessage || responseMessage || error.message);
    }

    throw error;
  },
);

const toBackendSubmitApplicationRequest = (data: SubmitApplicationRequest): BackendSubmitApplicationRequest => ({
  insuranceEnabled: data.insuranceEnabled,
  salaryClient: data.salaryClient,
  gender: data.gender,
  maritalStatus: data.maritalStatus,
  dependentAmount: data.dependentAmount,
  passportIssueDate: data.passportIssueDate,
  passportIssueBranch: data.passportIssueBranch,
  accountNumber: data.accountNumber.trim() === '' ? null : data.accountNumber.trim(),
  employment: {
    employmentStatus: data.employmentStatus,
    employerInn: data.employerInn.trim(),
    salary: data.salary,
    position: data.position,
    workExperienceTotal: data.workExperienceTotal,
    workExperienceCurrent: data.workExperienceCurrent,
  },
});

const mapSubmitData = (data: BackendApplicationSubmitData): ApplicationSubmitData => ({
  gender: data.gender,
  passportIssueDate: data.passportIssueDate,
  passportIssueBranch: data.passportIssueBranch,
  maritalStatus: data.maritalStatus,
  dependentAmount: data.dependentAmount,
  employmentStatus: data.employment.employmentStatus,
  employerInn: data.employment.employerInn,
  salary: data.employment.salary,
  position: data.employment.position,
  workExperienceTotal: data.employment.workExperienceTotal,
  workExperienceCurrent: data.employment.workExperienceCurrent,
  accountNumber: data.accountNumber,
  insuranceEnabled: data.insuranceEnabled,
  salaryClient: data.salaryClient,
  submittedAt: data.submittedAt,
});

const mapContractStatus = (status: string): ContractStatus => {
  switch (status) {
    case 'READY_TO_SIGN':
      return 'READY_TO_SIGN';
    case 'SIGNED':
      return 'SIGNED';
    case 'EXPIRED':
      return 'EXPIRED';
    case 'CANCELLED':
      return 'CANCELLED';
    default:
      return 'NOT_CREATED';
  }
};

const mapApplicationResponse = (data: BackendApplicationResponse): ApplicationResponse => ({
  ...data,
  paymentType: data.paymentType === 'DIFFERENTIAL' ? 'DIFFERENTIAL' : 'ANNUITY',
  contractStatus: mapContractStatus(data.contractStatus),
  submitData: data.submitData ? mapSubmitData(data.submitData) : null,
});

const mapContractResponse = (data: BackendContractResponse): ContractResponse => ({
  applicationId: data.applicationId,
  contractNumber: data.contractNumber,
  contractStatus: mapContractStatus(data.contractStatus),
  applicationStatus: data.applicationStatus,
  signed: data.signed,
  signedAt: data.signedAt,
  signatureId: data.signatureId,
});

const mapSubmitApplicationResponse = (data: BackendSubmitApplicationResponse): SubmitApplicationResponse => ({
  applicationId: data.applicationId,
  scoringStatus: data.status,
  scoringDecision: data.decision,
  scoreValue: data.scoreValue,
  riskGrade: data.riskGrade,
  rulesVersion: data.rulesVersion,
  approvedAmount: data.approvedAmount,
  approvedTermMonths: data.approvedTermMonths,
  approvedRate: data.approvedRate,
  rejectionReasons: data.reasons,
});

const mapScoringResultResponse = (data: BackendScoringResultResponse): ScoringResultResponse => ({
  applicationId: data.applicationId,
  scoringDecision: data.decision,
  scoreValue: data.scoreValue,
  riskGrade: data.riskGrade,
  rulesVersion: data.rulesVersion,
  approvedAmount: data.approvedAmount,
  approvedTermMonths: data.approvedTermMonths,
  approvedRate: data.approvedRate,
  rejectionReasons: data.reasons,
  evaluatedAt: data.scoredAt,
});

const ApiService = {
  async createApplication(data: CreateApplicationRequest): Promise<CreateApplicationResponse> {
    const response = await api.post<CreateApplicationResponse>('/applications', data);
    return response.data;
  },

  async getApplication(applicationId: string): Promise<ApplicationResponse> {
    const response = await api.get<BackendApplicationResponse>(`/applications/${applicationId}`);
    return mapApplicationResponse(response.data);
  },

  async getApplications(): Promise<ApplicationResponse[]> {
    const response = await api.get<BackendApplicationResponse[]>('/applications');
    return response.data.map(mapApplicationResponse);
  },

  async getContract(applicationId: string): Promise<ContractResponse> {
    const response = await api.get<BackendContractResponse>(`/applications/${applicationId}/contract`);
    return mapContractResponse(response.data);
  },

  async submitApplication(applicationId: string, data: SubmitApplicationRequest): Promise<SubmitApplicationResponse> {
    const response = await api.post<BackendSubmitApplicationResponse>(
      `/applications/${applicationId}/submit`,
      toBackendSubmitApplicationRequest(data),
    );
    return mapSubmitApplicationResponse(response.data);
  },

  async getScoringResult(applicationId: string): Promise<ScoringResultResponse> {
    const response = await api.get<BackendScoringResultResponse>(`/applications/${applicationId}/scoring-result`);
    return mapScoringResultResponse(response.data);
  },

  async getOffers(applicationId: string): Promise<OfferResponse[]> {
    const response = await api.get<OfferResponse[]>(`/applications/${applicationId}/offers`);
    return response.data;
  },

  async selectOffer(
    applicationId: string,
    offerId: string,
    data?: SelectOfferRequest,
  ): Promise<SelectOfferResponse> {
    const response = await api.post<SelectOfferResponse>(
      `/applications/${applicationId}/offers/${offerId}/select`,
      data,
    );
    return response.data;
  },

  async requestDocuments(
    applicationId: string,
    data?: RequestDocumentsRequest,
  ): Promise<RequestDocumentsResponse> {
    const response = await api.post<BackendRequestDocumentsResponse>(
      `/applications/${applicationId}/request-documents`,
      data,
    );
    return {
      applicationId: response.data.applicationId,
      status: response.data.status,
      message: response.data.message,
    };
  },

  async getDocuments(applicationId: string): Promise<DocumentResponse[]> {
    const response = await api.get<DocumentResponse[]>(`/applications/${applicationId}/documents`);
    return response.data;
  },

  async signContract(applicationId: string): Promise<ContractResponse> {
    const response = await api.post<BackendContractResponse>(`/applications/${applicationId}/contract/sign`);
    return mapContractResponse(response.data);
  },

  async downloadDocument(documentId: string): Promise<{ blob: Blob; fileName: string | null }> {
    const response = await api.get<Blob>(`/documents/${documentId}/download`, {
      responseType: 'blob',
    });

    const contentDisposition = response.headers['content-disposition'] as string | undefined;
    const utf8Match = contentDisposition?.match(/filename\*=UTF-8''([^;]+)/i);
    const simpleMatch = contentDisposition?.match(/filename="?([^"]+)"?/i);
    const rawFileName = utf8Match?.[1] ?? simpleMatch?.[1] ?? null;

    return {
      blob: response.data,
      fileName: rawFileName ? decodeURIComponent(rawFileName) : null,
    };
  },

  getDocumentDownloadUrl(documentId: string): string {
    return `/api/documents/${documentId}/download`;
  },
};

export default ApiService;
