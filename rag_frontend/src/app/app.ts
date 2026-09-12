import { Component, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { ApiService, HealthResponse } from './core/api.service';

@Component({
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  selector: 'app-root',
  styleUrl: './app.scss',
  templateUrl: './app.html',
})
export class App {
  private readonly api = inject(ApiService);
  protected readonly health = signal<HealthResponse | null>(null);
  protected readonly offline = signal(false);

  constructor() {
    this.api.health().subscribe({
      next: (h) => this.health.set(h),
      error: () => this.offline.set(true),
    });
  }
}
