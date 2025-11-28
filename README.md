# Project Poli

Sistema conversacional de control inteligente de IoT (luces) que integra una SPA en Vue 3 con un Gateway GraphQL (Node/TypeScript), microservicios Spring Boot (Chatbot + Analytics), una API FastAPI para puente ROS 2, micro-ROS y firmware ESP32.

## Objetivo
Permitir que el usuario envíe comandos naturales como "prende cocina" y que el sistema procese la intención, ejecute acciones sobre dispositivos físicos y actualice el estado en la interfaz con baja latencia y trazabilidad analítica.

## Capas y Flujo Resumido
1. **Frontend (Vue 3)**: Chat + vista de historial/estadísticas. Almacena `sessionId` en `localStorage`, aplica actualizaciones optimistas y refresca estado de luces vía consultas GraphQL.
2. **Gateway GraphQL (Apollo Server)**: Unifica 5 queries y 5 mutations, hace proxy a microservicios REST y expone un esquema tipado.
3. **Microservicios Spring**:
   - Chatbot: Persistencia de mensajes, detección de intención con LLM (OpenRouter) y ejecución de acciones.
   - Analytics: Conteo de comandos, estado agregado de habitaciones y métricas.
4. **ROS Control (FastAPI)**: Recibe solicitudes de encendido/apagado y publica caracteres en tópicos ROS 2.
5. **micro-ROS + ESP32**: El agente micro-ROS traduce DDS a UDP para el firmware; el ESP32 ejecuta GPIO y reporta eco.
6. **MongoDB**: Colecciones especializadas (`chat_messages`, `command_counts`, `lights_state`, `action_events`).

## Componentes Clave
| Componente | Tech | Puerto | Rol |
|------------|------|--------|-----|
| Frontend Vue | Vue 3, Composition API | 5173 / Nginx 80 | Interfaz de chat y estado |
| GraphQL Gateway | Node.js, TypeScript, Apollo | 8090 | Agregación y orquestación |
| Chatbot Service | Spring Boot | 8081 | Lógica conversacional e intents |
| Analytics Service | Spring Boot | 8082 | Estadísticas y estado global |
| ROS CLI API | FastAPI, ROS 2 | 8000 | Puente HTTP→ROS 2 |
| micro-ROS Agent | micro-ROS | 8888/UDP | Transporte DDS↔UDP |
| ESP32 Firmware | Arduino, C++ | - | Control físico LEDs |
| MongoDB | NoSQL | 27017 | Persistencia |

## Comandos Caracter (ESP32)
```
H/h  Habitación ON/OFF
C/c  Cocina ON/OFF
S/s  Sala ON/OFF
X    Consultar estado
```

## Flujo "prende cocina" (Resumen)
1. Usuario escribe mensaje → UI lo añade optimistamente y detecta acción pendiente.
2. Mutation `ask(sessionId, message)` al Gateway.
3. Gateway proxy → Chatbot: guarda mensaje, envía historial al LLM, obtiene intent.
4. Chatbot registra comando (Analytics) y ejecuta control de luz vía ROS API.
5. ROS API publica carácter (`C`) en tópico; micro-ROS lo entrega al ESP32.
6. Firmware activa GPIO (LED) y envía respuesta; Analytics actualiza conteos y estado.
7. Respuesta vuelve al frontend; chip pasa de `pending` a `success`; se refresca `lightsState`.

## Variables de Entorno (ejemplo `.env` raíz)
```
MONGO_URI=mongodb://mongo:27017
MONGO_DB=project_poli
OPENROUTER_API_KEY=sk-...
PORT_GATEWAY=8090
PORT_CHATBOT=8081
PORT_ANALYTICS=8082
PORT_ROS_API=8000
```
(No versionar: la configuración está excluida por `.gitignore`).

## Estructura Simplificada
```
project-poli/
├─ backend/
│  ├─ graphql-gateway/src/index.ts
│  ├─ chatbot-spring/
│  └─ analytics-spring/
├─ frontend/vue-chatbot/src/
│  ├─ views/ (ChatView.vue, HistoryStatsView.vue)
│  └─ components/ (ActionStateChip.vue, ...)
├─ ros/ (docker-compose.yml, ros-api/)
├─ sp32/code/code.ino
└─ README.md
```

## Inicio Rápido (Local)
Requisitos: Docker y Docker Compose.

```powershell
# Clonar
git clone https://github.com/Julian362/project-poli.git
cd project-poli

# Crear archivo .env (ver ejemplo arriba)
notepad .env

# Levantar servicios (según compose existente)
docker compose up --build

# Acceder al frontend (si está en compose con Nginx)
http://localhost:5173  # o el puerto asignado

# Probar GraphQL health
curl http://localhost:8090/graphql -H "Content-Type: application/json" -d '{"query":"{ health }"}'
```

## Despliegue (Estrategia Gratuita)
- Analytics (Spring) → Render (Web Service Docker) con `MONGO_URI` y `MONGO_DB`.
- Gateway GraphQL → Railway (Docker) con `ANALYTICS_BASE` apuntando a la URL Render.
- Frontend → Opcional: Netlify/Vercel (build estática) o servir Nginx desde Docker.

## Documentación Extendida (Deepwiki)
La wiki técnica completa (arquitectura, esquema GraphQL, flujo E2E, colecciones Mongo, tópicos ROS, guía de desarrollo) está en:

➡️ https://deepwiki.com/Julian362/project-poli

Secciones destacadas:
- Overview & Architecture
- GraphQL API Gateway (Schema, Resolvers)
- Backend Services (Chatbot, Analytics, ROS Control)
- IoT & Device Control (micro-ROS, ESP32 Firmware)
- Data Layer (MongoDB Schema, Persistence Flow)
- End-to-End Command Flow
- Environment Configuration & Deployment Guide

Consulta esa wiki para diagramas, referencias de código y guías detalladas de pruebas.

## Próximos Mejoras (Roadmap)
- Animaciones de transición en lista de mensajes.
- Animación de badges y subida de hover en estadísticas.
- Scrollbars temáticos y elevación de tarjetas.
- Endpoints `/health` dedicados para cada servicio (mejor monitoreo).

## Licencia
(Define aquí la licencia si aplica; actualmente no especificado.)

---
> Fuente resumida a partir de la documentación Deepwiki indexada el 27 Nov 2025. Para detalles completos, usa el enlace principal.
