import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', redirectTo: 'search', pathMatch: 'full' },
  {
    path: 'search',
    title: 'RAG – Vyhledávání',
    loadComponent: () => import('./features/search/search').then((m) => m.SearchComponent),
  },
  {
    path: 'documents',
    title: 'RAG – Dokumenty',
    loadComponent: () =>
      import('./features/documents/documents').then((m) => m.DocumentsComponent),
  },
  { path: '**', redirectTo: 'search' },
];
