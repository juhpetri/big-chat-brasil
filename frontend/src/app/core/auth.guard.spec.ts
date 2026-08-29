import { provideRouter, Router } from '@angular/router';
import { TestBed } from '@angular/core/testing';
import { authGuard } from './auth.guard';
import { AuthService } from './auth.service';

describe('authGuard', () => {
  function runGuard() {
    return TestBed.runInInjectionContext(() =>
      authGuard({} as never, {} as never),
    );
  }

  it('libera a navegação quando autenticado', () => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: { isAuthenticated: () => true } },
      ],
    });

    expect(runGuard()).toBe(true);
  });

  it('redireciona pra /login quando não autenticado', () => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: { isAuthenticated: () => false } },
      ],
    });

    const router = TestBed.inject(Router);
    expect(runGuard()).toEqual(router.createUrlTree(['/login']));
  });
});
