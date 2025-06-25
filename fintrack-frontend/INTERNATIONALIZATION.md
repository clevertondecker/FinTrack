# Internacionalização (i18n) - FinTrack

Este documento explica como configurar e usar a internacionalização no projeto FinTrack.

## 📦 Instalação das Dependências

Execute o seguinte comando no diretório `fintrack-frontend`:

```bash
npm install react-i18next i18next i18next-browser-languagedetector --legacy-peer-deps
```

## 🏗️ Estrutura Criada

```
src/
├── i18n/
│   ├── index.ts              # Configuração principal do i18n
│   └── locales/
│       ├── en.json           # Traduções em inglês
│       └── pt.json           # Traduções em português
├── components/
│   ├── LanguageSelector.tsx  # Componente seletor de idioma (com React Portal)
│   └── LanguageSelector.css  # Estilos do seletor
└── App.tsx                   # Importa configuração do i18n
```

## 🌐 Idiomas Suportados

- **🇺🇸 Inglês (en)** - Idioma padrão
- **🇧🇷 Português (pt)** - Idioma secundário

## 🔧 Como Usar

### 1. Importar o hook useTranslation

```tsx
import { useTranslation } from 'react-i18next';

const MyComponent = () => {
  const { t } = useTranslation();
  
  return <h1>{t('dashboard.title')}</h1>;
};
```

### 2. Usar traduções simples

```tsx
// Tradução simples
{t('common.loading')}

// Tradução com pluralização
{t('shares.shareCount', { count: 5 })}

// Tradução com interpolação
{t('shares.cardOf', { owner: 'João Silva' })}
```

### 3. Componente LanguageSelector

O componente `LanguageSelector` já está integrado no Dashboard e permite ao usuário alternar entre idiomas.

**Características:**
- ✅ **React Portal**: Dropdown renderizado fora da hierarquia DOM
- ✅ **Posicionamento dinâmico**: Calcula posição automaticamente
- ✅ **Click outside**: Fecha ao clicar fora
- ✅ **Animações suaves**: Transições CSS
- ✅ **Z-index garantido**: Sempre aparece na frente

## 📝 Adicionando Novas Traduções

### 1. Adicionar no arquivo `en.json`:

```json
{
  "novaSecao": {
    "titulo": "New Title",
    "descricao": "New description"
  }
}
```

### 2. Adicionar no arquivo `pt.json`:

```json
{
  "novaSecao": {
    "titulo": "Novo Título",
    "descricao": "Nova descrição"
  }
}
```

### 3. Usar no componente:

```tsx
{t('novaSecao.titulo')}
```

## 🎯 Funcionalidades Implementadas

### ✅ Dashboard
- Títulos e descrições dos cards
- Botões de navegação
- Mensagens de boas-vindas
- Seletor de idioma integrado

### ✅ MyShares (Minhas Divisões)
- Títulos e labels
- Status das faturas
- Mensagens de erro e loading
- Pluralização (share/shares, item/items)

### ✅ Componentes Comuns
- Botões (Salvar, Cancelar, Voltar, etc.)
- Estados (Loading, Error, Success)
- Formulários (Nome, Email, Senha)

### ✅ LanguageSelector
- Dropdown com React Portal
- Posicionamento dinâmico
- Animações suaves
- Click outside para fechar
- Z-index garantido

## 🔄 Como Funciona

1. **Detecção Automática**: O sistema detecta automaticamente o idioma do navegador
2. **Persistência**: A escolha do usuário é salva no localStorage
3. **Fallback**: Se uma tradução não existir, usa o inglês como fallback
4. **Interpolação**: Suporte a variáveis nas traduções
5. **Pluralização**: Suporte automático a singular/plural
6. **React Portal**: Dropdown sempre visível acima de outros elementos

## 🚀 Implementação Completa

✅ **Dependências instaladas**
✅ **Configuração i18n**
✅ **Componentes traduzidos**
✅ **Seletor de idioma funcional**
✅ **Documentação atualizada**

## 📚 Recursos Úteis

- [Documentação react-i18next](https://react.i18next.com/)
- [Documentação i18next](https://www.i18next.com/)
- [Pluralização](https://www.i18next.com/translation-function/plurals)
- [Interpolação](https://www.i18next.com/translation-function/interpolation)
- [React Portal](https://react.dev/reference/react-dom/createPortal) 