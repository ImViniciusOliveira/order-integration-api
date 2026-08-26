# ==========================================
# ESTÁGIO 1: BUILD (Compilação)
# ==========================================
FROM eclipse-temurin:26-jdk-alpine AS builder

WORKDIR /build

# Copia os arquivos de dependência primeiro (otimiza o cache do Docker)
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Baixa as dependências offline
RUN ./mvnw dependency:go-offline -B

# Copia o código fonte e compila o projeto ignorando os testes para ser mais rápido
COPY src src
RUN ./mvnw clean package -DskipTests

# ==========================================
# ESTÁGIO 2: RUN (Execução)
# ==========================================
FROM eclipse-temurin:26-jre-alpine

WORKDIR /app

# Copia apenas o arquivo .jar gerado no estágio anterior
COPY --from=builder /build/target/*.jar app.jar

# Expõe a porta padrão do Spring Boot
EXPOSE 8080

# Comando para iniciar a aplicação
ENTRYPOINT ["java", "-jar", "app.jar"]