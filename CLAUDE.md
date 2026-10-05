# Java Spring Boot Project

This is a Spring Boot application using Java.

## Project Information

- **Spring Boot Version**: 4.1.1 (Spring Framework 7)
- **Java Version**: 21 
- **Maven**: 
- **Package Structure**: `de.jkueck.*`

## Project Structure
- `src/main/java/` - Application source code
- `src/main/resources/` - Configuration files
- `src/test/java/` - Test source code
- `pom.xml`

## Architecture
- `controller/` - REST endpoints
- `service/` - Business logic
- `dto/` - Data transfer objects
- `config/` - Configuration classes
- `exception/` - Exception classes
- `client/` - External Api clients

## Conventions
- Use constructor injection over field injection
- Follow layered architecture (Controller → Service)
- Use DTOs for API request/response, not entities
- Handle exceptions with @ControllerAdvice
- Use Bean Validation annotations for input validation
- Use Lombok for boilerplate code reduction (e.g., @Data, @Builder)
- RFC 7231 compliant REST endpoints (GET, POST, PUT, DELETE)
- RFC 7807 compliant error responses (Problem Details)
- 

## Testing
- Unit test services
- Integration test with @SpringBootTest
- Use @WebMvcTest for controller tests
