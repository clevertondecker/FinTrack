#!/bin/bash

echo "🚀 Iniciando FinTranck - Backend + Frontend"
echo "📁 Diretório: $(pwd)"
echo "⏰ $(date)"

# Função para limpar processos ao sair
cleanup() {
    echo "🛑 Parando todos os processos..."
    kill $BACKEND_PID $FRONTEND_PID 2>/dev/null
    exit 0
}

# Capturar Ctrl+C para limpeza
trap cleanup SIGINT

# Iniciar backend em background
echo "🔧 Iniciando Backend..."
if command -v mvn &> /dev/null; then
    mvn spring-boot:run > backend.log 2>&1 &
else
    ./mvnw spring-boot:run > backend.log 2>&1 &
fi
BACKEND_PID=$!

# Aguardar backend inicializar
echo "⏳ Aguardando backend inicializar..."
sleep 10

# Verificar se backend está rodando
if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo "✅ Backend iniciado com sucesso!"
else
    echo "⚠️  Backend pode estar ainda inicializando..."
fi

# Iniciar frontend em background
echo "🎨 Iniciando Frontend..."
cd fintrack-frontend
npm start > ../frontend.log 2>&1 &
FRONTEND_PID=$!
cd ..

echo "🎉 FinTranck iniciado!"
echo "📊 Backend: http://localhost:8080"
echo "🎨 Frontend: http://localhost:3000"
echo "📝 Logs: backend.log e frontend.log"
echo ""
echo "Pressione Ctrl+C para parar todos os serviços"

# Aguardar indefinidamente
wait 