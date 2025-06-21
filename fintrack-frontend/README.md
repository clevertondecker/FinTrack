# FinTrack Frontend

Frontend React para o sistema FinTrack - aplicação de controle financeiro.

## 🚀 Tecnologias

- **React 18** com TypeScript
- **React Router** para navegação
- **Axios** para requisições HTTP
- **Context API** para gerenciamento de estado
- **CSS3** com design responsivo

## 📋 Pré-requisitos

- Node.js 16+ 
- npm ou yarn
- Backend Spring Boot rodando na porta 8080

## 🛠️ Instalação

1. Clone o repositório
2. Navegue até a pasta do frontend:
```bash
cd fintrack-frontend
```

3. Instale as dependências:
```bash
npm install
```

4. Inicie o servidor de desenvolvimento:
```bash
npm start
```

O aplicativo estará disponível em `http://localhost:3000`

## 🔧 Configuração

### Backend URL
O frontend está configurado para se conectar ao backend na URL `http://localhost:8080/api`. 
Se necessário, altere a URL base no arquivo `src/services/api.ts`.

### Variáveis de Ambiente
Crie um arquivo `.env` na raiz do projeto para configurar variáveis de ambiente:

```env
REACT_APP_API_URL=http://localhost:8080/api
```

## 📁 Estrutura do Projeto

```
src/
├── components/          # Componentes React
│   ├── Login.tsx       # Tela de login
│   ├── Register.tsx    # Tela de registro
│   ├── Dashboard.tsx   # Dashboard principal
│   └── ProtectedRoute.tsx # Rota protegida
├── contexts/           # Contextos React
│   └── AuthContext.tsx # Contexto de autenticação
├── services/           # Serviços de API
│   └── api.ts         # Cliente HTTP
├── types/              # Tipos TypeScript
│   └── auth.ts        # Tipos de autenticação
└── App.tsx            # Componente principal
```

## 🔐 Autenticação

O sistema utiliza JWT (JSON Web Tokens) para autenticação:

1. **Login**: Usuário faz login com email e senha
2. **Token**: Backend retorna um JWT token
3. **Storage**: Token é armazenado no localStorage
4. **Requests**: Token é enviado automaticamente em todas as requisições
5. **Logout**: Token é removido do localStorage

## 🎨 Funcionalidades

### ✅ Implementadas
- [x] Tela de login com validação
- [x] Tela de registro com validação
- [x] Dashboard protegido
- [x] Gerenciamento de estado de autenticação
- [x] Interceptadores de requisição HTTP
- [x] Design responsivo
- [x] Navegação entre páginas

### 🚧 Próximas Implementações
- [ ] Gerenciamento de cartões de crédito
- [ ] Visualização de faturas
- [ ] Gestão de bancos
- [ ] Relatórios financeiros
- [ ] Perfil do usuário

## 🧪 Testes

Para executar os testes:
```bash
npm test
```

## 📦 Build

Para gerar o build de produção:
```bash
npm run build
```

## 🔧 Scripts Disponíveis

- `npm start` - Inicia o servidor de desenvolvimento
- `npm test` - Executa os testes
- `npm run build` - Gera o build de produção
- `npm run eject` - Ejecta a configuração do Create React App

## 🌐 CORS

Certifique-se de que o backend Spring Boot está configurado para aceitar requisições do frontend:

```java
@Configuration
public class CorsConfig {
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
```

