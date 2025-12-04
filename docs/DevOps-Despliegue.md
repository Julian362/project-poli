# DevOps — Resumen de Despliegue (PPT/Gamma)

Breve guion para slides (sin pruebas, solo despliegue/infra).

## 1) Objetivo (1 slide)
- Unificar UI (Vue) con backend orquestado por GraphQL.
- Exponer microservicios (Spring, FastAPI) y capa IoT (ROS 2/micro-ROS) vía contenedores.
- Desplegar gateway/analytics en la nube y operar chatbot/ROS local con túneles.

## 2) Stack en la Nube (1-2 slides)
- Plataforma: Railway (servicios Docker de imagen pública).
- Servicios activos:
  - GraphQL Gateway: `https://gateway-production-214e.up.railway.app/graphql`.
  - Analytics: `https://analytics-production-0b74.up.railway.app`.
- Datos: MongoDB Atlas (`MONGO_URI`, `MONGO_DB`).

## 3) Stack Local (1-2 slides)
- Orquestación: Docker Compose (red bridge, resolución por servicio).
- Servicios:
  - Frontend (Nginx) en `http://localhost:5173` (build apunta al gateway cloud).
  - Chatbot Spring (8081), ROS API FastAPI (8000), Analytics (8082), Gateway (8090), micro-ROS Agent (8888/udp).
- Firmware: ESP32 (Arduino) via micro-ROS (fuera de Docker).

## 4) Contenerización (1 slide)
- Dockerfiles por servicio (multi-stage donde aplica).
- Imágenes publicadas bajo `project-poli/*` y `julian36/*` (para Railway).
- Frontend: build-time `ARG VITE_GRAPHQL_BASE` embebe endpoint del gateway cloud.

## 5) Flujo de Despliegue (Railway) (1 slide)
- Build & push imágenes (local/CI) → registry público.
- Crear servicio Analytics → set `MONGO_URI`, `MONGO_DB` → validar health.
- Crear servicio Gateway → set `ANALYTICS_BASE` a la URL de Analytics.
- Si Chatbot/ROS corren local: crear túneles, set `CHATBOT_BASE`/`ROS_BASE` y redeploy.

## 6) Variables y Secretos (1 slide)
- Core: `MONGO_URI`, `MONGO_DB`, `OPENROUTER_API_KEY`, `ROS_DOMAIN_ID`.
- Gateway: `ANALYTICS_BASE`, `CHATBOT_BASE`, `ROS_BASE`.
- Gestión: Railway Variables (producción) + `.env` local (no versionado). Referencia: `.env.example`.

## 7) Red y Túneles (1 slide)
- Cloudflare Tunnel (quick) para exponer `localhost`:
  - Chatbot → `http://localhost:8081` → `CHATBOT_BASE` (URL trycloudflare.com).
  - ROS API → `http://localhost:8000` → `ROS_BASE`.
- Opción estable: túneles nombrados en cuenta Cloudflare.

## 8) Frontend apuntando a la nube (1 slide)
- Compose: `build.args.VITE_GRAPHQL_BASE=https://gateway-production-214e.up.railway.app/graphql`.
- Resultado: SPA local consume GraphQL en Railway.

## 9) Mapa de responsabilidades (1 slide)
- Railway: Gateway/Analytics, scaling y dominios.
- Local: Chatbot/ROS, agent micro-ROS, ESP32.
- Atlas: persistencia.

---
Referencia extendida: `docs/Resumen-Despliegue.md`. Este archivo está enlazado en el README para indexación (Deepwiki).
