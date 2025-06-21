# 🚀 Guia de Integração: Frontend React + Backend Spring Boot

Este guia explica como integrar o frontend React com o backend Spring Boot do projeto FinTrack.

## 📋 Pré-requisitos

- ✅ Backend Spring Boot rodando na porta 8080
- ✅ Frontend React rodando na porta 3000
- ✅ Banco de dados MySQL configurado
- ✅ CORS configurado no backend

## 🛠️ Configuração do Backend

### 1. Configuração CORS
O backend já possui a configuração CORS em `src/main/java/com/fintrack/config/CorsConfig.java` que permite requisições do frontend React.

### 2. Endpoints de Autenticação Disponíveis

#### Login
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}
```

**Resposta:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer"
}
```

#### Registro
```http
POST /api/users/register
Content-Type: application/json

{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "password123"
}
```

**Resposta:**
```json
{
  "message": "User registered successfully"
}
```

#### Usuário Atual
```http
GET /api/users/current-user
Authorization: Bearer <jwt_token>
```

**Resposta:**
```json
{
  "id": 1,
  "name": "John Doe",
  "email": "john@example.com",
  "roles": ["USER"],
  "createdAt": "2024-01-01T10:00:00",
  "updatedAt": "2024-01-01T10:00:00"
}
```

## 🎨 Configuração do Frontend

### 1. Estrutura do Projeto
```
fintrack-frontend/
├── src/
│   ├── components/          # Componentes React
│   │   ├── Login.tsx       # Tela de login
│   │   ├── Register.tsx    # Tela de registro
│   │   ├── Dashboard.tsx   # Dashboard principal
│   │   └── ProtectedRoute.tsx # Rota protegida
│   ├── contexts/           # Contextos React
│   │   └── AuthContext.tsx # Contexto de autenticação
│   ├── services/           # Serviços de API
│   │   └── api.ts         # Cliente HTTP
│   ├── types/              # Tipos TypeScript
│   │   └── auth.ts        # Tipos de autenticação
│   └── App.tsx            # Componente principal
```

### 2. Configuração da API
O frontend está configurado para se conectar ao backend na URL `http://localhost:8080/api` através do arquivo `src/services/api.ts`.

### 3. Gerenciamento de Estado
- **Context API**: Gerencia o estado de autenticação globalmente
- **localStorage**: Armazena o JWT token
- **Interceptors**: Adicionam automaticamente o token em todas as requisições

## 🔐 Fluxo de Autenticação

### 1. Login
1. Usuário preenche formulário de login
2. Frontend envia credenciais para `/api/auth/login`
3. Backend valida credenciais e retorna JWT token
4. Frontend armazena token no localStorage
5. Usuário é redirecionado para o dashboard

### 2. Requisições Autenticadas
1. Frontend adiciona automaticamente o header `Authorization: Bearer <token>`
2. Backend valida o token via `JwtFilter`
3. Se válido, requisição é processada
4. Se inválido, retorna 401 e frontend redireciona para login

### 3. Logout
1. Usuário clica em logout
2. Frontend remove token do localStorage
3. Usuário é redirecionado para tela de login

## 🚀 Como Testar

### 1. Iniciar o Backend
```bash
# Na pasta raiz do projeto
mvn spring-boot:run
```

### 2. Iniciar o Frontend
```bash
# Na pasta fintrack-frontend
npm start
```

### 3. Testar a Integração

1. **Acesse**: `http://localhost:3000`
2. **Registre um usuário**: Clique em "Sign up here"
3. **Faça login**: Use as credenciais registradas
4. **Verifique o dashboard**: Deve mostrar as informações do usuário

### 4. Verificar no Browser

**Network Tab:**
- Verifique as requisições para `/api/auth/login`
- Confirme que o token está sendo enviado em requisições subsequentes
- Verifique se não há erros CORS

**Application Tab:**
- Verifique se o token está armazenado no localStorage
- Confirme que o token é removido no logout

## 🔧 Troubleshooting

### Erro CORS
Se aparecer erro de CORS:
1. Verifique se o backend está rodando na porta 8080
2. Confirme se a configuração CORS está ativa
3. Verifique se o frontend está rodando na porta 3000

### Token Inválido
Se o token não estiver sendo aceito:
1. Verifique se o token está sendo enviado corretamente
2. Confirme se o `JwtFilter` está configurado
3. Verifique se o `CustomUserDetailsService` está funcionando

### Usuário não encontrado
Se o endpoint `/api/users/current-user` retornar 404:
1. Verifique se o usuário existe no banco
2. Confirme se o email está correto
3. Verifique se o token contém o email correto

## 📝 Próximos Passos

### Funcionalidades a Implementar
- [ ] Gerenciamento de cartões de crédito
- [ ] Visualização de faturas
- [ ] Gestão de bancos
- [ ] Relatórios financeiros
- [ ] Perfil do usuário

### Melhorias Técnicas
- [ ] Refresh token
- [ ] Interceptors de erro mais robustos
- [ ] Loading states
- [ ] Validação de formulários mais avançada
- [ ] Testes de integração

## 🎯 Exemplo de Uso

### Criar um novo cartão de crédito
```javascript
// No frontend
const createCreditCard = async (cardData) => {
  try {
    const response = await apiService.post('/credit-cards', cardData);
    return response.data;
  } catch (error) {
    console.error('Error creating credit card:', error);
    throw error;
  }
};
```

### Buscar faturas do usuário
```javascript
// No frontend
const getUserInvoices = async () => {
  try {
    const response = await apiService.get('/invoices');
    return response.data;
  } catch (error) {
    console.error('Error fetching invoices:', error);
    throw error;
  }
};
```

## 📞 Suporte

Para dúvidas ou problemas:
1. Verifique os logs do backend
2. Verifique o console do browser
3. Confirme se todas as dependências estão instaladas
4. Verifique se as portas estão disponíveis 