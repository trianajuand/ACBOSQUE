import { Component, OnDestroy, OnInit, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-auth-shell',
  imports: [RouterOutlet],
  template: `
    <main class="auth-shell">
      <section class="brand-panel">

        <!-- Decorative terminal-style corners -->
        <div class="corner tl"></div>
        <div class="corner tr"></div>
        <div class="corner bl"></div>
        <div class="corner br"></div>

        <!-- Centered hero stack -->
        <div class="hero-stack">
          <div class="hero-eyebrow">
            <span class="ping"></span>
            Plataforma de inversión
          </div>

          <h1 class="display">
            <span class="line">Acciones</span>
            <span class="line">El&nbsp;<em>Bosque</em></span>
          </h1>

          <p class="tag">Broker digital de day trading</p>

          <div class="meta-chips">
            <span>+12 mercados globales</span>
            <span class="dot"></span>
            <span>2.0% comisión</span>
          </div>

          <div class="market-clock-mini">
            <span class="live"></span>
            <strong>NYSE</strong>
            <span class="t mono">{{ reloj() }}</span>
            <span class="sep"></span>
            <em>{{ mercadoAbierto() ? 'Abierto' : 'Cerrado' }}</em>
          </div>
        </div>

        <!-- Live ticker tape -->
        <div class="ticker-tape" aria-hidden="true">
          <div class="ticker-track">
            <span class="t-row"><strong>AAPL</strong> 248.91 <em class="up">▲ 1.32%</em></span>
            <span class="t-row"><strong>NVDA</strong> 142.18 <em class="up">▲ 2.41%</em></span>
            <span class="t-row"><strong>MSFT</strong> 418.24 <em class="up">▲ 0.84%</em></span>
            <span class="t-row"><strong>TSLA</strong> 198.42 <em class="down">▼ 2.18%</em></span>
            <span class="t-row"><strong>GOOGL</strong> 182.06 <em class="up">▲ 0.42%</em></span>
            <span class="t-row"><strong>META</strong> 557.99 <em class="up">▲ 3.59%</em></span>
            <span class="t-row"><strong>AMZN</strong> 224.10 <em class="up">▲ 1.07%</em></span>
            <span class="t-row"><strong>JPM</strong> 214.78 <em class="down">▼ 0.36%</em></span>
            <span class="t-row"><strong>V</strong> 312.04 <em class="up">▲ 0.62%</em></span>
            <span class="t-row"><strong>XOM</strong> 116.50 <em class="down">▼ 0.91%</em></span>
            <!-- duplicated for seamless loop -->
            <span class="t-row" aria-hidden="true"><strong>AAPL</strong> 248.91 <em class="up">▲ 1.32%</em></span>
            <span class="t-row" aria-hidden="true"><strong>NVDA</strong> 142.18 <em class="up">▲ 2.41%</em></span>
            <span class="t-row" aria-hidden="true"><strong>MSFT</strong> 418.24 <em class="up">▲ 0.84%</em></span>
            <span class="t-row" aria-hidden="true"><strong>TSLA</strong> 198.42 <em class="down">▼ 2.18%</em></span>
            <span class="t-row" aria-hidden="true"><strong>GOOGL</strong> 182.06 <em class="up">▲ 0.42%</em></span>
            <span class="t-row" aria-hidden="true"><strong>META</strong> 557.99 <em class="up">▲ 3.59%</em></span>
            <span class="t-row" aria-hidden="true"><strong>AMZN</strong> 224.10 <em class="up">▲ 1.07%</em></span>
            <span class="t-row" aria-hidden="true"><strong>JPM</strong> 214.78 <em class="down">▼ 0.36%</em></span>
            <span class="t-row" aria-hidden="true"><strong>V</strong> 312.04 <em class="up">▲ 0.62%</em></span>
            <span class="t-row" aria-hidden="true"><strong>XOM</strong> 116.50 <em class="down">▼ 0.91%</em></span>
          </div>
        </div>

      </section>

      <section class="auth-panel page-enter">
        <router-outlet />
      </section>
    </main>
  `,
  styleUrl: './auth-shell.component.scss',
})
export class AuthShellComponent implements OnInit, OnDestroy {
  readonly reloj = signal('');
  readonly mercadoAbierto = signal(false);
  private id?: number;

  ngOnInit(): void {
    this.tick();
    this.id = window.setInterval(() => this.tick(), 1000);
  }

  ngOnDestroy(): void {
    if (this.id) window.clearInterval(this.id);
  }

  private tick(): void {
    const now = new Date();
    this.reloj.set(now.toLocaleTimeString('en-US', { hour12: false, timeZone: 'America/New_York' }) + ' EST');
    // NYSE: Mon-Fri 09:30–16:00 EST
    const nyDay = Number(new Intl.DateTimeFormat('en-US', { timeZone: 'America/New_York', weekday: 'short' }).format(now).match(/Mon|Tue|Wed|Thu|Fri/) ? 1 : 0);
    const [h, m] = new Intl.DateTimeFormat('en-US', { timeZone: 'America/New_York', hour12: false, hour: '2-digit', minute: '2-digit' }).format(now).split(':').map(Number);
    const minutes = h * 60 + m;
    this.mercadoAbierto.set(nyDay === 1 && minutes >= 9 * 60 + 30 && minutes < 16 * 60);
  }
}
