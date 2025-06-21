#!/bin/bash

echo "🔨 Build FinTranck - Backend + Frontend"
echo "📁 Diretório: $(pwd)"
echo "⏰ $(date)"

# Build do Backend
echo "🔧 Build do Backend..."
if command -v mvn &> /dev/null; then
    mvn clean package -DskipTests
else
    ./mvnw clean package -DskipTests
fi

if [ $? -eq 0 ]; then
    echo "✅ Backend buildado com sucesso!"
    echo "📦 JAR: target/fintrack-*.jar"
else
    echo "❌ Erro no build do Backend"
    exit 1
fi

# Build do Frontend
echo "🎨 Build do Frontend..."
cd fintrack-frontend

if [ ! -d "node_modules" ]; then
    echo "📦 Instalando dependências do frontend..."
    npm install
fi

echo "🔨 Criando build de produção..."
npm run build

if [ $? -eq 0 ]; then
    echo "✅ Frontend buildado com sucesso!"
    echo "📦 Build: fintrack-frontend/build/"
else
    echo "❌ Erro no build do Frontend"
    exit 1
fi

cd ..

echo "🎉 Build completo!"
echo "📊 Backend JAR: target/fintrack-*.jar"
echo "🎨 Frontend Build: fintrack-frontend/build/" 