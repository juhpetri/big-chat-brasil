# Tasks — BCB (Big Chat Brasil)

> Cada task tem: **Objetivo**, **Depende de**, **Critério de pronto** e uma **STE** (Sequência Técnica de Execução) — os passos técnicos, em ordem, pra sair do zero até o critério de pronto. Decisões de escopo e trade-offs estão em [spec.md](spec.md); aqui é só execução.

## Status geral

| Fase | Tasks | Situação |
|---|---|---|
| 0. Fundação | TASK-00 | ✅ concluída |
| A. Persistência & domínio | TASK-01, TASK-02 | ✅ concluída |
| B. Cliente & autenticação | TASK-03 a TASK-05 | ✅ concluída |
| C. Conversas & mensagens | TASK-06 a TASK-11 | ✅ concluída |
| D. Frontend | TASK-12 a TASK-17 | ✅ concluída |
| E. Integração, Docker, docs | TASK-18 a TASK-21 | ✅ concluída (TASK-19: código pronto, `docker-compose up` não testado neste ambiente) |
| Stretch (só se sobrar tempo) | TASK-22 a TASK-25 | ✅ concluída |

---

## Fase 0 — Fundação

### TASK-00 — Monorepo com backend e frontend orquestrados ✅
**Objetivo:** ter Spring Boot e Angular rodando lado a lado em dev, com tema visual do BCB aplicado.
**Critério de pronto:** `npm run dev` sobe as duas apps; `/api/ping` acessível via proxy do Angular; tema Material com paleta BCB compilando.

**STE (já executada):**
1. `pom.xml` raiz (parent Maven) + `backend/pom.xml` com `spring-boot-starter-web`.
2. `WebConfig` com CORS liberado pra `localhost:4200`; `application.yml` na porta 8080.
3. `frontend/proxy.conf.json` redirecionando `/api` → `localhost:8080`; `npm start` já usa `--proxy-config`.
4. `package.json` raiz com `concurrently` orquestrando `dev:backend` + `dev:frontend`.
5. `ng add @angular/material`; tema M3 customizado gerado a partir dos hex da identidade (`ng generate @angular/material:m3-theme --primary-color="#F5D130" --tertiary-color="#FF5C4D"`) em `frontend/src/theme-colors.scss`, aplicado em `styles.scss` com `theme-type: dark` e tipografia Poppins/Inter.

---

## Fase A — Persistência & domínio base

