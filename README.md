# Load Balancer 🔀

![Java](https://img.shields.io/badge/Java-17-blue.svg)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5-green.svg)
![JUnit](https://img.shields.io/badge/JUnit-5-orange.svg)
![WireMock](https://img.shields.io/badge/WireMock-Mock_Server-yellow.svg)

## 🚀 Introduction
This project is a **Load Balancer** that distributes incoming requests across multiple service instances using different **load balancing strategies**.

### ✨ Features
- Implements **Round Robin** strategy.
- Tracks **unhealthy instances** and reroutes traffic to healthy ones.
- **Health check scheduler** to auto-recover failed instances.
- **Timeout handling** for slow/unresponsive instances.
- **Unit & Integration tests** with **JUnit 5**, **Mockito**, and **WireMock**.

---

## ⚙️ **Tech Stack**
- **Java 17**
- **Spring Boot 3.5**
- **Spring WebFlux (WebClient)**
- **WireMock (Mock API calls)**
- **JUnit 5 + Mockito (Testing Framework)**

---

## 🛠 **Setup & Installation**
### 🔹 Prerequisites:
- Install **Java 17**
- Install **Maven** (`brew install maven` or `sudo apt install maven`)
- Clone the repo:

  ```sh
      git clone https://github.com/alanjeremiah/loadbalancer.git
      cd loadbalancer
  ```
- Build the application
  ```sh
    mvn clean install
  ```
- Run the application
  ```sh
    spring-boot:run
  ```
- To test the application
  ```sh
    mvn test
  ```
### 🔹 Test the API:
- Use **Postman** or **curl** to test the /route API.
    ```sh
          curl -X POST http://localhost:8080/route \
               -H "Content-Type: application/json" \
               -d '{
                     "game": "Mobile Legends",
                     "gamerID": "GYUTDTE",
                     "points": 20
                   }'
  ```
- Sample Response
  ```sh
        {
            "game": "Mobile Legends",
            "gamerID": "GYUTDTE",
            "points": 20
        }
  ```
  
---

## 📌 Note on Testing with Postman

To test the **Load Balancer Router Application** using **Postman**, ensure that you have at least one instance of a separate project running that exposes the `/process` endpoint. 

### Steps to Set Up:
1. Create a separate Spring Boot project that includes a **REST controller** exposing the `/process` endpoint.
2. The **port** of this external service should match the instances configured in `application.properties` under:
3. If no external instances are running, **any request to `/route` will fail with**:
```sh
    { "error": "No healthy instance available" }
```
4. Run the **external service** before making API calls to this router application.
**This project serves only as a Load Balancer Router, and does not process requests directly!** 


