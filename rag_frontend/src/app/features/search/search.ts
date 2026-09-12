import { Component, OnInit, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { ApiService, RagDocument, SearchHit, SearchMode } from '../../core/api.service';

@Component({
  selector: 'app-search',
  imports: [FormsModule, DecimalPipe],
  templateUrl: './search.html',
  styleUrl: './search.scss',
})
export class SearchComponent implements OnInit {
  private readonly api = inject(ApiService);

  query = '';
  topK = 5;
  mode: SearchMode = 'hybrid';
  selectedDocumentIds: string[] = [];

  readonly documents = signal<RagDocument[]>([]);
  readonly hits = signal<SearchHit[]>([]);
  readonly answer = signal<string | null>(null);
  readonly answerModel = signal<string | null>(null);
  readonly busy = signal<'search' | 'ask' | null>(null);
  readonly error = signal<string | null>(null);
  readonly lastQuery = signal<string | null>(null);
  readonly llmAvailable = signal<boolean | null>(null);

  ngOnInit(): void {
    this.api.listDocuments().subscribe({
      next: (docs) => this.documents.set(docs.filter((d) => d.status === 'indexed')),
      error: () => undefined,
    });
    this.api.health().subscribe({
      next: (h) => this.llmAvailable.set(h.llm_provider !== 'none'),
      error: () => this.llmAvailable.set(false),
    });
  }

  search(): void {
    const q = this.query.trim();
    if (!q) return;
    this.busy.set('search');
    this.error.set(null);
    this.answer.set(null);
    this.api.search(this.request(q)).subscribe({
      next: (res) => {
        this.hits.set(res.hits);
        this.lastQuery.set(q);
        this.busy.set(null);
      },
      error: (err: HttpErrorResponse) => this.fail(err),
    });
  }

  ask(): void {
    const q = this.query.trim();
    if (!q) return;
    this.busy.set('ask');
    this.error.set(null);
    this.api.ask(this.request(q)).subscribe({
      next: (res) => {
        this.answer.set(res.answer);
        this.answerModel.set(res.model);
        this.hits.set(res.sources);
        this.lastQuery.set(q);
        this.busy.set(null);
      },
      error: (err: HttpErrorResponse) => this.fail(err),
    });
  }

  private request(query: string) {
    return {
      query,
      top_k: this.topK,
      mode: this.mode,
      document_ids: this.selectedDocumentIds.length ? this.selectedDocumentIds : null,
    };
  }

  private fail(err: HttpErrorResponse): void {
    const detail = err.error?.detail;
    this.error.set(
      typeof detail === 'string'
        ? detail
        : err.status === 0
          ? 'Backend není dostupný.'
          : `${err.status} ${err.statusText}`,
    );
    this.busy.set(null);
  }
}
