import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  let httpClient: HttpClient;
  let httpMock: HttpTestingController;

  function setup(token: string | null): void {
    localStorage.clear();
    if (token) {
      localStorage.setItem('bcb_token', token);
    }
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    });
    httpClient = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  }

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('anexa Authorization em chamadas /api quando existe token', () => {
    setup('abc123');
    httpClient.get('/api/conversations').subscribe();
    const req = httpMock.expectOne('/api/conversations');
    expect(req.request.headers.get('Authorization')).toBe('Bearer abc123');
  });

  it('não anexa header quando não há token', () => {
    setup(null);
    httpClient.get('/api/conversations').subscribe();
    const req = httpMock.expectOne('/api/conversations');
    expect(req.request.headers.has('Authorization')).toBe(false);
  });

  it('não anexa header em chamadas fora de /api', () => {
    setup('abc123');
    httpClient.get('/assets/logo.png').subscribe();
    const req = httpMock.expectOne('/assets/logo.png');
    expect(req.request.headers.has('Authorization')).toBe(false);
  });

  it('desloga e manda pro /login quando uma chamada autenticada volta 401', () => {
    setup('token-expirado');
    const router = TestBed.inject(Router);
    const navigateSpy = vi.spyOn(router, 'navigateByUrl');

    httpClient.get('/api/conversations').subscribe({ error: () => {} });
    httpMock.expectOne('/api/conversations').flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(localStorage.getItem('bcb_token')).toBeNull();
    expect(navigateSpy).toHaveBeenCalledWith('/login');
  });

  it('não tenta deslogar num 401 de chamada sem token (não deve acontecer, mas não pode entrar em loop)', () => {
    setup(null);
    const router = TestBed.inject(Router);
    const navigateSpy = vi.spyOn(router, 'navigateByUrl');

    httpClient.get('/api/conversations').subscribe({ error: () => {} });
    httpMock.expectOne('/api/conversations').flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(navigateSpy).not.toHaveBeenCalled();
  });
});
