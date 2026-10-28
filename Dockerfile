# ETAPA 1: Build da aplicação
FROM maven:3.9.6-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
# Baixa as dependências primeiro para aproveitar o cache do Docker
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# ETAPA 2: Execução (imagem final leve)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# Copia apenas o .jar gerado na etapa de build
COPY --from=build /app/target/backend-2-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]