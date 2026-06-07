---
title: "Spring Boot 3 Production Skill"
description: "Enforces production-ready Spring Boot 3.x architectures, Java 17/21 patterns, Jakarta EE packages, and modern security conventions."
globs: "**/*.java, **/pom.xml, **/build.gradle, **/*.properties, **/*.yml"
---

# Spring Boot 3 & Java 17/21 Best Practices

Use this skill when initializing or updating components in a Spring Boot 3 web application. Ensure all generated code adheres strictly to these conventions.

## 1. Core Stack Framework Rules
* Use Java 17 or Java 21 LTS syntax (Records, pattern matching, text blocks, enhanced switch expressions).
* Target Spring Boot 3.x+ dependencies.
* Always import the `jakarta.*` namespace for persistence, validation, and servlets. Never use legacy `javax.*` packages.

## 2. Architecture & Design Patterns
* Enforce a clean layered architecture: Controller -> Service -> Repository -> Domain Entity/Database.
* Restrict data exposing: Always map entities to clean Data Transfer Objects (DTOs) using Java Records before leaving the Controller layer.
* Never expose database auto-incrementing raw IDs directly to the presentation layers.
* Always use constructor-based dependency injection. Omit `@Autowired` fields entirely.

## 3. REST API Design Standards
* Use `@RestController` and explicit mapping annotations (`@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`).
* Validate all incoming request payloads using `@Valid` along with standard `jakarta.validation.constraints` annotations (e.g., `@NotBlank`, `@NotNull`, `@Size`).
* Ensure standardized HTTP status codes return appropriately: `201 Created` for creations, `204 No Content` for successful empty payloads, and structured `4xx/5xx` responses for exceptions.
* add openapi standard documentation annotations and descriptions

## 4. Exception Handling
* Centralize application exception controls via a global `@RestControllerAdvice`.
* Intercept failures cleanly using individual `@ExceptionHandler` declarations.
* Return a structured Problem Details error payload matching RFC 9457 specifications containing `timestamp`, `status`, `error`, and context-specific validation `errors`.

## 4A. Input validations and raise exceptions
 * validate all inputs for type, range and mandatory per requirements
 * proper error message with the cause of the failure and name of the property included in message


## 5. Persistence & Transaction Management
* Extend `JpaRepository<Entity, Id>` or use `CrudRepository` structures for database operations.
* Annotate write-heavy or composite service methods using `@Transactional(readOnly = false)`. 
* Use `@Transactional(readOnly = true)` for service retrieval operations to maximize performance.
* Explicitly manage database relationships using lazy fetching strategies (`FetchType.LAZY`) on `@OneToMany` or `@ManyToMany` properties.

## 6. Spring Security 6.x Conventions
* Implement security contexts via the component-based configuration approach using a `@Bean` of type `SecurityFilterChain`.
* Avoid legacy methods: Never use deprecated method chains such as `.authorizeRequests()` or `.and()`.
* Utilize lambda-style DSL configuration patterns explicitly:
  ```java
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
      http
          .csrf(csrf -> csrf.disable())
          .authorizeHttpRequests(auth -> auth
              .requestMatchers("/api/v1/public/**").permitAll()
              .anyRequest().authenticated()
          )
          .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
      return http.build;
  }
  ```

## 7. Testing Strategy
* Write comprehensive slicing tests using `@WebMvcTest` for API endpoint controllers.
* Leverage `@DataJpaTest` to validate isolated queries and repository layers.
* Utilize `Testcontainers` rather than in-memory H2 databases to conduct accurate integration verification.
