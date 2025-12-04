# Resumen de Despliegue — Pila IoT y DevOps

Este documento describe qué está desplegado en la nube, qué corre en local, la pila tecnológica IoT/DevOps y cómo reproducir el despliegue. Úsalo tal cual como brief para Gamma.

## Objetivo
Sistema conversacional que controla luces IoT vía ROS 2/micro-ROS y expone una UI (Vue) sobre un Gateway GraphQL que orquesta microservicios.

## Arquitectura (alto nivel)
- Frontend SPA: Vue 3 + Vite, servido por Nginx (contenedor).
- Gateway GraphQL: Node.js + Apollo Server. Hace proxy a microservicios REST.
- Microservicios Spring Boot:
  - Chatbot: procesa intents (LLM vía OpenRouter), persiste chat, dispara acciones.
  - Analytics: métricas/estado de luces y conteos de comandos en MongoDB.
- ROS CLI API: FastAPI publica comandos a ROS 2.
- micro-ROS Agent: puente DDS/UDP hacia el ESP32.
- Firmware: ESP32 (Arduino) ejecuta GPIO y reporta estado.

## Pila IoT
- ROS 2 Humble como middleware de robótica.
- micro-ROS para sistemas embebidos.
- Microcontrolador ESP32 con Arduino.
- Librerías cliente de ROS 2: rclpy (Python) y rclc (C).

## Pila DevOps
- Docker para contenerización.
- Docker Compose para orquestación (servicios locales y red bridge).
- Variables de entorno con archivos `.env` (no versionados).
- Railway para desplegar servicios en la nube (Docker images).
- Cloudflare Tunnel para exponer servicios locales al gateway en la nube (cuando se necesita).

## ¿Qué hay en la nube ahora?
- Gateway GraphQL (Railway): `https://gateway-production-214e.up.railway.app` (`/graphql`).
- Analytics (Railway): `https://analytics-production-0b74.up.railway.app`.
- MongoDB Atlas: conexión vía `MONGO_URI` y DB `MONGO_DB`.

Nota: El frontend se ejecuta localmente vía Docker Compose en `http://localhost:5173`, configurado para usar el Gateway en la nube.

## ¿Qué corre localmente?
Con `docker-compose.yml` en el root del repo:
- `frontend` (Nginx) → `http://localhost:5173` (build con `VITE_GRAPHQL_BASE` apuntando a la nube).
- `graphql-gateway` → `http://localhost:8090` (para entorno local puro).
- `chatbot-spring` → `http://localhost:8081`.
- `analytics-spring` → `http://localhost:8082`.
- `ros-cli-api` → `http://localhost:8000`.
- `micro-ros-agent` (UDP 8888) → puente con el ESP32.

## Variables de entorno (claves)
- Infraestructura/servicios:
  - `MONGO_URI`, `MONGO_DB` (MongoDB Atlas).
  - `OPENROUTER_API_KEY` (Chatbot → LLM via OpenRouter).
  - `ROS_DOMAIN_ID` (ROS 2 dominio de comunicación; por defecto 0).
- Gateway (Railway):
  - `ANALYTICS_BASE` → URL del servicio Analytics en Railway.
  - `CHATBOT_BASE` → Base del Chatbot (túnel Cloudflare si corre local).
  - `ROS_BASE` → Base de ROS API (túnel Cloudflare si corre local).

> Ver `.env.example` para un listado sin secretos. Mantén `.env` fuera del control de versiones.

## Despliegue (pasos resumidos)
1. Construir/push de imágenes Docker (local o CI) al registry (Docker Hub u otro).
2. Crear servicios en Railway para `analytics-spring` y `graphql-gateway` usando las imágenes públicas.
3. En `analytics-spring` (Railway): configurar `MONGO_URI` y `MONGO_DB`. Asegurar que la app escuche en `$PORT`.
4. En `graphql-gateway` (Railway): configurar `ANALYTICS_BASE` a la URL pública de Analytics.
5. Si `chatbot` y `ros` corren localmente, crear túneles Cloudflare:
   - `cloudflared tunnel --url http://localhost:8081` → usar URL en `CHATBOT_BASE`.
   - `cloudflared tunnel --url http://localhost:8000` → usar URL en `ROS_BASE`.
   - Redeploy del gateway tras actualizar variables.
6. Para desarrollo local completo: `docker compose up --build -d` en el root del repo.

## Pruebas (curl/PowerShell)
- Health (nube):
```powershell
Invoke-RestMethod -Uri "https://gateway-production-214e.up.railway.app/graphql" -Method Post -ContentType 'application/json' -Body (@{ query = '{ health }' } | ConvertTo-Json -Compress)
```
- Estado de luces (nube):
```powershell
Invoke-RestMethod -Uri "https://gateway-production-214e.up.railway.app/graphql" -Method Post -ContentType 'application/json' -Body (@{ query = '{ lightsState { sala habitacion cocina } }' } | ConvertTo-Json -Compress)
```
- Mutación `ask` (nube):
```powershell
$payload = @{ query = 'mutation($sid:String!, $msg:String!){ ask(sessionId:$sid, message:$msg){ message { id sessionId role text ts } } }'; variables = @{ sid = 'web-y53uilt'; msg = 'prende sala' } } | ConvertTo-Json -Compress
Invoke-RestMethod -Uri "https://gateway-production-214e.up.railway.app/graphql" -Method Post -ContentType 'application/json' -Body $payload
```
- Mutación `setRoom` (nube):
```powershell
$payload = @{ query = 'mutation { setRoom(room: "sala", on: true) }' } | ConvertTo-Json -Compress
Invoke-RestMethod -Uri "https://gateway-production-214e.up.railway.app/graphql" -Method Post -ContentType 'application/json' -Body $payload
```

## Notas y solución de problemas
- Endpoint del gateway es siempre `/graphql`. Errores `SyntaxError: Expected property name ...` indican un cuerpo no JSON o endpoint incorrecto.
- Si el gateway en la nube da 400 en mutaciones:
  - Verificar que `CHATBOT_BASE`/`ROS_BASE` apunten a servicios realmente accesibles (túneles activos si son locales).
  - Revisa variables tras cambios y realiza redeploy en Railway.
- Para el frontend local, el build incluye `VITE_GRAPHQL_BASE` en tiempo de compilación.

---
Este archivo está enlazado desde el README para que plataformas de documentación (p. ej., Deepwiki) lo indexen.
