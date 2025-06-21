#!/bin/bash

echo "🚀 Iniciando FinTranck Backend..."
echo "📁 Diretório: $(pwd)"
echo "⏰ $(date)"

# Verificar se o Maven está disponível
if command -v mvn &> /dev/null; then
    echo "✅ Maven encontrado"
    mvn spring-boot:run
else
    echo "⚠️  Maven não encontrado, tentando usar Maven Wrapper..."
    if [ -f "./mvnw" ]; then
        echo "✅ Maven Wrapper encontrado"
        ./mvnw spring-boot:run
    else
        echo "❌ Maven não encontrado. Instale o Maven ou use o Maven Wrapper."
        exit 1
    fi
fi 