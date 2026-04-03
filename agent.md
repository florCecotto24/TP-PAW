# Project Overview
This is a Java web application based on **Spring Framework 5.3**, organized as a multi-module **Maven** project. It uses a classic layered architecture with separated contracts and implementations.
## Architecture
The project is divided into the following modules:
**models**: Contains the domain entities (POJOs).
**persistence-contracts**: Defines the interfaces for the Data Access Objects (DAOs).
**persistence**: Provides the JDBC-based implementation of the DAOs, using ‘JdbcTemplate’.
**service-contracts**: Defines the interfaces for the Service layer.
**services**: Provides the implementation of the Services, which coordinate business logic.
**webapp**: The web layer, containing Spring MVC controllers, JSPs, and application configuration.
## Technologies
**Backend**: Java 21, Spring 5.3 (MVC, JDBC, Context, Test).
**Database**: PostgreSQL (Production), HSQLDB (Testing).
**Frontend**: JSP (JavaServer Pages) located in ‘webapp/src/main/webapp/WEB-INF/views/’.
**Build Tool**: Maven.
**Server**: Jetty (via ‘jetty-maven-plugin').
## Building and Running
### Prerequisites
Java 21
Maven
PostgreSQL (configured with database ‘paw’, user ‘pawdbuser’, and password ‘pawdbpassword as per ‘WebConfig.java’)
### Key Commands
**Build the entire project**:
mvn clean install
**Run the web application**:
mvn jetty:run -pl webapp
The application will be available at:
‘http://localhost:8080/’
**Run tests**:
mvn test
## Development Conventions
### Coding Style
**Dependency Injection**: Use constructor-based injection with Spring's ‘@Autowired’.
**Configuration**: The project uses a mix of Java-based configuration (‘@Configuration’ classes like ‘WebConfig.java’) and ‘web.xml’ for bootstrapping.
**Persistence**: Data access is implemented using plain SQL with Spring's ‘JdbcTemplate’. Schema management is handled via ‘schema.sql’ during application startup.
###Dependency Management
**Centralized Versions**: All dependency versions must be managed in the root ‘pom.xml’ using Maven properties.
**Dependency Management**: Use the ‘<dependencyManagement>’ section in the root ‘pom.xml’ to define the version and scope of all external dependencies.
**Module Dependencies**: Modules should reference dependencies without specifying their version, as they should be inherited from the parent.
**Local Module References**: Modules that depend on other modules in the same project should use ‘${project.version}’.
### Testing Practices
**Unit Testing**: Use JUnit 5 and Mockito.
**Persistence Testing**: DAO tests use an in-memory HSQLDB database (configured in ‘TestConfiguration.java’) to verify SQL queries and data mapping.
**Service Testing**: Services are typically tested by mocking their DAO dependencies.
### Directory Structure
‘src/main/java’: Java source files.
‘src/main/resources’: Configuration and static resources (e.g., ‘schema.sql’).
‘src/test/java’: Test source files.
‘webapp/src/main/webapp’: Web resources (JSPs, CSS, ‘WEB-INF/web.xm’l).