### TASK-01 — Modelagem do domínio ✅
**Objetivo:** transformar o domínio descrito em [spec.md §4](spec.md#4-domínio) em decisões concretas de entidade antes de escrever código (enums, tipos, chaves).
**Depende de:** —
**Critério de pronto:** um diagrama/lista curta (pode ser comentário no código ou neste doc) com todas as entidades, campos e tipos definidos — sem ambiguidade pra começar a codar.

**STE:**
1. Definir enums: `DocumentType {CPF, CNPJ}`, `PlanType {PREPAID, POSTPAID}`, `MessagePriority {NORMAL, URGENT}`, `MessageStatus {QUEUED, PROCESSING, SENT, DELIVERED, READ, FAILED}`, `SenderType {CLIENT, USER}`.
2. Decidir tipo de ID: UUID (evita expor sequência incremental, fácil de gerar no `POST /auth`/`POST /messages` sem round-trip).
3. Confirmar que `Recipient` **não** vira entidade (campos `recipientId`/`recipientName` direto em `Conversation`, ver [spec.md §2.3](spec.md#23-quem-é-cliente)).
4. Esboçar as 4 tabelas finais (Client, Conversation, Message, Session) com FKs.

### TASK-02 — PostgreSQL + Spring Data JPA ✅
**Objetivo:** projeto persiste em banco real, não em memória.
**Depende de:** TASK-01
**Critério de pronto:** `mvn spring-boot:run` com um Postgres local (ou container) sobe sem erro, o schema é criado pelos changesets do Liquibase (`db/changelog`), Hibernate confere com `ddl-auto: validate`, e uma query manual confirma o schema. (Decisão original era `ddl-auto: update` sem Liquibase — revertida depois; ver [spec.md §6](spec.md#6-decisões-técnicas-e-trade-offs-pra-citar-na-entrevista).)

**STE:**
1. `backend/pom.xml`: `spring-boot-starter-data-jpa`, `postgresql` e `liquibase-core` (Spring Boot detecta a dependência no classpath e roda o Liquibase sozinho na subida — não precisa de `@EnableLiquibase` nem config extra além do changelog).
2. `application.yml`: datasource via env var com default local (`SPRING_DATASOURCE_URL`/`USERNAME`/`PASSWORD`, já compatível com o que o `docker-compose.yml` da TASK-19 injeta — sem precisar de um `application-docker.yml` separado); `spring.jpa.hibernate.ddl-auto: validate` (não `update` — quem cria/altera schema é o Liquibase, Hibernate só confere que bate); `spring.liquibase.change-log: classpath:db/changelog/db.changelog-master.yaml`.
3. `db/changelog/db.changelog-master.yaml` com `includeAll` apontando pra `db/changelog/changes/`; um changeset por tabela em SQL puro (`--liquibase formatted sql`, um `CREATE TABLE` por arquivo, prefixo numérico pra ordem — `001-create-client-table.sql` → `005-create-transaction-table.sql`, respeitando as FKs).
4. Criar entidades JPA (`@Entity`) pra Client, Conversation, Message, Session conforme o detalhamento de campos/tipos em [spec.md §4.1](spec.md#41-detalhamento-de-campos-e-tipos-task-01), em `com.bcb.client`, `com.bcb.conversation`, `com.bcb.message`, `com.bcb.auth`:
   - Strings como `String`/`TEXT` (`@Column(columnDefinition = "TEXT")`), dinheiro como `BigDecimal`/`NUMERIC(10,2)` (`@Column(precision = 10, scale = 2)`) — as duas anotações são obrigatórias com `ddl-auto: validate`, senão o Hibernate assume um default (`varchar(255)`/`numeric(19,2)`) que diverge do changeset e a aplicação não sobe.
   - Enums com `@Enumerated(EnumType.STRING)` + `@Column(columnDefinition = "TEXT")`.
   - `Conversation.client` e `Message.conversation` como `@ManyToOne(fetch = FetchType.LAZY)` (objeto real, não UUID cru) — N+1 tratado depois, na query de listagem (TASK-06/TASK-10), não evitado aqui.
   - `id` com `@GeneratedValue(strategy = GenerationType.UUID)`, exceto `Session.token`, que é `@Id` sem `@GeneratedValue` (gerado pela aplicação no `AuthService`, TASK-04).
5. Criar `ClientRepository`, `ConversationRepository`, `MessageRepository` (`JpaRepository<Entity, UUID>`) e `SessionRepository` (`JpaRepository<Session, String>`, já que o token é a PK).
6. Rodar local com `docker run postgres` avulso pra validar antes do compose completo existir; confirmar que o Liquibase roda os changesets no log de boot e que o Hibernate sobe sem erro de `validate`.

---

## Fase B — Cliente & autenticação

### TASK-03 — CRUD de clientes ✅
**Objetivo:** atender "API de CRUD para clientes" do mínimo exigido.
**Depende de:** TASK-02
**Critério de pronto:** `POST /api/clients` cria um cliente com plano; `GET /api/clients/{id}` retorna saldo/limite atual; documentId duplicado retorna erro claro (409).

**STE:**
1. `com.bcb.client.dto`: `CreateClientRequest` (name, documentId, documentType, planType, saldo/limite inicial), `ClientResponse` (espelha o `client` de `AuthResponse` em `fullstack.md`).
2. `ClientService`: valida documentId único, formato básico de CPF/CNPJ (11/14 dígitos — sem validar dígito verificador, fora de escopo), cria com `active = true`.
3. `ClientController`: `POST /api/clients`, `GET /api/clients/{id}`.
4. `GlobalExceptionHandler` (criar já aqui, vai crescer nas próximas tasks): trata `DocumentAlreadyExistsException` → 409 com payload `{error, message}`.

### TASK-04 — Autenticação simples (POST /auth) ✅
**Objetivo:** cliente se identifica por CPF/CNPJ e recebe um token.
**Depende de:** TASK-03
**Critério de pronto:** `POST /api/auth` com documentId cadastrado retorna `{token, client}`; documentId não cadastrado retorna 404; cliente inativo retorna 403.

**STE:**
1. `com.bcb.auth`: entidade `Session` (token UUID, clientId, createdAt) já criada na TASK-02 — usar aqui.
2. `AuthService.authenticate(documentId, documentType)`: busca `Client`, valida `active`, gera token, persiste `Session`, retorna `AuthResponse`.
3. `AuthController`: `POST /api/auth`.
4. Exceptions novas no `GlobalExceptionHandler`: `ClientNotFoundException` → 404, `ClientInactiveException` → 403.

### TASK-05 — Filtro de autenticação (Bearer token) ✅
**Objetivo:** proteger `/api/conversations/**` e `/api/messages/**` exigindo token válido.
**Depende de:** TASK-04
**Critério de pronto:** requisição sem header `Authorization` ou com token inválido/expirado nesses endpoints retorna 401; com token válido, o `clientId` da sessão fica disponível pro controller sem precisar passar por parâmetro.

**STE:**
1. `com.bcb.auth.AuthTokenFilter extends OncePerRequestFilter`: extrai `Bearer {token}`, busca `Session`, se válida injeta `clientId` num `AuthenticatedClient` (request attribute ou `ThreadLocal` simples — ver trade-off em [spec.md §6](spec.md#6-decisões-técnicas-e-trade-offs-pra-citar-na-entrevista)).
2. Registrar o filtro no `WebConfig` (ou `SecurityFilterChain` mínimo, decidir na hora conforme o que for mais simples de ligar sem trazer Spring Security inteiro).
3. Whitelist explícita: `/api/auth`, `/api/clients` (POST), `/api/ping` não passam pelo filtro.
4. `@ExceptionHandler` pra token ausente/inválido → 401 com payload padronizado.

---

## Fase C — Conversas & mensagens

### TASK-06 — Entidades Conversation e Message ✅
**Objetivo:** persistir conversas e mensagens conforme TASK-01.
**Depende de:** TASK-02
**Critério de pronto:** repositórios funcionando, com um teste manual (Postman/curl) inserindo uma conversa e mensagens associadas.

**STE:**
1. Entidades `Conversation` (clientId, recipientId, recipientName) e `Message` (conversationId, content, priority, status, cost, sentByType, queuedAt, processedAt) em `com.bcb.conversation`/`com.bcb.message`.
2. `ConversationRepository.findByClient_IdOrderByLastMessageAtDesc(...)` — decidir se "última mensagem" é campo derivado (query) ou desnormalizado na própria `Conversation` (mais simples e rápido pra listar — escolher isso). O `_` navega o relacionamento `Conversation.client` até `Client.id`, não um campo `clientId` solto (ver TASK-02/spec.md §6).
3. `MessageRepository.findByConversation_IdOrderByQueuedAtAsc(...)`.

### TASK-07 — Regras financeiras (BillingService) ✅
**Objetivo:** implementar a validação de saldo/limite descrita em `regras-negocio.md §3`.
**Depende de:** TASK-03, TASK-06
**Critério de pronto:** cliente pré-pago sem saldo suficiente não consegue enviar mensagem (erro claro, sem debitar); cliente pós-pago que excede o limite mensal também bloqueia; os exemplos numéricos do `regras-negocio.md` (R$10 → 5 msgs normais → R$8,75; limite R$50, já usou R$40 → 10 msgs → resta R$7,50) batem exatamente.

**STE:**
1. `com.bcb.billing.BillingService.validateAndCharge(client, priority)`:
   - `PREPAID`: `if (client.balance < cost) throw InsufficientBalanceException; client.balance -= cost;`
   - `POSTPAID`: `if (client.monthlyUsage + cost > client.monthlyLimit) throw LimitExceededException; client.monthlyUsage += cost;`
2. `cost` vem de `priority == URGENT ? 0.50 : 0.25` (constante em `MessagePriority` ou `BillingService`).
3. Persistir mudança no `Client` na mesma transação do `Message` (usar `@Transactional` no service que orquestra envio, TASK-09) — nunca debitar sem garantir que a mensagem foi de fato enfileirada.
4. `InsufficientBalanceException` → 402 (ou 400 com código específico), `LimitExceededException` → 400.
5. Teste manual reproduzindo os dois exemplos numéricos do doc de regras de negócio.

### TASK-08 — Fila de mensagens com prioridade ✅
**Objetivo:** implementar o enhancement escolhido em [spec.md §3.1](spec.md#31-backend--escolhido-fila-com-prioridade-normalurgente).
**Depende de:** TASK-06
**Critério de pronto:** enviando 3 mensagens normais e depois 1 urgente, a urgente é processada (`status → SENT`) antes das normais que ainda estavam `QUEUED`; mensagens já `PROCESSING` não são interrompidas.

> ⚠️ Esta task introduz processamento assíncrono (worker em background) de propósito — isso **não** contradiz "processamento síncrono (sem fila assíncrona)" do mínimo de `requisitos-tecnicos.md`. Aquela frase escopa o **piso** (Parte 1); esta task é o enhancement da Parte 2, que só demonstra prioridade de verdade se as mensagens puderem se acumular antes de processar. Justificativa completa em [spec.md §3.1.1](spec.md#311-isso-conflita-com-processamento-síncrono-sem-fila-assíncrona).

**STE:**
1. `com.bcb.message.MessageQueueService`: estrutura em memória thread-safe (`PriorityBlockingQueue<QueuedMessage>` com `Comparator` por `(priority desc, queuedAt asc)`), reidratada a partir do banco no startup (`@PostConstruct` carregando mensagens `QUEUED`/`PROCESSING`) — garante sobrevivência a restart sem reimplementar fila em SQL.
2. `enqueue(Message)`: insere na fila em memória + persiste `status = QUEUED` no banco.
3. `MessageQueueWorker` (`@Scheduled(fixedDelay = ...)`, roda em background): faz `poll()` da fila, marca `PROCESSING` → simula processamento → marca `SENT` (e opcionalmente `DELIVERED` num segundo tick, pra dar ao frontend uma transição de status real de observar).
4. Sincronização: usar os métodos atômicos da `PriorityBlockingQueue` — evitar lock manual desnecessário.

### TASK-09 — POST /messages ✅
**Objetivo:** endpoint de envio, unindo billing (TASK-07) + fila (TASK-08).
**Depende de:** TASK-05, TASK-07, TASK-08
**Critério de pronto:** resposta bate exatamente com `SendMessageResponse` de `fullstack.md` (`id, status: 'queued', timestamp, estimatedDelivery, cost, currentBalance?`).

**STE:**
1. `com.bcb.message.dto.SendMessageRequest/SendMessageResponse` espelhando o contrato.
2. `MessageService.send(clientId, request)` (`@Transactional`): resolve ou cria `Conversation` (se `recipientId` novo), chama `BillingService`, persiste `Message`, chama `MessageQueueService.enqueue`, monta resposta.
3. `MessageController`: `POST /api/messages`, `clientId` vem do filtro de auth (TASK-05), não do body.
4. `estimatedDelivery`: calcular como `now + N segundos` conforme intervalo do worker (TASK-08) — não precisa ser exato, é estimativa.

### TASK-10 — GET /conversations e GET /conversations/{id}/messages ✅
**Objetivo:** listar conversas do cliente autenticado e histórico de mensagens.
**Depende de:** TASK-05, TASK-06
**Critério de pronto:** resposta bate com `ConversationResponse[]` e `MessageResponse[]` de `fullstack.md`; conversa de outro cliente retorna 403/404 (nunca vaza dado entre clientes).

**STE:**
1. `ConversationController.GET /api/conversations`: pede a lista de `ConversationSummary` pro `ConversationService` (filtrado por `clientId` da sessão, ordenado por última mensagem desc) e a contagem de não lidas pro `MessageService`, e monta o `ConversationResponse` final juntando os dois — essa montagem fica no controller, não em nenhum dos dois services, pra evitar um ciclo de dependência entre eles (ver [spec.md §6](spec.md#6-decisões-técnicas-e-trade-offs-pra-citar-na-entrevista)).
2. `ConversationController.GET /api/conversations/{id}/messages`: chama `ConversationService.assertOwnership` (valida que a conversa pertence ao `clientId` da sessão, lança `ConversationNotFoundException` → 404 se não) antes de pedir as mensagens pro `MessageService`.
3. `unreadCount`: mensagens `sentByType = CLIENT` com `status != READ`, contadas via `MessageRepository` com `GROUP BY conversation_id` (uma query agrupada pra todas as conversas da lista, não uma por conversa).

### TASK-11 — Tratamento de erros centralizado ✅
**Objetivo:** todo erro de negócio (não só os das tasks anteriores) sai com payload consistente.
**Depende de:** TASK-03 a TASK-10
**Critério de pronto:** todo `4xx` do backend segue o mesmo formato `{error: string, message: string}`; erros não mapeados caem num handler genérico 500 sem vazar stacktrace.

**STE:**
1. Revisar `GlobalExceptionHandler` (criado incrementalmente nas tasks anteriores) e consolidar todos os `@ExceptionHandler` num único lugar.
2. Adicionar handler genérico `Exception.class` → 500 com mensagem genérica (nunca a exception real em produção).
3. Conferir que todo `throw` das fases B/C usa uma exception mapeada — nenhuma `RuntimeException` solta chegando ao cliente.

---

## Fase D — Frontend

### TASK-12 — Base de sessão no Angular ✅
**Objetivo:** infraestrutura de auth reaproveitável por todas as telas.
**Depende de:** TASK-04, TASK-05
**Critério de pronto:** token persiste entre reloads (localStorage), interceptor injeta `Authorization: Bearer` em toda chamada pra `/api`, guard bloqueia rotas protegidas sem sessão.

**STE:**
1. `frontend/src/app/core/auth.service.ts`: signal `currentClient`, métodos `login(documentId, documentType)`, `logout()`, `isAuthenticated` computed.
2. `frontend/src/app/core/auth.interceptor.ts` (functional interceptor, `withInterceptors` em `app.config.ts`): anexa header se houver token.
3. `frontend/src/app/core/auth.guard.ts` (functional guard): redireciona pra `/login` se `!isAuthenticated()`.
4. Registrar interceptor/guard em `app.config.ts` e `app.routes.ts`.

### TASK-13 — Tela de identificação (login) ✅
**Objetivo:** primeira tela do fluxo — CPF/CNPJ → token.
**Depende de:** TASK-12
**Critério de pronto:** usuário digita CPF ou CNPJ, sistema identifica o tipo automaticamente (por tamanho), chama `POST /auth`, trata erro 404 (não cadastrado) e 403 (inativo) com mensagem clara, redireciona pra lista de conversas em caso de sucesso.

**STE:**
1. `frontend/src/app/features/auth/login-page.component.ts` (standalone): `mat-form-field` com máscara/validação de CPF (11 dígitos) ou CNPJ (14 dígitos).
2. Estados de UI: `idle | loading | error` (signal), mensagem de erro específica por status HTTP.
3. Usar a identidade visual do BCB (fundo preto, CTA amarelo pill, tipografia Poppins) — primeira tela real a sair do placeholder do Angular CLI.
4. Rota `/login` pública em `app.routes.ts`.

### TASK-14 — Lista de conversas ✅
**Objetivo:** segunda tela — conversas do cliente autenticado.
**Depende de:** TASK-10, TASK-12
**Critério de pronto:** lista carrega via `GET /conversations`, mostra nome do destinatário, prévia da última mensagem, hora e badge de não lidas; states de loading/empty/error tratados.

**STE:**
1. `frontend/src/app/core/conversations.service.ts`: `httpResource`/signal + `GET /api/conversations`.
2. `frontend/src/app/features/conversations/conversation-list.component.ts`: lista com `@for`, item clicável navegando pra `/chat/{id}`.
3. Estado vazio ("nenhuma conversa ainda") e estado de erro (retry) — não deixar tela em branco silenciosa.

### TASK-15 — Tela de chat ✅
**Objetivo:** histórico + envio de mensagem.
**Depende de:** TASK-09, TASK-10, TASK-14
**Critério de pronto:** abrir uma conversa carrega o histórico (`GET /conversations/{id}/messages`); campo de texto + seletor normal/urgente envia via `POST /messages`; mensagem aparece na tela imediatamente (otimista) com status `queued`, custo visível.

**STE:**
1. `frontend/src/app/features/chat/chat-page.component.ts`: carrega histórico no `ngOnInit`/`afterNextRender` (mesmo padrão do ping em `app.ts` — evitar chamada durante SSR/prerender).
2. Componente de bolha de mensagem reaproveitando o padrão visual de `identidade-visual.html` (`.msg`, `.badge.normal/.badge.urgent`).
3. Toggle normal/urgente no formulário de envio, mostrando o custo (R$0,25/R$0,50) antes de enviar.
4. Envio otimista: insere a mensagem na lista local com status `queued` antes da resposta confirmar, reconcilia com a resposta real do `POST /messages`.
5. Tratar erro de saldo insuficiente/limite excedido (TASK-07) com mensagem específica, não genérica.

### TASK-16 — Status visuais de mensagem ✅
**Objetivo:** enhancement de frontend escolhido em [spec.md §3.3](spec.md#33-frontend--escolhido-status-visuais-de-mensagem-enviada-entregue-lida).
**Depende de:** TASK-08, TASK-15
**Critério de pronto:** badge de status muda sozinho na tela (sem reload manual) conforme o worker do backend avança a mensagem de `queued` → `sent` → `delivered`.

**STE:**
1. No `chat-page.component.ts`, após enviar/ao ter mensagens `QUEUED`/`PROCESSING` na tela, iniciar um polling leve (`interval(2000)` do RxJS, ou `setTimeout` recursivo) que rechama `GET /conversations/{id}/messages` só enquanto houver mensagem em status não-terminal.
2. Parar o polling quando todas as mensagens visíveis estiverem em status terminal (`sent`, `delivered`, `read`, `failed`) — não deixar polling infinito rodando.
3. Componente de badge (`message-status-badge.component.ts`) com cor/ícone por status, reaproveitando os tokens `--bcb-yellow`/`--bcb-urgent` de `styles.scss`.

### TASK-17 — Responsividade e identidade visual aplicada ✅
**Objetivo:** layout mobile/desktop real, substituindo de vez o placeholder do Angular CLI.
**Depende de:** TASK-13 a TASK-16
**Critério de pronto:** lista de conversas + chat em duas colunas no desktop, uma coluna (navegação entre lista e chat) no mobile; `app.html`/`app.ts` não têm mais nenhum resquício do template padrão do `ng new`.

**STE:**
1. Remover o conteúdo placeholder de `app.html` (logo do Angular, links de doc) — vira só `<router-outlet>` com um shell (nav/topbar da marca BCB).
2. Breakpoints via Angular CDK (`BreakpointObserver`) ou CSS media queries simples — decidir conforme complexidade real necessária (provavelmente CSS puro basta pra esse layout).
3. Revisar as telas das tasks 13–16 num viewport mobile de verdade (não só redimensionar a janela) antes de marcar como pronta.

---

## Fase E — Integração, Docker, documentação

### TASK-18 — Estados de UI consistentes fim-a-fim ✅
**Objetivo:** nenhuma chamada HTTP do frontend fica sem tratamento de loading/erro/sucesso.
**Depende de:** TASK-13 a TASK-17
**Critério de pronto:** desligar o backend propositalmente e navegar pelo app inteiro — nenhuma tela quebra ou fica em branco; todas mostram um estado de erro compreensível.

**STE:**
1. Checklist manual por tela (login, lista, chat, envio) confirmando os 3 estados.
2. Padronizar como erros HTTP viram mensagem pro usuário (mapear os payloads `{error, message}` da TASK-11 pra texto em pt-BR nas telas).

### TASK-19 — Docker e docker-compose ⚠️ (código pronto, `docker-compose up` não testado neste ambiente — sem Docker disponível)
**Objetivo:** atender o requisito obrigatório de `requisitos-tecnicos.md` — `git clone && docker-compose up` funcionando.
**Depende de:** TASK-02 (Postgres), resto do backend/frontend funcional
**Critério de pronto:** `docker-compose up` sobe Postgres + backend + frontend do zero, app acessível no browser, dados persistem entre restarts do container do backend.

**STE:**
1. `backend/Dockerfile`: multi-stage (build com Maven + JDK, runtime só com JRE).
2. `frontend/Dockerfile`: build do Angular (`ng build`) + serve via o `server.ts`/Express SSR que já existe no projeto (reaproveitar, não trocar por Nginx à toa).
3. `docker-compose.yml` na raiz: serviços `postgres` (com volume nomeado), `backend` (depende de `postgres`, profile `docker` da TASK-02), `frontend` (depende de `backend`), variáveis de ambiente (`DB_HOST`, `DB_PASSWORD` etc. via `.env`).
4. Testar `docker-compose down && docker-compose up` — confirmar que dado sobrevive (volume do Postgres).

### TASK-20 — Swagger/OpenAPI ✅
**Objetivo:** enhancement escolhido em [spec.md §3.4](spec.md#34-parte-3-infraestruturaqualidade--escolhido-swaggeropenapi).
**Depende de:** TASK-03 a TASK-11 (endpoints existentes)
**Critério de pronto:** `/swagger-ui.html` lista os endpoints (6 do mínimo + os 5 de administração da TASK-23, ver [spec.md §5](spec.md#5-contrato-de-api)) com request/response de exemplo, sem precisar de Postman pra testar manualmente. Endpoints públicos (`POST /auth`, `POST /clients`, `GET /ping`) aparecem sem exigir o cadeado de autenticação; os demais herdam o esquema `bearerAuth` global.

**STE:**
1. Adicionar `springdoc-openapi-starter-webmvc-ui` no `pom.xml`.
2. Anotar DTOs principais com `@Schema` (descrição curta nos campos menos óbvios: `priority`, `planType`), controllers com `@Tag`/`@Operation`/`@ApiResponses` (status esperados por endpoint, incluindo os erros de negócio mapeados na TASK-11).
3. Configuração básica (título, descrição, `SecurityScheme` bearer) num `OpenApiConfig`; `@SecurityRequirements` vazio nos 3 endpoints públicos pra sobrescrever o esquema global.

### TASK-21 — README.md ✅
**Objetivo:** entrega final legível por quem nunca viu o projeto.
**Depende de:** TASK-19
**Critério de pronto:** alguém de fora clona o repo, segue o README, roda `docker-compose up`, consegue usar o app sem perguntar nada.

**STE:**
1. Descrição do projeto (2-3 linhas) + link pra `docs/regras-negocio.md`.
2. Seção "Premissas assumidas" — puxar direto de [spec.md §2](spec.md#2-conflitos-entre-os-documentos-e-como-resolvi) e [spec.md §6](spec.md#6-decisões-técnicas-e-trade-offs-pra-citar-na-entrevista), reescritas em tom de leitura corrida.
3. Tecnologias utilizadas (lista curta, sem repetir o óbvio do `pom.xml`/`package.json`).
4. Passo a passo: `docker-compose up` (produção-like) e também rodar em dev puro (`npm run dev`, já documentado informalmente nesta conversa).

---

## Stretch — só se sobrar tempo, nesta ordem de prioridade

### TASK-22 — Testes automatizados chave ✅
**Objetivo:** cobrir a lógica com mais risco de regressão silenciosa.
**Critério de pronto:** escopo original (`MessageQueueService`, `BillingService` no backend; `AuthService`/interceptor no frontend) cumprido e depois ampliado pra todo `Service` do backend com dependência — 9 classes de teste, 39 testes: `AuthService`, `SessionService`, `ClientService`, `ConversationService`, `MessageService`, `MessageQueueService`, `MessageQueueWorker`, `TransactionService` (todos com `@ExtendWith(MockitoExtension.class)` e `@Mock`/`@InjectMocks` pros colaboradores) e `BillingService` (sem mock — não tem dependência externa). No frontend, `auth.service.spec.ts`, `auth.guard.spec.ts`, `auth.interceptor.spec.ts`.

**STE:**
1. Um `@Mock` por dependência do service (repository e/ou outro service), `@InjectMocks` pro service sob teste — nunca instanciar o service manualmente num inicializador de campo do teste (o `MockitoExtension` só popula os `@Mock` durante o ciclo de vida do JUnit, depois da construção da classe de teste; um inicializador de campo rodaria antes, com os mocks ainda `null`).
2. Por service, pelo menos: um caminho de sucesso, e um teste por exceção de negócio que ele pode lançar (não só o "happy path").
3. `BillingServiceTest` reproduz os dois exemplos numéricos do `regras-negocio.md` (mesmo teste desde a TASK-07) — não muda com a expansão de escopo.

### TASK-23 — Administração completa de planos ✅
**Objetivo:** cobrir `regras-negocio.md §4` (Administração) por inteiro.
**Critério de pronto:** endpoints pra adicionar crédito (pré-pago), ajustar limite (pós-pago), converter entre planos com tratamento de saldo/consumo residual, e consultar histórico de transações — com uma tela simples de "conta" no frontend.

### TASK-24 — Segunda opção de frontend (filtros/busca) ✅
**Objetivo:** a opção de Parte 2 (frontend) que não foi escolhida em [spec.md §3.3](spec.md#33-frontend--escolhido-status-visuais-de-mensagem-enviada-entregue-lida).
**Critério de pronto:** campo de busca filtrando o histórico de mensagens por texto, client-side (sem endpoint novo, dado que o histórico já é carregado inteiro).

### TASK-25 — Dashboard na tela de conta ✅
**Objetivo:** dar mais substância à tela de conta (TASK-23) com uma visão de uso, não só o formulário de crédito/limite e a lista crua de transações.
**Depende de:** TASK-23
**Critério de pronto:** 3 KPIs (total gasto, mensagens enviadas, custo médio), gráfico de gasto por dia e gráfico normal/urgente, todos derivados de `GET /clients/{id}/transactions` — sem endpoint novo. Estado vazio quando não há transações ainda.

**STE:**
1. `account-page.component.ts`: `computed` signals sobre `accountService.transactions()` — `totalSpent`, `messageCount`, `averageCost`, `spendChartBars` (agrupado por dia, últimos 14 dias com movimento), `priorityDonut` (deriva NORMAL/URGENT do valor da transação `DEBIT`: `0.25` vs `0.50` — nenhuma transação carrega a prioridade explicitamente, mas o custo já a identifica).
2. Gráficos em SVG inline no template (`spend-chart` com `<rect>` por dia, `priority-donut` com o truque de `stroke-dasharray`/`stroke-dashoffset` num `<circle>`) — sem adicionar biblioteca de charting.
3. Reaproveitar os tokens de cor já existentes (`--bcb-yellow`, `--bcb-urgent`, `--bcb-panel`) — nada de paleta nova só pro dashboard.
