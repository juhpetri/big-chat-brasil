import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth.service';

type LoginState = 'idle' | 'loading' | 'error';

@Component({
  selector: 'app-login-page',
  standalone: true,
  imports: [
    FormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './login-page.component.html',
  styleUrl: './login-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoginPageComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly document = signal('');
  protected readonly state = signal<LoginState>('idle');
  protected readonly errorMessage = signal('');

  protected readonly documentType = computed(() => {
    const digits = this.digitsOnly(this.document());
    if (digits.length === 11) return 'CPF';
    if (digits.length === 14) return 'CNPJ';
    return null;
  });

  protected submit(): void {
    const digits = this.digitsOnly(this.document());

    if (!this.documentType()) {
      this.state.set('error');
      this.errorMessage.set('Informe um CPF (11 dígitos) ou CNPJ (14 dígitos) válido.');
      return;
    }

    this.state.set('loading');
    this.authService.login(digits).subscribe({
      next: () => this.router.navigateByUrl('/conversations'),
      error: (err: HttpErrorResponse) => {
        this.state.set('error');
        this.errorMessage.set(this.messageFor(err.status));
      },
    });
  }

  private digitsOnly(value: string): string {
    return value.replace(/\D/g, '');
  }

  private messageFor(status: number): string {
    switch (status) {
      case 404:
        return 'Documento não cadastrado. Verifique o CPF/CNPJ informado.';
      case 403:
        return 'Este cliente está inativo. Entre em contato com o suporte.';
      default:
        return 'Não foi possível entrar agora. Tente novamente em instantes.';
    }
  }
}
