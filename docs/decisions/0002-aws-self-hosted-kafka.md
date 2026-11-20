# 0002. AWS with ECS Fargate and self-hosted single-broker Kafka instead of MSK
Date: 2026-11-19. Status: accepted

## Context
- The system must run in a public cloud for the portfolio, by 3 Jan, paid from a personal budget.
- My existing cloud experience is Azure. I want AWS experience, and it supports CLF-C02 preparation.
- Three containers (web, shipment-service, tracking-service), two PostgreSQL databases, one Kafka broker.
- Amazon MSK (managed Kafka) bills for broker instances running all day, with several brokers across zones.
  TODO before merging: check the real monthly figure in the AWS pricing calculator and write it here.
- Nobody depends on this system, so high availability is not a requirement.

## Decision
- Run the three services on ECS Fargate behind one ALB with path routing, tasks in public subnets (no NAT gateway cost).
- One RDS PostgreSQL instance holding two databases.
- Run Kafka (KRaft, one broker) on one small EC2 instance with an EBS volume, reachable only from the service tasks' security group.
- Secrets in SSM Parameter Store. Deploy from GitHub Actions with OIDC.
- The README states clearly that this Kafka setup is not production-grade.

## Alternatives considered
- **Azure.** Cheaper and quicker for this design. It lost on purpose: I already know Azure, the goal is to learn AWS.
- **Amazon MSK.** Managed, replicated, patched. It lost on cost: the bill is out of proportion for a demo.
- **Kafka as a Fargate task.** No server to patch, but the broker needs durable disk and a stable address, and Fargate makes both awkward.
- **EKS (Kubernetes).** Powerful and expensive in money and in time. Already in the "cut from MVP" list.
- **Everything on one EC2 with Docker Compose.** Cheapest and fastest. Kept as the fallback deploy if the plan slips, but it teaches less about ECS.

## Consequences
Easier:
- Low, predictable cost, and the same Kafka image as local Compose.
- Practical use of ECS, ALB, RDS, ECR, SSM, IAM and OIDC.

Harder, and accepted:
- I patch and monitor the Kafka EC2 myself.
- One broker, replication factor 1, one disk: no high availability, and a broker failure interrupts event flow.
- The source of truth is `shipment_db`. If outbox rows are kept, the log can in principle be republished, but I am not building that tool in the MVP.
- The EC2 instance needs a Terraform module and a runbook entry, which costs time in W5.
- Anyone reading this repo must be told this is a learning setup, not a template for production.
