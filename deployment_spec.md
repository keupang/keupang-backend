# Keupang MSA Deployment & Infrastructure Specification

## 1. Project Overview
- **Project Name:** Keupang (MSA E-Commerce)
- **Environment:** Local On-Premise (Mini PC)
- **OS:** Windows 11 Pro + WSL2 (Ubuntu)
- **Hardware:** AMD Ryzen 7 5700U / 32GB RAM / 500GB NVMe

## 2. Software Architecture (Current Status)

### 2.1 Microservices (Spring Boot 3.x / Java 17)
| Service Name | Port | Description | Docker Context |
|---|---|---|---|
| `keupang-eureka-server` | 8761 | Service Discovery | `./keupang-eureka-server` |
| `keupang-config-server` | 9000 | Configuration Management | `./keupang-config-server` |
| `keupang-gateway` | 8080 | API Gateway (Entry Point) | `./keupang-gateway` |
| `keupang-auth` | - | Authentication & Authorization | `./keupang-auth` |
| `keupang-user` | - | User Management | `./keupang-user` |
| `keupang-product` | - | Product Catalog | `./keupang-product` |
| `keupang-stock` | - | Inventory Management | `./keupang-stock` |
| `keupang-review` | - | User Reviews | `./keupang-review` |

### 2.2 Data Infrastructure
- **Message Broker:** N/A (Kafka planned but not currently in compose)
- **Database:** MySQL 8.0 (Container: `service-mysql`)
    - *Note:* Codebase currently uses MySQL drivers. Future migration to PostgreSQL is planned.
- **Cache:** Redis (Container: `service-redis`)
    - Port: 6379

### 2.3 DevOps & CI/CD
- **Containerization:** Docker & Docker Compose
- **CI/CD:** Jenkins (Container: `jenkins`)
    - Port: 9090 (Host mapped)
    - Pipeline: Git Trigger -> Build (Gradle) -> Docker Build -> Deploy

## 3. Future Infrastructure Roadmap: "Home Data Center"

The goal is to evolve the Mini PC into a professional-grade network lab. 

### Candidate Architectures

#### Option 1: Virtualized Network Appliance (Selected Candidate for Dev+Ops)
*   **Concept:** "Software defined everything." Running enterprise firewall OS as a VM.
*   **Cost:** ~20,000 KRW (USB LAN Card)
*   **Hardware:** Mini PC + USB NIC + Existing Router (AP Mode)
*   **Software:** **Proxmox VE** (Hypervisor) + **pfSense/OPNsense** (VM)
*   **Topology:** `[Internet] -> [USB NIC] -> [Proxmox(pfSense)] -> [Internal VM Network]`
*   **Learning Goals:** Proxmox Virtualization, Firewall Policies, NAT, VPN, Load Balancing.

#### Option 2: MikroTik Hardware ("The Cost-Performance King")
*   **Concept:** Dedicated hardware router with enterprise features.
*   **Cost:** 80,000 ~ 100,000 KRW
*   **Hardware:** MikroTik hEX S (RB760iGS)
*   **Pros:** Deep dive into packet flow, extremely stable, explicitly separates Network vs Compute.

#### Option 3: Cisco Legacy Lab
*   **Concept:** Classic Enterprise CLI experience.
*   **Cost:** 30,000 ~ 50,000 KRW (Used Catalyst 2960)
*   **Pros:** Industry standard CLI experience, VLAN physical simulation.
*   **Cons:** High noise, power consumption, bulky.

### Recommended Path
**Option 1 (Proxmox + pfSense)** is the recommended path for a "Developer-First" approach, allowing flexible snapshots, rebuilding, and zero hardware footprint (aside from a USB dongle).
