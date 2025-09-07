# GenAI Workflow Documentation

## Overview
During the development of this project, GenAI tools were actively used to accelerate design, coding, and documentation tasks. The workflow combined **ChatGPT** for exploration, technical discussions, and iterative prompt refinement, together with **Cursor AI** as the coding assistant directly integrated into the IDE.

This section documents how GenAI was leveraged, the decisions supported by it, and the iterative process that led to the MVP.

---

## 1. Role of ChatGPT
ChatGPT was used as a **strategic and technical partner** throughout the project:

- **Technical decisions:** Discussed architectural trade-offs (e.g., consistency vs. availability, optimistic locking, synchronization frequency, metrics, tracing).  
- **Design exploration:** Debated how to structure services (`store-service` vs `central-service`), package organization, DTO responsibilities, and concurrency strategies.  
- **Business perspective:** Considered how requirements such as “stock synchronization every 15 minutes” or “manual push triggers” would affect both usability and system reliability.  
- **Prompt crafting:** Iteratively refined long and detailed prompts to ensure that Cursor generated code aligned with layered architecture (controllers, services, repositories, DTOs, mappers).  

---

## 2. Iterative Prompting
The project was intentionally built **step by step**, instead of generating the entire solution at once. This had two benefits:
1. **Understanding:** Each iteration focused on a single aspect (e.g., first health endpoints, then stock adjustment, then synchronization, then metrics).  
2. **Control:** By refining prompts with ChatGPT before passing them to Cursor, code generation was guided and aligned with the MVP goals.  

Examples of iterations:
- Prompt 1: Generate a minimal `store-service` with electronic/tech products and endpoints for health/products/stock.  
- Prompt 2: Add optimistic locking and retries for stock adjustments.  
- Prompt 3: Implement synchronization (push and pull) between store and central.  
- Later prompts: Add observability (TraceId, Micrometer metrics), Docker support, Postman collections, and documentation (`run.md`, `prompts.md`).  

---

## 3. Role of Cursor
Once prompts were validated with ChatGPT, they were **handed over to Cursor**, which:
- Generated the actual Java/Spring Boot code.  
- Integrated code directly in the project structure (`store-service`, `central-service`).  
- Allowed quick editing, running, and testing within the IDE.  

Cursor acted as the **execution engine**, while ChatGPT acted as the **design and planning assistant**.

---

## 4. MVP Definition
The Minimum Viable Product (MVP) aimed to:
- Run two independent Spring Boot services (`store-service`, `central-service`).  
- Support core inventory operations: product listing, stock queries, stock adjustments.  
- Synchronize stock periodically and manually between store and central, using Last-Write-Wins.  
- Ensure concurrency safety with optimistic locking and retries.  
- Provide basic observability (logs, traceId, custom metrics with Micrometer).  
- Run locally with Maven or Docker, with Postman collection available for easy testing.  

The MVP balanced **completeness** (covering the requirements of the assignment) and **simplicity** (H2 in-memory database, no external dependencies).

---

## 5. Outcome
By combining ChatGPT and Cursor:
- Development was faster and iterative, without losing understanding of the code.  
- Documentation (`README.md`, `run.md`, `prompts.md`, Postman collection) was generated alongside the code.  
- The final MVP demonstrates both **technical soundness** (architecture, concurrency, observability) and **modern development practices** (GenAI-assisted coding, containerization, automated testing).  

---

📌 *This workflow shows how GenAI can be integrated responsibly into software projects: ChatGPT for reasoning and planning, Cursor for code generation and integration, and the developer in full control of the iterative process.*  
