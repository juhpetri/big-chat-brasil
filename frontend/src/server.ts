import {
  AngularNodeAppEngine,
  createNodeRequestHandler,
  isMainModule,
  writeResponseToNodeResponse,
} from '@angular/ssr/node';
import express from 'express';
import { createProxyMiddleware } from 'http-proxy-middleware';
import { join } from 'node:path';

const browserDistFolder = join(import.meta.dirname, '../browser');

const app = express();
const angularApp = new AngularNodeAppEngine();

/**
 * Em dev, `ng serve` usa proxy.conf.json (Vite) pra redirecionar /api -> backend.
 * Esse mecanismo não existe fora do dev-server, então precisamos do equivalente aqui:
 * sem isso, em produção/Docker, todo fetch('/api/...') do browser bateria neste
 * próprio servidor Express (que não tem rota nenhuma pra /api) em vez do backend Spring.
 */
app.use(
  '/api',
  createProxyMiddleware({
    target: process.env['BACKEND_URL'] || 'http://localhost:8080',
    changeOrigin: true,
    // O Express tira o prefixo do mount path (`/api`) de req.url antes de passar pro
    // middleware, e o http-proxy-middleware usa esse url já sem prefixo — sem isso o
    // backend recebe POST /auth em vez de POST /api/auth e rejeita como não-autenticado.
    pathRewrite: (path) => `/api${path}`,
  }),
);

/**
 * Serve static files from /browser
 */
app.use(
  express.static(browserDistFolder, {
    maxAge: '1y',
    index: false,
    redirect: false,
  }),
);

/**
 * Handle all other requests by rendering the Angular application.
 */
app.use((req, res, next) => {
  angularApp
    .handle(req)
    .then((response) =>
      response ? writeResponseToNodeResponse(response, res) : next(),
    )
    .catch(next);
});

/**
 * Start the server if this module is the main entry point, or it is ran via PM2.
 * The server listens on the port defined by the `PORT` environment variable, or defaults to 4000.
 */
if (isMainModule(import.meta.url) || process.env['pm_id']) {
  const port = process.env['PORT'] || 4000;
  app.listen(port, (error) => {
    if (error) {
      throw error;
    }

    console.log(`Node Express server listening on http://localhost:${port}`);
  });
}

/**
 * Request handler used by the Angular CLI (for dev-server and during build) or Firebase Cloud Functions.
 */
export const reqHandler = createNodeRequestHandler(app);
