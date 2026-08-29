# Spec — BCB (Big Chat Brasil)

> Consolida `fullstack.md`, `regras-negocio.md` e `requisitos-tecnicos.md` num único documento de decisão. Onde os três docs se contradizem ou deixam lacuna, a decisão e a justificativa estão explícitas aqui — isso também alimenta a seção "Premissas assumidas" do README final.

## 1. Validação da stack escolhida

| Requisito (`requisitos-tecnicos.md`) | Escolha do projeto | Atende? |
|---|---|---|
| Backend: Java / Node / Go | Java 21 + Spring Boot 3.3 | ✅ |
| Frontend: React ou Angular | Angular 22 + Angular Material (M3) | ✅ |
| Banco: PostgreSQL (preferência) | PostgreSQL + Spring Data JPA + Liquibase | ✅ [TASK-02](tasks.md#task-02) |
| Docker + docker-compose (obrigatório p/ todos os perfis) | — | ✅ código pronto — [TASK-19](tasks.md#task-19) (`docker-compose up` não testado neste ambiente por falta de Docker) |
| Design responsivo | Angular Material + breakpoints | ✅ [TASK-17](tasks.md#task-17) |

**Conclusão: a stack atende.** Todas as tasks referenciadas foram executadas — o que resta de risco é só o que está marcado com ⚠️ nelas (ver `docs/tasks.md`), não decisão de tecnologia em aberto.

## 2. Conflitos entre os documentos e como resolvi

### 2.1 Docker é obrigatório ou "Parte 3 opcional"?
`requisitos-tecnicos.md` → *"O Que Entregar (Mínimo Necessário) → Para todos os perfis: Docker-compose para executar o projeto"*.
`fullstack.md` → lista Docker dentro de *"Parte 3: Infraestrutura e Qualidade (opcional) — escolha apenas uma"*.

**Decisão:** trato Docker/docker-compose como **obrigatório** (não é uma das "escolhas" da Parte 3) porque `requisitos-tecnicos.md` é o documento transversal de entrega — vale pros três perfis (backend/frontend/fullstack) e é explícito com o comando `docker-compose up`. Isso libera a escolha real da Parte 3 para outro diferencial (ver 3.3).

### 2.2 Fila de prioridade e planos de pagamento são "core" ou "enhancement"?
`regras-negocio.md` descreve fila de prioridade e os dois planos de pagamento na seção "Principais Regras de Negócio", sem marcá-los como opcionais. `fullstack.md` os lista na "Parte 2 (escolha duas)", e `requisitos-tecnicos.md` pede como mínimo apenas **"implementação de pelo menos um tipo de plano"** e **"processamento síncrono (sem fila assíncrona)"**.

**Decisão:** `regras-negocio.md` é o documento de domínio compartilhado entre todos os perfis — ele descreve o sistema completo, não o mínimo de cada perfil. O mínimo de cada perfil é o que `requisitos-tecnicos.md`/`fullstack.md` recortam dele. Então:
- **Mínimo (Parte 1):** array ordenado simples (FIFO), processamento síncrono, **um** tipo de plano.
- **Enhancement escolhido (Parte 2 — backend):** fila com prioridade (normal/urgente). Ver 3.1.
- Os **dois** tipos de plano (pré e pós-pago) eu decidi implementar mesmo assim, porque o custo incremental é baixo (é um `if` na validação de billing) e a identidade visual do produto (`identidade-visual.html`) já assume os dois planos como parte central da proposta. A "Administração" completa de planos (crédito, ajuste de limite, conversão entre planos, histórico de transações) fica como **stretch** — ver 3.4.

> **Isso não entra em conflito com "processamento síncrono de mensagens (sem fila assíncrona)"?** Não, mas a tensão é real o suficiente pra merecer ser explicada em vez de assumida — ver 3.1.1.

### 2.3 Quem é "cliente"?
`regras-negocio.md` diz "conversas individuais com clientes finais" e ao mesmo tempo "clientes podem ser PF/PJ... cada cliente tem plano". Lendo os dois junto com o contrato TypeScript de `fullstack.md` (que tem `Client` com saldo/limite/plano, e `Conversation.recipientId/recipientName` como campos soltos, sem uma entidade própria), fica claro que são dois papéis diferentes:

- **Client** = a empresa (PF/PJ) que contrata o BCB, autentica via CPF/CNPJ, tem plano/saldo, e é quem manda mensagens.
- **Recipient** = o cliente final que recebe a mensagem. Não autentica, não tem plano. Modelado como campo simples dentro de `Conversation` (não como entidade própria) — decisão de escopo pra não inflar o domínio sem necessidade real dos requisitos.

## 3. Decisões de escopo (Parte 2 e Parte 3)

### 3.1 Backend — escolhido: **fila com prioridade (normal/urgente)**
Alternativas descartadas: validação financeira completa (já fazemos o suficiente no mínimo + os dois planos, ver 2.2), persistência em banco (tratada como parte da fundação, não como o "enhancement" — ver 3.2).

**Algoritmo:** fila ordenada por `(prioridade DESC, enfileiradoEm ASC)`. Mensagens urgentes furam a frente de qualquer mensagem normal ainda **não processada**; entre mensagens da mesma prioridade, ordem FIFO é preservada. Um worker (`@Scheduled`, intervalo curto) drena a fila continuamente e marca as mensagens como `sent`/`delivered`, simulando processamento assíncrono sem precisar de infraestrutura de mensageria (Kafka/RabbitMQ) — fora de escopo pro tamanho do desafio.

#### 3.1.1 Isso conflita com "processamento síncrono (sem fila assíncrona)"?

Só na leitura literal, fora de contexto. A frase completa é:

> *"Processamento síncrono de mensagens (sem fila assíncrona)"* — `requisitos-tecnicos.md`, seção **"Backend (mínimo)"**.

Duas coisas amarram essa frase ao **piso**, não ao projeto inteiro:

1. **Posição estrutural.** Ela vive dentro da lista `### Backend (mínimo):`, que é a definição do que basta pra passar — não uma regra que sobrevive independente de qualquer enhancement escolhido. `requisitos-tecnicos.md` não menciona fila de prioridade em lugar nenhum; quem introduz esse enhancement é `fullstack.md`.
2. **A arquitetura sugerida pelo próprio desafio já é assíncrona.** Em `fullstack.md`, a seção "Fluxo de Integração" — apresentada como o desenho geral do desafio fullstack, não como uma opção escondida atrás de uma escolha — mostra explicitamente:
   ```
   |-- POST /messages ------------------>|
   |<-- {id, status: 'queued', cost} -----|
   |                                      |   +-----------------+
   |                                      |-->| Fila de Mensagens|
   |                                      |   | - Normal        |
   |                                      |   | - Urgente       |
   |                                      |<--| Worker Process  |
   |                                      |   +-----------------+
   |<-- WebSocket/Polling: status='sent' -|
   ```
   Ou seja, o próprio documento do desafio já desenha `Worker Process` + atualização de status via `WebSocket/Polling` como o comportamento esperado da versão completa.

**Resolução:** "sem fila assíncrona" define o que você **não é obrigado** a construir pra bater o mínimo — não uma proibição do projeto inteiro. Ao escolher a fila com prioridade como enhancement da Parte 2, saio do mínimo por definição, e a arquitetura assíncrona deixa de ser opcional: **é o único jeito da fila de prioridade demonstrar algo de verdade.** Se cada mensagem processasse na mesma requisição que a criou, nunca haveria mais de uma mensagem na fila ao mesmo tempo — e sem acúmulo, não existe "urgente furando a frente de normal", porque não tem fila nenhuma pra furar.

**Garantia de que o mínimo continua respeitado:** o caminho síncrono da Parte 1 não deixa de existir — ele é o caso onde a fila está vazia no momento do envio (o caso comum, sem concorrência). O worker assíncrono só se torna observável quando há mais de uma mensagem pendente ao mesmo tempo, que é exatamente a situação que a Parte 2 pede pra demonstrar.

### 3.2 Persistência: por que não é "o enhancement"
`requisitos-tecnicos.md` já exige Docker-compose pra todos os perfis, e `PostgreSQL (preferência)` está listado nas tecnologias recomendadas, não como bônus. Como Docker já vai orquestrar o ambiente de qualquer forma, subir um container Postgres junto não é custo extra relevante — então entra como parte da fundação (TASK-02), liberando a escolha real da Parte 2 para a fila de prioridade, que é o diferencial técnico mais interessante de se explicar em entrevista.

### 3.3 Frontend — escolhido: **status visuais de mensagem** (enviada/entregue/lida)
Alternativas descartadas: indicador de digitação/presença (exige WebSocket real-time, custo alto pro valor entregue nesse prazo), filtros/busca no histórico (fica como stretch, [TASK-24](tasks.md#task-24)).

Justificativa: o próprio contrato de `MessageResponse` já modela `status: 'queued'|'processing'|'sent'|'delivered'|'read'|'failed'` — implementar o enhancement é reaproveitar dado que o backend já precisa expor pela fila de prioridade (3.1), sem endpoint novo.

### 3.4 Parte 3 (infraestrutura/qualidade) — escolhido: **Swagger/OpenAPI**
Alternativas descartadas: testes automatizados (viram stretch, [TASK-22](tasks.md#task-22) — valem a pena mas competem por tempo com o core), Docker (já é obrigatório, não conta como a "escolha" da Parte 3 — ver 2.1).

Justificativa: `springdoc-openapi` é ~15 minutos de setup e dá um artefato vivo e clicável pra abrir na entrevista de live-coding (`requisitos-tecnicos.md` menciona explicitamente que você vai "explicar sua solução" nessa entrevista) — melhor retorno por hora investida nesse prazo.

## 4. Domínio

```
Client (empresa PF/PJ)
 ├── id, name, documentId, documentType [CPF|CNPJ]
 ├── planType [PREPAID|POSTPAID], active
 ├── balance (se PREPAID) | monthlyLimit + monthlyUsage (se POSTPAID)
 └── 1—N Conversation

Conversation
 ├── id, clientId
 ├── recipientId, recipientName   (cliente final — sem entidade própria)
 └── 1—N Message

Message
 ├── id, conversationId
 ├── content, priority [NORMAL|URGENT]
 ├── status [QUEUED|PROCESSING|SENT|DELIVERED|READ|FAILED]
 ├── cost (0.25 | 0.50), sentByType [CLIENT|USER]
 └── queuedAt, processedAt

Transaction (histórico financeiro — stretch, TASK-23)
 ├── id, clientId, messageId (sem relacionamento JPA com Client/Message — só coluna, ver §6)
 └── type [DEBIT|CREDIT], amount, timestamp

Session (token opaco de autenticação)
 └── token, clientId, createdAt
```

### 4.1 Detalhamento de campos e tipos (TASK-01)

Decisões de tipo de coluna, hoje aplicadas tanto nas entidades JPA quanto nos changesets do Liquibase (`db/changelog/changes/*.sql` — ver §6):

- **Strings → `TEXT` no Postgres, não `VARCHAR(n)`.** No Postgres os dois têm performance e armazenamento idênticos internamente — `VARCHAR(n)` só adiciona uma checagem de tamanho na escrita. Usar `TEXT` sem limite evita ter que adivinhar um tamanho máximo arbitrário pra `name`, `document`, `recipientName`, `content` etc. **Enums armazenados como texto também são `TEXT`**, não `VARCHAR(255)` (o default do Hibernate pra `@Enumerated(STRING)` sem override) — mesma lógica, e mantém o schema e a intenção documentada aqui alinhados de verdade (`@Column(columnDefinition = "TEXT")` em cada enum).
- **Dinheiro → `NUMERIC(10,2)` / `BigDecimal`, nunca `double`.** Evita erro de arredondamento em `balance`, `monthlyLimit`, `monthlyUsage`, `cost`, `amount`. Enforçado explicitamente com `@Column(precision = 10, scale = 2)` — sem isso o Hibernate cai no default (`numeric(19,2)`), que diverge do que está documentado aqui.
- **IDs → `UUID`, gerado pelo Hibernate** (`@GeneratedValue(strategy = GenerationType.UUID)`, nativo do Hibernate 6) — exceto `Session.token`.
- **`Session.token` é a própria chave primária** (`TEXT`, sem coluna `id` redundante) e é gerado pela **aplicação** (`AuthService`, não pelo banco), porque o valor precisa ser devolvido no corpo da resposta de `POST /auth` na mesma chamada que persiste a sessão.

**Client**
| campo | tipo | notas |
|---|---|---|
| id | UUID (PK) | gerado pelo Hibernate |
| name | TEXT | |
| documentId (`document` + `documentType`) | TEXT, unique + TEXT | `@Embedded` — dois campos numa `DocumentId` só, não uma tabela separada |
| planType | enum (PREPAID/POSTPAID) | STRING, coluna TEXT |
| active | boolean | default true |
| balance | NUMERIC(10,2), nullable | só PREPAID |
| monthlyLimit | NUMERIC(10,2), nullable | só POSTPAID |
| monthlyUsage | NUMERIC(10,2), nullable | só POSTPAID |

**Conversation**
| campo | tipo | notas |
|---|---|---|
| id | UUID (PK) | |
| client (`client_id`) | UUID (FK → Client) | `@ManyToOne(FetchType.LAZY)` — único mapeamento da FK, sem coluna `UUID` cru em paralelo (ver §6) |
| recipientId | TEXT | não é FK (Recipient não é entidade, ver §2.3) |
| recipientName | TEXT | |
| lastMessageAt | timestamp, nullable | desnormalizado, antecipando a TASK-06 (ordenar lista de conversas sem subquery) |

**Message**
| campo | tipo | notas |
|---|---|---|
| id | UUID (PK) | |
| conversation (`conversation_id`) | UUID (FK → Conversation) | `@ManyToOne(FetchType.LAZY)`, mesma lógica de `Conversation.client` |
| content | TEXT | |
| priority | enum (NORMAL/URGENT) | STRING, coluna TEXT |
| status | enum (QUEUED/PROCESSING/SENT/DELIVERED/READ/FAILED) | STRING, coluna TEXT |
| cost | NUMERIC(10,2) | derivado da priority na escrita (0.25/0.50) |
| sentByType | enum (CLIENT/USER) | STRING, coluna TEXT |
| queuedAt | timestamp | |
| processedAt | timestamp, nullable | preenchido quando sai de QUEUED (ou marcado FAILED, ver §6) |

**Session**
| campo | tipo | notas |
|---|---|---|
| token | TEXT (PK) | gerado pela aplicação, não pelo banco |
| client (`client_id`) | UUID (FK → Client) | `@ManyToOne(FetchType.LAZY)` |
| createdAt | timestamp | |

**Transaction** (stretch, TASK-23)
| campo | tipo | notas |
|---|---|---|
| id | UUID (PK) | |
| clientId | UUID, FK → Client no banco | **sem** `@ManyToOne` na entidade (coluna `UUID` cru) — decisão deliberada, não esquecimento: ver §6 |
| messageId | UUID, nullable, FK → Message no banco | idem; hoje sempre `null` na prática (nenhum fluxo ainda liga transação a mensagem), mas a coluna/FK já existem pro dia que precisar |
| type | enum (DEBIT/CREDIT) | STRING, coluna TEXT |
| amount | NUMERIC(10,2) | |
| timestamp | timestamp | |

## 5. Contrato de API

Reaproveita literalmente as interfaces TypeScript já definidas em `fullstack.md` (seção "Interface de Integração") — não há motivo pra reinventar um contrato que o próprio desafio já especifica. Ver ali: `AuthRequest/AuthResponse`, `ConversationResponse`, `SendMessageRequest/SendMessageResponse`, `MessageResponse`.

Endpoints mínimos:
```
POST   /api/auth                              → AuthResponse
GET    /api/conversations                     → ConversationResponse[]
GET    /api/conversations/{id}/messages       → MessageResponse[]
POST   /api/messages                          → SendMessageResponse
POST   /api/clients                           → cadastro de cliente (CRUD mínimo exigido)
GET    /api/clients/{id}                      → consulta de saldo/limite
```
Todos exceto `POST /api/auth` e `POST /api/clients` exigem `Authorization: Bearer {token}`.

Além do mínimo, a Administração completa de planos (TASK-23, §2.2) adicionou:
```
POST   /api/clients/{id}/credit               → adiciona crédito (só PREPAID)
POST   /api/clients/{id}/limit                → ajusta limite mensal (só POSTPAID)
POST   /api/clients/{id}/plan                 → converte entre PREPAID/POSTPAID
GET    /api/clients/{id}/transactions         → histórico de transações do cliente
GET    /api/ping                              → healthcheck, sem autenticação
```
Esses seis endpoints extras não têm contrato TypeScript prévio em `fullstack.md` (são além do desafio original) — o contrato vivo e definitivo pra todos os 11 endpoints é o Swagger (`/swagger-ui.html`, TASK-20), não este documento. Manter os DTOs duplicados aqui só criaria mais um lugar pra ficar desatualizado a cada mudança de campo.

## 6. Decisões técnicas e trade-offs (pra citar na entrevista)

- **Autenticação:** token opaco simples (UUID) validado por um `OncePerRequestFilter` próprio, não Spring Security completo. Menos boilerplate e mais fácil de explicar em 2 minutos de live-coding; trade-off explícito é que não é produção-grade (sem expiração, refresh, rate limit) — assumido conscientemente e documentado no README.
- **Fila de prioridade:** estrutura em memória (`PriorityBlockingQueue` ou equivalente thread-safe) + persistência da mensagem no Postgres em paralelo, pra sobreviver a restart sem precisar reimplementar a fila com query SQL ordenada a cada tick.
- **Estado no frontend:** Angular signals (já é o padrão do Angular 22 usado no scaffold) em vez de NgRx — a superfície de estado do app (sessão, lista de conversas, mensagens da conversa aberta) é pequena o suficiente pra não justificar uma lib de state management.
- **SSR:** o projeto já nasceu com Angular SSR habilitado (schematics padrão do CLI). Mantido, mas não é foco de otimização neste desafio — chat autenticado não se beneficia de SSR/SEO; citar isso é honesto se perguntarem "por que SSR num chat logado".
- **Schema do banco: Liquibase, `ddl-auto: validate` (supera a decisão original abaixo).** A decisão inicial (TASK-02) era `ddl-auto: update` sem Liquibase, pra ter uma peça em movimento a menos no prazo do desafio. Reavaliado: schema versionado é justamente o tipo de decisão "produção-like" que vale citar numa entrevista, e o custo de manter é baixo — 5 changesets, um por tabela. Mudança de verdade em relação à tentativa anterior (que só existia como dependência solta no pom, nunca usada): os changesets são **SQL puro** (formato `--liquibase formatted sql`, um `CREATE TABLE` por arquivo em `db/changelog/changes/`), não XML nem YAML — mais fácil de ler/revisar que a sintaxe declarativa do Liquibase, e continua 100% versionável/idempotente. O `db.changelog-master.yaml` usa `includeAll` apontando pra `db/changelog/changes/` — varre a pasta sozinho, sem listar arquivo por arquivo; a ordem de execução vem do prefixo numérico do nome (`001-`, `002-`...), que já reflete a ordem das FKs (client → session → conversation → message → transaction). Um changeset novo é só criar o `.sql` com o próximo número, sem tocar no master. Hibernate muda de `update` pra `validate`: já não cria/altera nada sozinho, só confere na subida que as entidades batem com o schema que o Liquibase criou — se alguém adicionar um campo na entidade e esquecer o changeset correspondente, a aplicação não sobe, em vez de o Postgres divergir silenciosamente do código.
  Isso obrigou a destravar uma inconsistência que já existia (e passava despercebida com `update`, que nunca é estrito sobre tipo): nenhuma entidade tinha `@Column(precision = 10, scale = 2)` nos campos de dinheiro nem `columnDefinition = "TEXT"` nas colunas de enum, então o Hibernate gerava `numeric(19,2)`/`varchar(255)` por conta própria — divergindo do que o §4.1 já dizia ser a intenção (`numeric(10,2)`, `TEXT` sem limite). Como `validate` é estrito quanto a isso, as entidades (`Client`, `Message`, `Transaction`) ganharam essas anotações explícitas, e os changesets usam exatamente `NUMERIC(10,2)`/`TEXT` — schema e código finalmente dizendo a mesma coisa.
  `transaction.client_id`/`message_id` continuam sem relacionamento JPA (mesma fronteira de domínio da decisão de mapeamento acima), mas ganharam FK de verdade no banco — integridade referencial é responsabilidade do schema, não do jeito que a entidade Java decide (ou não) mapear a coluna como objeto.
- **Enums persistidos como `STRING`, não `ORDINAL`** (`@Enumerated(EnumType.STRING)`) em todas as entidades. `ORDINAL` grava só o índice do enum (`0`, `1`...) — se alguém reordenar as constantes no Java no futuro, dados já gravados são silenciosamente reinterpretados como outro valor. `STRING` é levemente mais pesado por linha, mas remove essa classe inteira de bug.
- **Relacionamentos JPA — `@ManyToOne` único, sem coluna duplicada (superou as duas decisões anteriores).** `Conversation.client` e `Message.conversation` são o único mapeamento da FK — nada de campo `UUID` cru em paralelo nem `insertable/updatable = false`. A tentativa anterior (campo `UUID` cru + relacionamento só-leitura) resolvia o acoplamento entre services mas duplicava a mesma coluna em dois lugares da entidade, o que é o problema original que motivou a mudança. A regra que resolve os dois ao mesmo tempo é de camada, não de mapeamento: **cada `Service` só injeta o `Repository` do próprio domínio**; quando precisa de uma entidade de outro domínio pra montar uma associação (ex: `ConversationService` criando uma `Conversation` a partir de um `clientId`), pede pro `Service` dono daquele repositório (`ClientService.getClientReference(id)`), nunca injeta o `Repository` alheio direto. Isso evita tanto o `EntityManager.getReference(...)` manual quanto a duplicação de coluna. Efeito colateral que também importa: com o relacionamento de volta como fonte única, a checagem de dono de conversa (`conversation.getClient().getId().equals(clientId)`) não paga SELECT extra — acessar o id de um proxy lazy não o inicializa.
  N+1 nas listagens: tratado como planejado — `GET /conversations` usa 2 queries fixas (1 pra listar, 1 agrupada com `GROUP BY conversation_id` pra contar não lidas de todas de uma vez), não N+1 por conversa.
  Essa regra de "cada Service só fala com o próprio Repository" tem uma consequência que exigiu mover uma responsabilidade: `MessageService` já depende de `ConversationService` (pra resolver/criar a conversa no envio de mensagem), e a listagem de conversas precisa da contagem de não lidas, que é dado de `Message`. Se `ConversationService` também dependesse de `MessageService` pra montar essa contagem, os dois beans formariam um ciclo que o Spring recusa a montar (`ConversationService → MessageService → ConversationService`). Resolvido tirando a montagem do `ConversationResponse` (que junta `Conversation` + contagem de `Message`) de dentro dos services e colocando no `ConversationController`, que já falava com os dois — nenhum service passou a depender do outro na direção contrária, e o ciclo nunca chega a existir.
- **Testes de service com `MockitoExtension`:** todo `Service` com dependência (repository ou outro service) ganhou teste unitário mockando os colaboradores — `AuthService`, `SessionService`, `ClientService`, `ConversationService`, `MessageService`, `MessageQueueWorker`, `TransactionService`, além de `BillingService` (sem mock — não tem dependência) e `MessageQueueService` (já existia). `@InjectMocks` em vez de construir o service manualmente no teste: evita o bug sutil de inicializar um campo `@Mock` num inicializador de campo do teste, que roda *antes* do `MockitoExtension` processar os mocks (a extensão só popula os `@Mock` durante o ciclo de vida do JUnit 5, não na construção da classe de teste). Escrevendo esses testes, um encontrou um bug de verdade: `ClientResponse.toClient()` não propagava `active` — inofensivo hoje (o único uso real é `SessionService.createSession`, sem cascade), mas corrigido na origem.
- **Dashboard da tela de conta:** gráficos de gasto por dia e distribuição normal/urgente, montados só com dado de `GET /clients/{id}/transactions` — sem endpoint novo. Como toda transação `DEBIT` custa exatamente `0.25` (NORMAL) ou `0.50` (URGENT), o valor cobrado já identifica a prioridade da mensagem, sem precisar cruzar com o histórico de mensagens. Gráficos são SVG inline escrito à mão (barras e um donut com o truque de `stroke-dasharray`/`stroke-dashoffset`) — sem adicionar biblioteca de charting pra duas visualizações simples.
- **Concorrência no débito de saldo/limite:** `chargeForMessage`/`addCredit`/`adjustLimit`/`convertPlan` usam `ClientRepository.findByIdForUpdate` (`SELECT ... FOR UPDATE` via `@Lock(PESSIMISTIC_WRITE)`) em vez de `findById`. Sem isso, duas requisições concorrentes pro mesmo cliente poderiam ler o mesmo saldo antes de qualquer uma commitar (lost update) e ambas passarem na validação com saldo que só existia uma vez. `getClientReference` (usado só pra montar a FK de `Conversation`/`Session`, não pra alterar saldo) continua com `findById` normal — lock pessimista ali seria custo sem benefício.
- **Retry na fila de mensagens:** o `MessageQueueWorker` reenfileira a mensagem (mantendo o `queuedAt` original, sem pular a fila) se `save()` falhar, até 3 tentativas; na terceira falha marca `status = FAILED`. Sem backoff — a mensagem só volta a ser tentada no próximo tick do `@Scheduled` (3s), o que já é suficiente pra uma falha transitória de conexão com o banco. Antes dessa decisão não havia retry nenhum: uma falha no meio do processamento fazia a mensagem sumir da fila em memória (já removida pelo `poll()`) sem nunca mais ser reprocessada até a aplicação reiniciar — e `MessageStatus.FAILED` existia no enum sem nunca ser usado.

## 7. Fora de escopo (documentado, não esquecido)

- WebSocket real-time (indicadores de digitação, push de mensagem sem polling)
- Fila assíncrona real (Kafka/RabbitMQ/SQS) — o worker `@Scheduled` simula o comportamento sem a infra
- Multi-tenancy, RBAC, múltiplos usuários por empresa
- Internacionalização
