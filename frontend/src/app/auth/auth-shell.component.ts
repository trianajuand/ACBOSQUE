import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-auth-shell',
  imports: [RouterOutlet],
  template: `
    <main class="auth-shell">
      <section class="brand-panel">
        <div class="brand-mark">AE</div>
        <p class="eyebrow">Acciones ElBosque</p>
        <h1>Trading academico con una experiencia limpia.</h1>
        <p class="brand-copy">
          Registro, MFA, pagos premium y ordenes conectadas al backend real del proyecto.
        </p>
        <div class="status-row">
          <span></span>
          Backend esperado en localhost:8080
        </div>
      </section>
      <section class="auth-panel page-enter">
        <router-outlet />
      </section>
    </main>
  `,
  styleUrl: './auth-shell.component.scss',
})
export class AuthShellComponent {}
