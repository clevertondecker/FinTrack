#!/bin/bash

echo "🎨 Iniciando FinTranck Frontend..."
echo "📁 Diretório: $(pwd)/fintrack-frontend"
echo "⏰ $(date)"

# Verificar se o diretório frontend existe
if [ ! -d "fintrack-frontend" ]; then
    echo "❌ Diretório fintrack-frontend não encontrado!"
    exit 1
fi

# Navegar para o diretório frontend
cd fintrack-frontend

# Verificar se o Node.js está disponível
if command -v node &> /dev/null; then
    echo "✅ Node.js encontrado: $(node --version)"
else
    echo "❌ Node.js não encontrado. Instale o Node.js primeiro."
    exit 1
fi

# Verificar se o npm está disponível
if command -v npm &> /dev/null; then
    echo "✅ npm encontrado: $(npm --version)"
else
    echo "❌ npm não encontrado. Instale o npm primeiro."
    exit 1
fi

# Instalar dependências se necessário
if [ ! -d "node_modules" ]; then
    echo "📦 Instalando dependências..."
    npm install
fi

echo "🚀 Iniciando servidor de desenvolvimento..."
npm start 