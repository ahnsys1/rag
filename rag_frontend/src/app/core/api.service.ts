import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export type DocumentStatus = 'processing' | 'indexed' | 'failed';

export interface RagDocument {
  id: string;
  filename: string;
  content_type: string | null;
  size_bytes: number;
  status: DocumentStatus;
  error: string | null;
  chunk_count: number;
  page_count: number | null;
  created_at: string;
  indexed_at: string | null;
}

export interface UploadResponse {
  document: RagDocument;
  duplicate: boolean;
}

export type SearchMode = 'semantic' | 'hybrid';

export interface SearchRequest {
  query: string;
  top_k: number;
  mode: SearchMode;
  document_ids?: string[] | null;
}

export interface SearchHit {
  chunk_id: number;
  document_id: string;
  filename: string;
  chunk_index: number;
  page: number | null;
  content: string;
  score: number;
}

export interface SearchResponse {
  query: string;
  mode: SearchMode;
  hits: SearchHit[];
}

export interface AskResponse {
  question: string;
  answer: string;
  model: string | null;
  sources: SearchHit[];
}

export interface HealthResponse {
  status: string;
  embedding_model: string;
  embedding_dim: number;
  llm_provider: string;
  llm_model: string | null;
}

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api';

  health(): Observable<HealthResponse> {
    return this.http.get<HealthResponse>(`${this.base}/health`);
  }

  listDocuments(): Observable<RagDocument[]> {
    return this.http.get<RagDocument[]>(`${this.base}/documents`);
  }

  uploadDocument(file: File): Observable<UploadResponse> {
    const form = new FormData();
    form.append('file', file, file.name);
    return this.http.post<UploadResponse>(`${this.base}/documents`, form);
  }

  deleteDocument(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/documents/${id}`);
  }

  search(req: SearchRequest): Observable<SearchResponse> {
    return this.http.post<SearchResponse>(`${this.base}/search`, req);
  }

  ask(req: SearchRequest): Observable<AskResponse> {
    return this.http.post<AskResponse>(`${this.base}/ask`, req);
  }
}
