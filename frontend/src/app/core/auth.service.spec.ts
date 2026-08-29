import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthResponse } from './auth.models';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  const mockResponse: AuthResponse = {
    token: 'token-123',
    client: {
      id: 'c1',
      name: 'Fulano',
      documentId: { document: '12345678901', documentType: 'CPF' },
      planType: 'PREPAID',
      balance: 10,
      limit: null,
      active: true,
    },
  };

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('starts unauthenticated with no sessão armazenada', () => {
    expect(service.isAuthenticated()).toBe(false);
    expect(service.currentClient()).toBeNull();
  });

  it('login envia { document } e armazena token + client no sucesso', () => {
    service.login('12345678901').subscribe();

    const req = httpMock.expectOne('/api/auth');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ document: '12345678901' });
    req.flush(mockResponse);

    expect(service.isAuthenticated()).toBe(true);
    expect(service.currentClient()).toEqual(mockResponse.client);
    expect(localStorage.getItem('bcb_token')).toBe('token-123');
  });

  it('logout limpa estado e localStorage', () => {
    service.login('12345678901').subscribe();
    httpMock.expectOne('/api/auth').flush(mockResponse);

    service.logout();

    expect(service.isAuthenticated()).toBe(false);
    expect(service.currentClient()).toBeNull();
    expect(localStorage.getItem('bcb_token')).toBeNull();
  });

  it('reidrata a sessão do localStorage ao construir (persiste entre reloads)', () => {
    localStorage.setItem('bcb_token', 'stored-token');
    localStorage.setItem('bcb_client', JSON.stringify(mockResponse.client));

    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    const rehydrated = TestBed.inject(AuthService);

    expect(rehydrated.isAuthenticated()).toBe(true);
    expect(rehydrated.currentClient()).toEqual(mockResponse.client);
  });
});
