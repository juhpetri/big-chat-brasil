import { DatePipe, DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, afterNextRender, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { RouterLink } from '@angular/router';
import { PlanType } from '../../core/auth.models';
import { AuthService } from '../../core/auth.service';
import { AccountService } from '../../core/account.service';

@Component({
  selector: 'app-account-page',
  standalone: true,
  imports: [
    DatePipe,
    DecimalPipe,
    FormsModule,
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './account-page.component.html',
  styleUrl: './account-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AccountPageComponent {
  private readonly authService = inject(AuthService);
  protected readonly accountService = inject(AccountService);

  protected readonly client = this.authService.currentClient;

  protected readonly creditAmount = signal<number | null>(null);
  protected readonly newLimit = signal<number | null>(null);
  protected readonly actionError = signal('');

  protected readonly showConvertForm = signal(false);
  protected readonly convertInitialValue = signal<number | null>(null);

  protected readonly targetPlanType = computed<PlanType>(() =>
    this.client()?.planType === 'PREPAID' ? 'POSTPAID' : 'PREPAID',
  );

  // Dados do dashboard vêm só de /transactions — sem endpoint novo. Uma transação DEBIT sempre
  // custa 0.25 (NORMAL) ou 0.50 (URGENT), então dá pra recuperar a prioridade da mensagem a
  // partir do valor cobrado, sem precisar cruzar com o histórico de mensagens.
  private readonly debitTransactions = computed(() =>
    this.accountService.transactions().filter((t) => t.type === 'DEBIT'),
  );

  protected readonly totalSpent = computed(() =>
    this.debitTransactions().reduce((sum, t) => sum + t.amount, 0),
  );

  protected readonly messageCount = computed(() => this.debitTransactions().length);

  protected readonly averageCost = computed(() => {
    const count = this.messageCount();
    return count === 0 ? 0 : this.totalSpent() / count;
  });

  protected readonly hasDashboardData = computed(() => this.accountService.transactions().length > 0);

  protected readonly spendChartBars = computed(() => {
    const byDay = new Map<string, number>();
    for (const t of this.debitTransactions()) {
      const day = t.timestamp.slice(0, 10);
      byDay.set(day, (byDay.get(day) ?? 0) + t.amount);
    }

    const days = [...byDay.entries()].sort(([a], [b]) => a.localeCompare(b)).slice(-14);
    const max = Math.max(1, ...days.map(([, amount]) => amount));

    const chartWidth = 320;
    const chartHeight = 110;
    const gap = 4;
    const barWidth = days.length > 0 ? (chartWidth - gap * (days.length - 1)) / days.length : 0;

    return days.map(([day, amount], index) => {
      const barHeight = (amount / max) * (chartHeight - 4);
      return {
        x: index * (barWidth + gap),
        y: chartHeight - barHeight,
        width: Math.max(barWidth, 1),
        height: barHeight,
        label: day.slice(8) + '/' + day.slice(5, 7),
        amount,
      };
    });
  });

  // Donut normal/urgente desenhado com o truque clássico de stroke-dasharray + stroke-dashoffset
  // num círculo (evita montar um path de arco SVG à mão pra dois segmentos).
  protected readonly priorityDonut = computed(() => {
    const debits = this.debitTransactions();
    const urgentCount = debits.filter((t) => t.amount > 0.3).length;
    const normalCount = debits.length - urgentCount;
    const total = debits.length;
    const circumference = 2 * Math.PI * 45;

    if (total === 0) {
      return { circumference, normalLength: 0, urgentLength: 0, normalPct: 0, urgentPct: 0, normalCount: 0, urgentCount: 0 };
    }

    return {
      circumference,
      normalLength: (normalCount / total) * circumference,
      urgentLength: (urgentCount / total) * circumference,
      normalPct: Math.round((normalCount / total) * 100),
      urgentPct: Math.round((urgentCount / total) * 100),
      normalCount,
      urgentCount,
    };
  });

  constructor() {
    afterNextRender(() => this.accountService.loadTransactions());
  }

  protected submitCredit(): void {
    const amount = this.creditAmount();
    if (!amount || amount <= 0) {
      return;
    }
    this.actionError.set('');
    this.accountService.addCredit(amount).subscribe({
      next: () => {
        this.creditAmount.set(null);
        this.accountService.loadTransactions();
      },
      error: (err: HttpErrorResponse) => this.actionError.set(this.messageFor(err)),
    });
  }

  protected submitLimit(): void {
    const limit = this.newLimit();
    if (limit === null || limit < 0) {
      return;
    }
    this.actionError.set('');
    this.accountService.adjustLimit(limit).subscribe({
      next: () => this.newLimit.set(null),
      error: (err: HttpErrorResponse) => this.actionError.set(this.messageFor(err)),
    });
  }

  protected submitConvertPlan(): void {
    const initialValue = this.convertInitialValue();
    if (initialValue === null || initialValue < 0) {
      return;
    }
    this.actionError.set('');
    this.accountService.convertPlan(this.targetPlanType(), initialValue).subscribe({
      next: () => {
        this.showConvertForm.set(false);
        this.convertInitialValue.set(null);
        this.accountService.loadTransactions();
      },
      error: (err: HttpErrorResponse) => this.actionError.set(this.messageFor(err)),
    });
  }

  private messageFor(err: HttpErrorResponse): string {
    return err.error?.message ?? 'Não foi possível concluir a operação agora.';
  }
}
