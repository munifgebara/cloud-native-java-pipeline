# Stella

[English](README.md) | Português (pt-BR)

Stella é um projeto cloud-native de gestão de inventário pessoal criado para demonstrar uma plataforma Java full stack com autenticação moderna, infraestrutura local conteinerizada, deploy em Kubernetes e automação de CI/CD.

Ele foi pensado com dois objetivos complementares:

- portfólio: apresentar um projeto de engenharia de software ponta a ponta, cobrindo backend, frontend, infraestrutura, segurança e entrega
- aprendizado: servir como exemplo didático para estudantes entenderem como SPA, API protegida, banco de dados, containers e pipelines de deploy se conectam

## Visão Geral

A aplicação combina:

- API Spring Boot 4 com Java 25
- SPA Angular 21 com PrimeNG
- Keycloak para autenticação OAuth2 / OpenID Connect
- PostgreSQL com migrações Flyway
- Docker Compose para infraestrutura local
- manifests Kubernetes para deploy
- workflows do GitHub Actions para CI, publicação de imagem e deploy
- métricas via Actuator prontas para Prometheus

Hoje, o principal fluxo de negócio já implementado é o gerenciamento de pessoas (`pessoas`), junto com login, proteção de rotas e a base do dashboard. A estrutura do repositório e da infraestrutura já prepara o projeto para crescer para módulos mais amplos de inventário e operação.

## Por Que Este Projeto Importa

O Stella foi intencionalmente além de um CRUD simples. Ele mostra como o código da aplicação e as preocupações de plataforma evoluem juntos:

- a autenticação fica externalizada no Keycloak, em vez de estar hardcoded na aplicação
- frontend e backend fazem parte do mesmo fluxo de entrega
- a aplicação é empacotada para deploy em container
- manifests Kubernetes e GitHub Actions aproximam o projeto de um fluxo realista de produção
- suporte a Actuator e Prometheus abre caminho para monitoramento e maturidade operacional

Isso torna o repositório útil tanto como peça de portfólio quanto como referência didática em desenvolvimento Java cloud-native.

## Arquitetura

```text
Navegador
  -> SPA Angular (/app)
  -> API Spring Boot (:8080)
  -> PostgreSQL (:5432)

Fluxo de autenticação
  -> Usuário acessa a SPA
  -> SPA redireciona para o Keycloak (:9080)
  -> Keycloak autentica e emite tokens
  -> SPA chama a API com bearer token
  -> API valida o JWT e processa a requisição
```

## Stack Tecnológica

| Camada | Tecnologia |
| --- | --- |
| Backend | Spring Boot 4, Spring Security, Spring Data JPA, Flyway, Actuator |
| Frontend | Angular 21, PrimeNG, TypeScript |
| Identidade | Keycloak, OAuth2, OpenID Connect, JWT |
| Banco de dados | PostgreSQL |
| Observabilidade | Micrometer, endpoint Prometheus |
| Infraestrutura | Docker Compose, Kubernetes |
| CI/CD | GitHub Actions, GHCR |

## Escopo Funcional Atual

Já implementado ou visível no código:

- fluxo de login integrado ao Keycloak
- rotas protegidas no Angular
- telas de listagem e edição de pessoas
- API REST Spring Boot protegida como resource server
- migrações de banco com Flyway
- ambiente local baseado em Docker
- artefatos de deploy em Kubernetes
- base de workflow para CI/CD
- exposição de métricas via Actuator e Prometheus

Evoluções planejadas visíveis no backlog:

- módulos mais amplos de inventário
- internacionalização
- melhoria de logging e observabilidade
- preenchimento automático de endereço por CEP
- refinamento da documentação e onboarding

## Estrutura do Repositório

```text
.
|-- frontend/                  # SPA Angular
|-- k8s/                       # Manifests Kubernetes
|-- keycloak/                  # Arquivos de importação de realm
|-- postgres/                  # Scripts de inicialização do banco
|-- src/main/java/             # Código da aplicação Spring Boot
|-- src/main/resources/        # Configurações, migrações e assets
|-- .github/workflows/         # Pipelines de CI/CD
|-- docker-compose.yml         # Infraestrutura local
`-- pom.xml                    # Build Maven, integração do frontend e testes
```

## Execução Local

### Pré-requisitos

- Java 25
- Maven Wrapper ou Maven 3.9+
- Node.js 22+ e npm
- Docker e Docker Compose

### 1. Subir a infraestrutura

```bash
docker compose up -d
```

Isso sobe:

- PostgreSQL em `127.0.0.1:5432`
- Keycloak em `http://127.0.0.1:9080`

### 2. Rodar o backend

```bash
./mvnw spring-boot:run
```

No Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

A API ficará disponível em `http://127.0.0.1:8080`.

### 3. Rodar o frontend em modo de desenvolvimento

```bash
cd frontend
npm install
npm start
```

O servidor de desenvolvimento do Angular fica em `http://127.0.0.1:4200`.

### 4. Gerar o build integrado

```bash
./mvnw clean verify
```

O build Maven instala as dependências do frontend, gera o build do Angular e empacota o backend.

## Autenticação e Acesso de Demonstração

A autenticação local é feita pelo Keycloak no realm `stella`.

Credenciais padrão do admin local:

- usuário: `admin`
- senha: `admin`

O projeto também faz referência a papéis de aplicação como:

- `ADMIN_STELLA`
- `CONTRATANTE`
- `PAGADOR`
- `FAVORECIDO`

A validação de JWT é configurada no Spring Security como OAuth2 resource server.

## API e Observabilidade

Endpoints úteis no ambiente local:

- aplicação: `http://127.0.0.1:8080/app`
- base da API: `http://127.0.0.1:8080/api`
- OpenAPI / Scalar UI: `http://127.0.0.1:8080/scalar`
- health: `http://127.0.0.1:8080/actuator/health`
- metrics: `http://127.0.0.1:8080/actuator/metrics`
- prometheus: `http://127.0.0.1:8080/actuator/prometheus`

## Fluxo de Deploy e Entrega

O repositório já inclui os blocos principais de um fluxo cloud-native de entrega:

- `ci.yml` valida a aplicação em pushes e pull requests
- `publish-stella-api.yml` gera e publica a imagem do container
- `cd.yml` atualiza o deploy no Kubernetes após uma publicação bem-sucedida
- `k8s/` guarda os manifests usados no cluster

Esse conjunto ajuda a demonstrar a passagem do desenvolvimento local para a entrega automatizada.

## Notas Didáticas

Estudantes e avaliadores podem usar este repositório para explorar:

- como uma API Spring Boot funciona como resource server protegido por JWT
- como Angular e Spring Boot podem ser entregues juntos
- como o Flyway mantém a evolução do banco explícita
- como o Docker Compose simplifica o onboarding local
- como GitHub Actions pode separar responsabilidades entre CI, publicação e deploy
- como preocupações de observabilidade começam com métricas e disciplina operacional

## Roadmap

- expandir o domínio além do fluxo atual de pessoas
- evoluir o suporte multilíngue na interface
- refinar logging no servidor e integração com Grafana
- fortalecer a documentação para contribuidores e estudantes
- continuar endurecendo a pipeline para cenários mais próximos de produção

## Autor

Munif Gebara Junior

Se este repositório estiver sendo avaliado como portfólio, os sinais mais fortes estão na combinação de código de aplicação, infraestrutura, autenticação, observabilidade e fluxo de entrega em um único sistema orientado a aprendizado.
