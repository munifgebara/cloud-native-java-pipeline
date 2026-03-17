# Pagadoria

Sistema de administração de comissões imobiliárias com autenticação OAuth2, API REST e frontend SPA.

Autor: Munif Gebara Junior

---

## 1. Visão geral

O projeto Pagadoria é um sistema completo composto por:

- Backend Java com Spring Boot
- Frontend Angular (SPA)
- Autenticação com Keycloak (OAuth2 / OpenID Connect / JWT)
- Banco PostgreSQL
- Ambiente local com Docker Compose
- Build integrado frontend + backend
- Frontend servido pelo próprio Spring em /app

O objetivo é permitir o gerenciamento de:

- Pessoas
- Transações
- Participações
- Parcelas
- Antecipações
- Relatórios
- Controle de permissões por role

---

## 2. Arquitetura

Arquitetura geral do sistema:

Browser
↓
Angular SPA (/app)
↓
Spring Boot API (:8080)
↓
Keycloak (:9080) → autenticação
↓
PostgreSQL (:5432)


Fluxo de autenticação:


Browser → Angular
Angular → Keycloak login
Keycloak → JWT
Angular → API (Bearer token)
API → valida token
API → PostgreSQL


O frontend Angular é compilado e copiado para:


src/main/resources/static/app


O Spring Boot serve o frontend automaticamente.

---

## 3. Estrutura do projeto


pagadoria/

frontend/
src/main/java/
src/main/resources/
src/main/resources/static/app/

docker-compose.yml
pom.xml
README.md


Backend:


domain
repository
service
controller
dto
security
config


Frontend:


frontend/
src/
angular.json
package.json


---

## 4. Portas usadas

| Serviço | Porta | URL |
|---------|--------|--------|
| API Spring | 8080 | http://127.0.0.1:8080 |
| Frontend | 8080 | http://127.0.0.1:8080/app |
| Keycloak | 9080 | http://127.0.0.1:9080 |
| PostgreSQL | 5432 | 127.0.0.1:5432 |

---

## 5. Requisitos

Instalar:

- Java 21+
- Maven 3.9+
- Node 18+
- Angular CLI
- Docker
- Docker Compose

Instalar Angular CLI:

npm install -g @angular/cli


Verificar:


ng version
node -v
mvn -v
docker -v


---

## 6. Subir infraestrutura

Na raiz do projeto:


docker compose up -d


Verificar:


docker compose ps


Containers esperados:

- postgres
- keycloak

---

## 7. PostgreSQL

Host:


127.0.0.1


Porta:


5432


Banco principal:


pagadoria


Usuário:


pagadoria


Senha:


pagadoria


Banco do Keycloak:


keycloak


Usuário:


keycloak


Senha:


keycloak


---

## 8. Keycloak

Abrir:


http://127.0.0.1:9080


Login admin:


admin
admin


Realm:


pagadoria


Roles:


ADMIN_PAGADORIA
CONTRATANTE
PAGADOR
FAVORECIDO


Usuários:

| usuário | senha | role |
|---------|--------|--------|
| pagadoria_admin | admin123 | ADMIN_PAGADORIA |
| contratante1 | demo123 | CONTRATANTE |
| pagador1 | demo123 | PAGADOR |
| favorecido1 | demo123 | FAVORECIDO |

Client API:


pagadoria-api
bearer-only


Client CLI:


pagadoria-cli
public
direct access grants enabled


---

## 9. Gerar token


curl -X POST
http://127.0.0.1:9080/realms/pagadoria/protocol/openid-connect/token

-d "client_id=pagadoria-cli"
-d "username=pagadoria_admin"
-d "password=admin123"
-d "grant_type=password"


Resposta contém:


access_token


---

## 10. Usar token


curl http://127.0.0.1:8080/api/test

-H "Authorization: Bearer TOKEN"


## 11. Backend Spring Boot

Rodar pelo IntelliJ ou Maven:

mvn spring-boot:run

Build:

mvn clean package

Jar gerado em:

target/pagadoria.jar


application.yml

server:
port: 8080

spring:

datasource:
url: jdbc:postgresql://127.0.0.1:5432/pagadoria
username: pagadoria
password: pagadoria

jpa:
hibernate:
ddl-auto: validate

security:
oauth2:
resourceserver:
jwt:
issuer-uri: http://127.0.0.1:9080/realms/pagadoria


--------------------------------------------

## 12. SecurityConfig

Exemplo:

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
            .csrf(csrf -> csrf.disable())

            .authorizeHttpRequests(auth -> auth

                .requestMatchers(
                        "/",
                        "/app/**",
                        "/favicon.ico",
                        "/index.html",
                        "/static/**"
                ).permitAll()

                .requestMatchers("/api/**").authenticated()

                .anyRequest().permitAll()
            )

            .oauth2ResourceServer(oauth ->
                    oauth.jwt(Customizer.withDefaults())
            );

        return http.build();
    }
}


IMPORTANTE

