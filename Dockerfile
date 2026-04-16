# Estágio de Build
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copia os arquivos do projeto
COPY pom.xml .
COPY src ./src

# EXECUTA O BUILD (Esta linha estava faltando)
RUN mvn clean package -DskipTests

# Estágio de Execução
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copia o jar gerado no estágio anterior
COPY --from=build /app/target/dumply-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENV JAVA_OPTS="-Xms256m -Xmx512m"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]