FROM gradle:jdk21-alpine AS builder

WORKDIR /usr/app

COPY --chown=gradle:gradle . .

RUN gradle build -x test


FROM eclipse-temurin:21-jre

COPY --from=builder /usr/app/build/libs/backend-all.jar backend.jar

ENTRYPOINT ["java", "-jar", "backend.jar"]