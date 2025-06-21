# Guia de Cartões de Crédito - FinTranck

Este guia explica como usar a funcionalidade de cartões de crédito no FinTranck.

## 🎯 Funcionalidades

### ✅ **Funcionalidades Implementadas:**

1. **Visualizar Cartões**: Lista todos os cartões do usuário logado
2. **Criar Cartão**: Adicionar novo cartão de crédito
3. **Editar Cartão**: Modificar informações do cartão
4. **Desativar Cartão**: Desativar cartão (soft delete)
5. **Filtro por Usuário**: Cada usuário vê apenas seus próprios cartões
6. **Validações**: Validação de dados no frontend e backend

## 🚀 Como Usar

### 1. **Acessar a Funcionalidade**

1. Faça login no sistema
2. No Dashboard, clique em **"View Cards"** no card "Credit Cards"
3. Você será direcionado para a tela de gerenciamento de cartões

### 2. **Visualizar Cartões**

- Os cartões são exibidos em cards organizados
- Cada card mostra:
  - Nome do cartão
  - Últimos 4 dígitos (mascarados)
  - Banco emissor
  - Limite de crédito
  - Data de criação
  - Status (Ativo/Inativo)

### 3. **Criar Novo Cartão**

1. Clique no botão **"Add New Card"**
2. Preencha os campos:
   - **Card Name**: Nome do cartão (ex: "Nubank", "Itaú")
   - **Last 4 Digits**: Últimos 4 dígitos do cartão
   - **Credit Limit**: Limite de crédito
   - **Bank**: Selecione o banco da lista
3. Clique em **"Create Card"**

### 4. **Editar Cartão**

1. Clique no botão **"Edit"** no card do cartão
2. Modifique os campos desejados
3. Clique em **"Update Card"**

### 5. **Desativar Cartão**

1. Clique no botão **"Deactivate"** no card do cartão
2. Confirme a ação na caixa de diálogo

## 🔧 Configuração Técnica

### **Backend (Spring Boot)**

- **Endpoint**: `/api/credit-cards`
- **Autenticação**: JWT Token obrigatório
- **Filtro**: Cartões são filtrados automaticamente por usuário
- **Validações**: Bean Validation + Domain Validation

### **Frontend (React)**

- **Componente**: `CreditCards.tsx`
- **Estados**: Loading, Error, Success
- **Formulários**: Validação em tempo real
- **Responsivo**: Funciona em desktop e mobile

### **Banco de Dados**

- **Tabela**: `credit_cards`
- **Relacionamento**: `owner_id` → `users.id`
- **Soft Delete**: Campo `active` para desativação

## 📋 Estrutura de Dados

### **CreditCard Entity**
```java
@Entity
@Table(name = "credit_cards")
public class CreditCard {
    private Long id;
    private String name;
    private String lastFourDigits;
    private BigDecimal creditLimit;
    private User owner;           // Relacionamento com usuário
    private Bank bank;            // Relacionamento com banco
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

### **CreateCreditCardRequest**
```typescript
interface CreateCreditCardRequest {
  name: string;           // Nome do cartão
  lastFourDigits: string; // Últimos 4 dígitos
  limit: number;          // Limite de crédito
  bankId: number;         // ID do banco
}
```

## 🛡️ Segurança

### **Validações Implementadas:**

1. **Frontend**:
   - Campos obrigatórios
   - Formato dos últimos 4 dígitos (apenas números)
   - Limite positivo
   - Banco selecionado

2. **Backend**:
   - Bean Validation
   - Domain Validation
   - Verificação de propriedade (usuário só acessa seus cartões)
   - Validação de existência do banco

### **Autenticação**:
- JWT Token obrigatório em todas as requisições
- Token é enviado automaticamente pelo interceptor do Axios
- Redirecionamento para login em caso de token inválido

## 🎨 Interface do Usuário

### **Design Features:**
- **Cards Responsivos**: Layout em grid que se adapta ao tamanho da tela
- **Estados Visuais**: Loading, erro, sucesso, vazio
- **Animações**: Hover effects e transições suaves
- **Modal**: Formulário em overlay para criar/editar
- **Confirmação**: Dialog para ações destrutivas

### **Cores e Estilos:**
- **Primária**: Gradiente azul/roxo (#667eea → #764ba2)
- **Sucesso**: Verde (#d4edda)
- **Erro**: Vermelho (#f8d7da)
- **Inativo**: Cinza (#f8f9fa)

## 🔄 Fluxo de Dados

### **Criar Cartão:**
1. Usuário preenche formulário
2. Frontend valida dados
3. Envia POST para `/api/credit-cards`
4. Backend valida e salva
5. Retorna sucesso
6. Frontend atualiza lista

### **Listar Cartões:**
1. Frontend faz GET para `/api/credit-cards`
2. Backend filtra por usuário autenticado
3. Retorna lista de cartões
4. Frontend renderiza cards

## 🐛 Troubleshooting

### **Problemas Comuns:**

1. **"Failed to load credit cards"**
   - Verifique se está logado
   - Verifique se o backend está rodando
   - Verifique o token JWT

2. **"Bank not found"**
   - Verifique se o banco existe no sistema
   - Execute o script `data.sql` para criar bancos de teste

3. **"User not found"**
   - Faça logout e login novamente
   - Verifique se o token está válido

4. **Formulário não envia**
   - Verifique se todos os campos obrigatórios estão preenchidos
   - Verifique se o formato dos dados está correto

## 🚀 Próximos Passos

### **Funcionalidades Futuras:**
1. **Faturas**: Gerenciar faturas dos cartões
2. **Transações**: Registrar gastos por cartão
3. **Relatórios**: Análise de gastos por cartão
4. **Compartilhamento**: Compartilhar cartão entre usuários
5. **Notificações**: Alertas de vencimento de fatura

## 📞 Suporte

Para dúvidas ou problemas:
1. Verifique este guia
2. Consulte os logs do backend
3. Verifique o console do navegador
4. Abra uma issue no repositório 