# FinTranck - Sistema de Controle Financeiro

Sistema completo de controle financeiro com backend Spring Boot e frontend React.

## 🏗️ Estrutura do Projeto

```
FinTranck/
├── src/                    # Backend Spring Boot
├── fintrack-frontend/      # Frontend React
├── start-backend.sh        # Script para iniciar backend
├── start-frontend.sh       # Script para iniciar frontend
├── start-all.sh           # Script para iniciar ambos
├── build-all.sh           # Script para build completo
├── docker-compose.yml     # Deploy com Docker
└── pom.xml               # Dependências Maven
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
- **Maven** (ou use o Maven Wrapper incluído)
- **Node.js 18+**
- **npm** ou **yarn**
- **MySQL** (para produção)

## 🔧 Configuração

### Backend (Spring Boot)
- **Porta**: 8080
- **API**: http://localhost:8080/api
- **Health Check**: http://localhost:8080/actuator/health

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

- [Guia de Integração](INTEGRATION_GUIDE.md)
- [API Documentation](http://localhost:8080/swagger-ui.html)

## 🛠️ Desenvolvimento

### Backend
```bash
# Executar testes
mvn test

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
3. **MySQL não conecta**: Verifique variáveis de ambiente
4. **Frontend não carrega**: Verifique se backend está rodando

## 📞 Suporte

Para dúvidas ou problemas, consulte a documentação ou abra uma issue no repositório.