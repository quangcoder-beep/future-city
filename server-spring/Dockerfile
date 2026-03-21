# 1. Stage Build
FROM gradle:jdk17-alpine as build
WORKDIR /delivery-game

# Copy toàn bộ dự án vào ngay từ đầu để không bị lỗi cấu trúc multi-project
COPY . . 

# Chạy build jar cho server
RUN ./gradlew :server-spring:bootJar --no-daemon

# 2. Stage Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /delivery-game

# Lấy file jar đã build thành công
COPY --from=build /delivery-game/server-spring/build/libs/*.jar ./server.jar
# Coppy file model cho hệ thống vật lý
COPY assets/models/currentcity.glb ./assets/models/

EXPOSE 8080 54777 54555
CMD ["java", "-jar", "server.jar"]