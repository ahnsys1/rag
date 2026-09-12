import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { forkJoin, of, timer } from 'rxjs';
import { catchError, filter, switchMap } from 'rxjs/operators';
import { ApiService, RagDocument } from '../../core/api.service';

interface UploadItem {
  name: string;
  state: 'uploading' | 'accepted' | 'duplicate' | 'error';
  message?: string;
}

@Component({
  selector: 'app-documents',
  imports: [DatePipe, DecimalPipe],
  templateUrl: './documents.html',
  styleUrl: './documents.scss',
})
export class DocumentsComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly destroyRef = inject(DestroyRef);

  readonly documents = signal<RagDocument[]>([]);
  readonly uploads = signal<UploadItem[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly dragOver = signal(false);

  readonly processingCount = computed(
    () => this.documents().filter((d) => d.status === 'processing').length,
  );
  readonly indexedChunks = computed(() =>
    this.documents().reduce((sum, d) => sum + d.chunk_count, 0),
  );

  ngOnInit(): void {
    this.refresh();
    // Poll while any document is still being indexed.
    timer(2000, 2000)
      .pipe(
        filter(() => this.processingCount() > 0),
        switchMap(() => this.api.listDocuments().pipe(catchError(() => of(null)))),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((docs) => docs && this.documents.set(docs));
  }

  refresh(): void {
    this.loading.set(true);
    this.api.listDocuments().subscribe({
      next: (docs) => {
        this.documents.set(docs);
        this.error.set(null);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(this.describe(err));
        this.loading.set(false);
      },
    });
  }

  onFileInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files) {
      this.upload(Array.from(input.files));
      input.value = '';
    }
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.dragOver.set(false);
    if (event.dataTransfer?.files) {
      this.upload(Array.from(event.dataTransfer.files));
    }
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.dragOver.set(true);
  }

  upload(files: File[]): void {
    if (!files.length) return;
    const items: UploadItem[] = files.map((f) => ({ name: f.name, state: 'uploading' }));
    this.uploads.update((u) => [...items, ...u]);

    forkJoin(
      files.map((file, i) =>
        this.api.uploadDocument(file).pipe(
          catchError((err: HttpErrorResponse) => {
            this.patchUpload(items[i], { state: 'error', message: this.describe(err) });
            return of(null);
          }),
        ),
      ),
    ).subscribe((results) => {
      results.forEach((res, i) => {
        if (res) {
          this.patchUpload(items[i], {
            state: res.duplicate ? 'duplicate' : 'accepted',
            message: res.duplicate ? 'Dokument je již zaindexován.' : 'Přijato k indexaci.',
          });
        }
      });
      this.refresh();
    });
  }

  remove(doc: RagDocument): void {
    if (!confirm(`Smazat dokument "${doc.filename}" včetně indexu?`)) return;
    this.api.deleteDocument(doc.id).subscribe({
      next: () => this.documents.update((docs) => docs.filter((d) => d.id !== doc.id)),
      error: (err: HttpErrorResponse) => this.error.set(this.describe(err)),
    });
  }

  clearUploads(): void {
    this.uploads.set([]);
  }

  private patchUpload(item: UploadItem, patch: Partial<UploadItem>): void {
    this.uploads.update((list) => list.map((u) => (u === item ? { ...u, ...patch } : u)));
  }

  private describe(err: HttpErrorResponse): string {
    const detail = err.error?.detail;
    if (typeof detail === 'string') return detail;
    if (err.status === 0) return 'Backend není dostupný.';
    return `${err.status} ${err.statusText}`;
  }
}