Sem liberar /app/** o Angular dá 401.


--------------------------------------------

## 13. Forward para SPA Angular

Necessário para rotas do Angular funcionarem.

Exemplo:

@Controller
public class SpaForwardController {

    @RequestMapping({"/app", "/app/**"})
    public String forward(HttpServletRequest request) {

        String path = request.getRequestURI();

        if (path.contains(".")) {
            return null;
        }

        return "forward:/app/index.html";
    }
}


Sem isso:

/app/pessoas
/app/login
/app/parcelas

dão 404.


--------------------------------------------

## 14. Frontend Angular

Criado dentro do projeto:

ng new frontend --routing --style=css


Estrutura:

frontend/
src/
angular.json
package.json
tsconfig.json


Rodar dev:

cd frontend
npm install
ng serve


URL dev:

http://127.0.0.1:4200


--------------------------------------------

## 15. Build Angular dentro do Spring

O Angular deve gerar arquivos em:

src/main/resources/static/app


Configurar angular.json


build.options.outputPath:

"outputPath": "../src/main/resources/static/app"


Configurar base-href

"baseHref": "/app/"


Exemplo:

"options": {
"outputPath": "../src/main/resources/static/app",
"baseHref": "/app/"
}


Isso faz o Angular funcionar dentro do Spring.


--------------------------------------------

## 16. package.json build customizado

Para limpar antes de build:

"scripts": {

"build":
"rm -rf ../src/main/resources/static/app/* && ng build --base-href /app/"

}


Sem limpar, arquivos antigos podem causar erro.


--------------------------------------------

## 17. Arquivos estáticos

Após build:

src/main/resources/static/app/index.html
src/main/resources/static/app/main.js
src/main/resources/static/app/styles.css


Spring Boot serve automaticamente.


URL:

http://127.0.0.1:8080/app


--------------------------------------------

## 18. Problemas comuns Angular + Spring


### MIME error

Causa:

base-href errado


Corrigir:

--base-href /app/


### JS não carrega

Causa:

outputPath errado


### 401 /app

Causa:

Security bloqueando


Liberar:

/app/**


### Loop forward

Causa:

forward pegando index.html


Resolver:

if(path.contains(".")) return null;


### 404 ao atualizar página

Causa:

sem forward controller


--------------------------------------------

## 19. Build integrado com Maven (opcional)

Pode integrar frontend no build.


Exemplo plugin:

frontend-maven-plugin


Fluxo:

mvn clean package

→ build Angular
→ copia para static/app
→ gera jar


(usar se necessário)


--------------------------------------------

## 20. URLs finais

Frontend

http://127.0.0.1:8080/app

API

http://127.0.0.1:8080/api

Keycloak

http://127.0.0.1:9080

Postgres

127.0.0.1:5432

## 21. .gitignore recomendado

Separar backend e frontend.

Exemplo:

####################################
# Backend - Java / Maven
####################################

target/
*.log

.mvn/wrapper/maven-wrapper.jar

!**/src/main/**/target/
!**/src/test/**/target/


####################################
# IntelliJ
####################################

.idea/
*.iml
*.ipr
*.iws


####################################
# VSCode
####################################

.vscode/


####################################
# Angular / Node
####################################

frontend/node_modules/
frontend/node/
frontend/.angular/
frontend/dist/


####################################
# Angular build dentro do Spring
####################################

src/main/resources/static/app/


####################################
# OS
####################################

.DS_Store
Thumbs.db


--------------------------------------------

## 22. docker-compose

Arquivo docker-compose.yml deve subir:

- postgres
- keycloak

Exemplo simplificado:

services:

postgres:
image: postgres:15
ports:
- "5432:5432"

keycloak:
image: quay.io/keycloak/keycloak
ports:
- "9080:8080"


Subir:

docker compose up -d

Parar:

docker compose down


--------------------------------------------

## 23. Fluxo completo do sistema

Login:

Browser → Angular
Angular → Keycloak
Keycloak → JWT
Angular → API
API → valida token
API → PostgreSQL

Sem token:

401


--------------------------------------------

## 24. Fluxo de desenvolvimento

1. subir docker

docker compose up -d


2. rodar backend

mvn spring-boot:run


3. rodar frontend

cd frontend
ng serve


4. acessar

http://127.0.0.1:4200


--------------------------------------------

## 25. Fluxo de produção local

1. build frontend

cd frontend
npm run build


2. build backend

mvn clean package


3. rodar jar

java -jar target/pagadoria.jar


abrir

http://127.0.0.1:8080/app


--------------------------------------------

## 26. Estrutura recomendada backend

config/
security/
controller/
service/
repository/
domain/
dto/


--------------------------------------------

## 27. Convenções do projeto

Entidades no singular

Pessoa
Transacao
Parcela


DTO termina com DTO

PessoaDTO


Controller termina com Controller

PessoaController


Service termina com Service


--------------------------------------------

## 28. Convenções Angular

Component:

pessoa-list.component.ts

Service:

pessoa.service.ts

Model:

pessoa.model.ts


--------------------------------------------

## 29. Problemas comuns


401 no Angular

→ liberar /app/**


404 ao atualizar rota

→ forward controller


JS não carrega

→ base-href errado


MIME error

→ outputPath errado


CORS

→ configurar Spring


Token inválido

→ issuer-uri errado


--------------------------------------------

## 30. Checklist build

✔ docker rodando  
✔ postgres rodando  
✔ keycloak rodando  
✔ realm criado  
✔ roles criadas  
✔ angular build ok  
✔ static/app gerado  
✔ forward funcionando  
✔ security liberado  
✔ API protegida


--------------------------------------------

## 31. Checklist deploy

✔ mvn clean package  
✔ frontend build  
✔ jar gerado  
✔ banco acessível  
✔ keycloak acessível  
✔ issuer correto  
✔ portas corretas


--------------------------------------------

## 32. Boas práticas

Nunca versionar:

node_modules
dist
static/app
target


Sempre usar:

127.0.0.1
não localhost


Sempre limpar build Angular


--------------------------------------------

## 33. Autor

Munif Gebara Junior

Projeto Pagadoria

Sistema de administração de comissões imobiliárias





