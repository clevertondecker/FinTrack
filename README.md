# FinTrack

A simple finance tracking application built with Spring Boot and MySQL.

## Prerequisites

- [Docker](https://www.docker.com/get-started)
- [Java 17+](https://adoptopenjdk.net/)
- [Maven](https://maven.apache.org/) (or use the Maven Wrapper: `./mvnw`)

## 1. Clone the repository

```sh
git clone <your-repo-url>
cd FinTranck
```

## 2. Configure environment variables

Create a `.env` file in the project root (same folder as `docker-compose.yml`):

```env
MYSQL_DATABASE=fintrackdb
MYSQL_USER=fintrack
MYSQL_PASSWORD=your_strong_password_here
MYSQL_ROOT_PASSWORD=your_root_password_here
```

> **IMPORTANT SECURITY NOTE:**
> - The values above are **examples**. Always set your own strong, unique passwords for `MYSQL_PASSWORD` and `MYSQL_ROOT_PASSWORD`.
> - The password you set in your `.env` file **must match** the password used in your Spring Boot configuration (`application.properties` or environment variables).
> - **Never use these example passwords in production! Never expose your secrets publicly.**

## 3. Start MySQL with Docker Compose

```sh
docker-compose up -d
```

This will start a MySQL 8 container with the credentials from your `.env` file.

## 4. Configure Spring Boot database connection

The application reads database credentials from environment variables. Example `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/${MYSQL_DATABASE}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Sao_Paulo
spring.datasource.username=${MYSQL_USER}
spring.datasource.password=${MYSQL_PASSWORD}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.jpa.hibernate.ddl-auto=update
spring.jpa.database-platform=org.hibernate.dialect.MySQL8Dialect
```

> **Tip:** If running the app outside Docker Compose, export the variables in your terminal:
> ```sh
> export MYSQL_DATABASE=fintrackdb
> export MYSQL_USER=fintrack
> export MYSQL_PASSWORD=your_strong_password_here
> export MYSQL_ROOT_PASSWORD=your_root_password_here
> ```
> Replace the values with your own if you changed them in the `.env` file.

## 5. Build and run the application

```sh
./mvnw spring-boot:run
```

Or, if you have Maven installed:

```sh
mvn spring-boot:run
```

## 6. API Usage

- The user registration endpoint is available at:
  - `POST /api/users/register`
  - Example JSON body:
    ```json
    {
      "name": "John Doe",
      "email": "john@example.com",
      "password": "yourpassword"
    }
    ```

## 6. Troubleshooting

- **MySQL container keeps restarting:**
  - Check your `.env` file for correct variables and values.
  - Run `docker-compose down -v` and then `docker-compose up -d` to reset the database volume.
- **Access denied for user:**
  - Make sure the user and password in `.env` match those in `application.properties` or your exported variables.
- **Public Key Retrieval is not allowed:**
  - Ensure your JDBC URL contains `allowPublicKeyRetrieval=true`.
- **Environment variables not working in Spring Boot:**
  - Export them in your terminal before running the app, or set them in your IDE run configuration.

## 7. Best Practices

- Never commit your `.env` file or any secrets to version control.
- Use environment variables for all sensitive configuration.
- Use Docker volumes for persistent database data in development.
- For production, use strong passwords and secure your database.

---

Happy coding!