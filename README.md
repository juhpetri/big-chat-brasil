# BCB — Big Chat Brasil

Plataforma de mensageria entre empresas e clientes finais, com fila de prioridade (normal/urgente), planos de pagamento pré e pós-pago, e custo transparente por mensagem. Regras de negócio completas em [docs/regras-negocio.md](docs/regras-negocio.md).

## Rodando o projeto

### Opção 1 — Docker (recomendado)

```bash
docker-compose up
```

Sobe Postgres + backend (porta 8080) + frontend (porta 4200) do zero. Acesse `http://localhost:4200`. Os dados do Postgres persistem entre restarts (volume nomeado `bcb_postgres_data`) — `docker-compose down && docker-compose up` mantém o que já foi cadastrado.

> Testado quanto à sintaxe e à lógica dos arquivos (`Dockerfile`s, `docker-compose.yml`), mas **não foi validado rodando de ponta a ponta** neste ambiente de desenvolvimento (sem Docker disponível). Se `docker-compose up` não subir de primeira, comece checando os logs de `backend` (`docker-compose logs backend`) — o erro mais provável é de conectividade com o Postgres antes do healthcheck liberar.

### Opção 2 — Dev local (sem Docker)

Pré-requisitos: Java 21, Node 22+, Postgres rodando localmente (`localhost:5432`, banco `bcb`, usuário/senha `bcb` — ou ajuste via `SPRING_DATASOURCE_*`).

```bash
npm install
npm run dev
```

Sobe backend (`mvn spring-boot:run`, porta 8080) e frontend (`ng serve`, porta 4200) juntos, com hot-reload nos dois. O frontend usa `proxy.conf.json` pra redirecionar `/api/*` pro backend — não precisa configurar CORS pra isso funcionar em dev.

### Dados de exemplo (seed)

Banco vazio, sem cliente cadastrado, não dá pra explorar nada de cara. Pra subir com dois clientes de exemplo (um PREPAID com saldo, um POSTPAID com limite), cada um já com conversa e mensagens variadas:

```bash
mvn -pl backend spring-boot:run -Dspring-boot.run.profiles=seed
```

Só roda se o banco estiver vazio (não duplica em restart). Sai no log o token de sessão pronto de cada cliente — cole em `Authorize` no Swagger (`/swagger-ui.html`) sem precisar passar por `POST /api/auth`.

## Tecnologias

- **Backend:** Java 21, Spring Boot 3.3, Spring Data JPA, PostgreSQL, springdoc-openapi (Swagger UI em `/swagger-ui.html` com o backend rodando).
- **Frontend:** Angular 22 (standalone components, signals), Angular Material (tema M3 customizado), SSR habilitado.
- **Infra:** Docker + docker-compose.

## Premissas assumidas

Os documentos de requisitos originais (`docs/requisitos-tecnicos.md`, `docs/fullstack.md`, `docs/regras-negocio.md`) se contradizem ou deixam lacuna em alguns pontos. As decisões e a justificativa completa de cada uma estão em [docs/spec.md](docs/spec.md) — resumo:

- **Docker é obrigatório**, não uma opção da "Parte 3" — é o único requisito citado como válido "para todos os perfis".
- **Fila de prioridade normal/urgente** foi o enhancement escolhido pro backend — a arquitetura assíncrona (worker + fila) que ela exige não contradiz "processamento síncrono (sem fila assíncrona)" do mínimo: essa frase define o piso, não uma proibição geral (detalhe em [spec.md §3.1.1](docs/spec.md#311-isso-conflita-com-processamento-síncrono-sem-fila-assíncrona)).
- **Status visuais de mensagem** (enviada/entregue/lida) foi o enhancement escolhido pro frontend — reaproveita o `status` que o `MessageResponse` já precisa expor pela fila de prioridade, sem endpoint novo.
- **Swagger/OpenAPI** foi o enhancement escolhido pra infraestrutura/qualidade — artefato vivo e clicável pra usar na entrevista de live-coding.
- **Client** é a empresa (PF/PJ) que autentica e manda mensagem; **Recipient** (cliente final) não é entidade própria, é campo simples em `Conversation`.

## Decisões técnicas e trade-offs

- **Autenticação:** token opaco (UUID) validado por um filtro próprio (`OncePerRequestFilter`), não Spring Security completo — trade-off consciente (sem expiração/refresh/rate limit), mais simples de explicar em live-coding.
- **Fila de prioridade:** `PriorityBlockingQueue` em memória, reidratada do Postgres no startup — sobrevive a restart sem precisar de fila implementada via query SQL ordenada a cada tick.
- **Frontend:** Angular signals em vez de NgRx — a superfície de estado (sessão, conversas, mensagens) é pequena o suficiente pra não justificar uma lib de state management.
- **CPF/CNPJ:** validação só de tamanho (11/14 dígitos), sem dígito verificador — decisão de escopo documentada, não esquecimento.

Lista completa em [docs/spec.md §6](docs/spec.md#6-decisões-técnicas-e-trade-offs-pra-citar-na-entrevista).

## Fora de escopo

WebSocket real-time, fila assíncrona real (Kafka/RabbitMQ), multi-tenancy/RBAC, internacionalização. Detalhe em [docs/spec.md §7](docs/spec.md#7-fora-de-escopo-documentado-não-esquecido).

## Estrutura do repositório

```
backend/    Spring Boot — API REST
frontend/   Angular — SPA com SSR
docs/       Specs originais do desafio + spec.md (decisões) + tasks.md (execução)
```

Acompanhamento task a task (o que está pronto, o que falta) em [docs/tasks.md](docs/tasks.md).
