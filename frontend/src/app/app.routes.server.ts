import { RenderMode, ServerRoute } from '@angular/ssr';

export const serverRoutes: ServerRoute[] = [
  {
    // Único caminho público, sem depender de sessão — seguro pra prerender.
    path: 'login',
    renderMode: RenderMode.Prerender,
  },
  {
    // Toda sessão vive em localStorage (token Bearer), inacessível no servidor —
    // rotas atrás do authGuard não têm como ser renderizadas corretamente em SSR/prerender,
    // e prerenderizar uma rota protegida gera um HTML estático que ignora o guard em build-time.
    path: '**',
    renderMode: RenderMode.Client,
  },
];
