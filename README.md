# FinTrack — Controle de Gastos com Cartão de Crédito

Sistema de gestão de faturas, divisão de despesas (Círculo de Confiança) e categorização automática. Backend em Spring Boot e frontend em React.

**Principais funcionalidades:** cartões (físico, virtual, adicional), faturas por mês, rateio de itens entre usuários e contatos, importação de PDF, regras de categorização por estabelecimento, login com email/senha ou Google (OAuth2).

## 🏗️ Estrutura do Projeto

```
FinTrack/
├── src/                    # Backend Spring Boot (Maven)
├── fintrack-frontend/      # Frontend React (TypeScript)
├── .cursor/docs/           # Contexto e requisitos (.cursor/docs/fintrack-context)
├── start-backend.sh        # Script para iniciar backend
├── start-frontend.sh       # Script para iniciar frontend
├── start-all.sh            # Script para iniciar ambos
├── build-all.sh            # Script para build completo
├── docker-compose.yml      # Deploy com Docker
├── .env                    # Variáveis de ambiente (MySQL, JWT, OAuth2)
└── pom.xml                 # Dependências Maven
```

## 🚀 Início Rápido

### Opção 1: Iniciar Tudo Junto (Recomendado)
```bash
./start-all.sh
```

### Opção 2: Iniciar Separadamente
```bash
# Terminal 1 - Backend
./start-backend.sh

# Terminal 2 - Frontend
./start-frontend.sh
```

### Opção 3: Build Completo
```bash
./build-all.sh
```

## 📋 Pré-requisitos

- **Java 17+**
- **Maven** (ou Maven Wrapper incluído)
- **Node.js 18+** e **npm** ou **yarn**
- **MySQL** (backend usa MySQL; testes usam H2)
- Arquivo **`.env`** na raiz com variáveis de banco, JWT e OAuth2 (ver Configuração)

## 🔧 Configuração

### Variáveis de ambiente (`.env`)
- `MYSQL_DATABASE`, `MYSQL_USER`, `MYSQL_PASSWORD` — conexão com MySQL
- `JWT_SECRET` — chave para tokens JWT
- `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` — login com Google (OAuth2)
- `SPRING_PROFILES_ACTIVE` — ex.: `dev`

### Backend (Spring Boot)
- **Porta**: 8080
- **API**: http://localhost:8080/api

### Frontend (React)
- **Porta**: 3000
- **URL**: http://localhost:3000

## 🐳 Deploy com Docker

```bash
# Iniciar com Docker Compose
docker-compose up -d

# Parar serviços
docker-compose down
```

## 📚 Documentação

- [Contexto e requisitos funcionais](.cursor/docs/fintrack-context) — visão do produto, regras de negócio e arquitetura
- API REST documentada nos controllers; base path `/api`

## 🛠️ Desenvolvimento

### Backend
```bash
# Executar testes
mvn test

# Checkstyle
mvn checkstyle:check

# Build
mvn clean package

# Executar
mvn spring-boot:run
```

### Frontend
```bash
cd fintrack-frontend

# Instalar dependências
npm install

# Executar em desenvolvimento
npm start

# Build para produção
npm run build
```

## 📝 Logs

Quando usando os scripts:
- **Backend**: `backend.log`
- **Frontend**: `frontend.log`

## 🔍 Troubleshooting

### Problemas comuns:

1. **Porta 8080 ocupada**: Pare outros serviços Java
2. **Porta 3000 ocupada**: Pare outros serviços Node.js
3. **MySQL não conecta**: Verifique o arquivo `.env` (MYSQL_*)
4. **Frontend não carrega**: Verifique se backend está rodando

## 📞 Suporte

Para dúvidas ou problemas, consulte a documentação ou abra uma issue no repositório.