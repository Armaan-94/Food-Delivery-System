Food Delivery Microservices Platform
A scalable, distributed food delivery platform built using the Spring Cloud microservices architecture. This project implements modular RESTful services to handle user management, order processing, and delivery logistics, ensuring high availability and secure inter-service communication.

Architecture & Tech Stack
Framework: Spring Boot, Spring Cloud

Database: MySQL, Spring Data JDBC

Routing: Spring Cloud Gateway

Service Discovery: Netflix Eureka

Configuration: Spring Cloud Config Server

Core Services
The platform is divided into distinct, loosely coupled microservices. To ensure data decentralization and service independence, the core business services each manage their own dedicated MySQL database.

API Gateway: The centralized entry point handling request routing and global security filtering.

Service Registry (Eureka): Enables dynamic service discovery and load balancing across the platform.

Config Server: Provides centralized, externalized configuration management for all microservices.

Authentication Service: A dedicated security layer managing user login, token generation, and secure inter-service communication.

User Service: A RESTful microservice managing customer profiles and account data.

Order Service: A RESTful microservice handling order placement, tracking, and history.

Delivery Service: A RESTful microservice managing delivery partner assignments and logistics.

Key Features
Distributed Design: Fully modular architecture with independent service deployment and decentralized data management.

Dynamic Routing & Discovery: Seamless service-to-service communication and load balancing via Eureka and Spring Cloud Gateway.

Robust Security: Centralized authentication securing both external client requests and internal microservice interactions.

Externalized Configuration: Centralized property management allowing for seamless environment updates without rebuilding individual services.
