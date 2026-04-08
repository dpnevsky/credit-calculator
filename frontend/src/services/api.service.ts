import axios from 'axios';
import AuthService from './auth.service';
import type {
  CreateApplicationRequest,
  CreateApplicationResponse,
  ApplicationResponse,
  SubmitApplicationRequest,
  SubmitApplicationResponse,
  ScoringResultResponse,
  OfferResponse,
  SelectOfferResponse,
  DocumentResponse,
  RequestDocumentsResponse,
} from '../types/api';

const api = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.request.use(async (config) => {
  try {
    await AuthService.refreshToken(30);
  } catch {
    // token refresh failed, continue without token
  }
  const token = AuthService.getToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

const ApiService = {
  async createApplication(data: CreateApplicationRequest): Promise<CreateApplicationResponse> {
    const response = await api.post<CreateApplicationResponse>('/applications', data);
    return response.data;
  },

  async getApplication(applicationId: string): Promise<ApplicationResponse> {
    const response = await api.get<ApplicationResponse>(`/applications/${applicationId}`);
    return response.data;
  },

  async getApplications(): Promise<ApplicationResponse[]> {
    const response = await api.get<ApplicationResponse[]>('/applications');
    return response.data;
  },

  async submitApplication(applicationId: string, data: SubmitApplicationRequest): Promise<SubmitApplicationResponse> {
    const response = await api.post<SubmitApplicationResponse>(`/applications/${applicationId}/submit`, data);
    return response.data;
  },

  async getScoringResult(applicationId: string): Promise<ScoringResultResponse> {
    const response = await api.get<ScoringResultResponse>(`/applications/${applicationId}/scoring-result`);
    return response.data;
  },

  async getOffers(applicationId: string): Promise<OfferResponse[]> {
    const response = await api.get<OfferResponse[]>(`/applications/${applicationId}/offers`);
    return response.data;
  },

  async selectOffer(applicationId: string, offerId: string): Promise<SelectOfferResponse> {
    const response = await api.post<SelectOfferResponse>(`/applications/${applicationId}/offers/${offerId}/select`);
    return response.data;
  },

  async requestDocuments(applicationId: string): Promise<RequestDocumentsResponse> {
    const response = await api.post<RequestDocumentsResponse>(`/applications/${applicationId}/request-documents`);
    return response.data;
  },

  async getDocuments(applicationId: string): Promise<DocumentResponse[]> {
    const response = await api.get<DocumentResponse[]>(`/applications/${applicationId}/documents`);
    return response.data;
  },

  getDocumentDownloadUrl(documentId: string): string {
    return `/api/documents/${documentId}/download`;
  },
};

export default ApiService;
