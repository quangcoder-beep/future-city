# 1. Stage Build
FROM gradle:jdk17-alpine AS build
WORKDIR /delivery-game

# Copy toan bo du an vao de khong bi loi cau truc multi-project
COPY . .

# Chay build jar cho server
RUN ./gradlew :server-spring:bootJar --no-daemon

# 2. Stage Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /delivery-game

# Lay file jar da build thanh cong
COPY --from=build /delivery-game/server-spring/build/libs/*.jar ./server.jar
# Copy file model cho he thong vat ly (lay tu stage build)
COPY --from=build /delivery-game/assets/models/city_map.glb ./assets/models/

EXPOSE 8080 54555/tcp 54777/udp
CMD ["java", "-jar", "server.jar"]