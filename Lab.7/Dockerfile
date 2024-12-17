# Folosim o imagine de bază cu OpenJDK (care include și javac pentru compilare)
FROM openjdk:11-jdk-slim

# Setăm directorul de lucru în container
WORKDIR /app

# Copiem fișierul SimpleWebApp.java din src în container
COPY src/SimpleWebApp.java .

# Compilăm aplicația Java
RUN javac SimpleWebApp.java

# Pornim aplicația Java
CMD ["java", "SimpleWebApp"]
