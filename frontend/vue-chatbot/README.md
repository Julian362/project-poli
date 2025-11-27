# ChatBot IoT (Vue + GraphQL)

Frontend minimal en Vue 3 + Vite que consume el gateway GraphQL (`/` en `http://localhost:8090/`).

## Requisitos
- Node.js 18+
- Gateway GraphQL, Chatbot y Analytics corriendo (variables en `.env`)

## Variables
- `VITE_GRAPHQL_BASE` (default: `http://localhost:8090/`)

## Usar
```powershell
cd frontend/vue-chatbot
npm install
npm run dev
```

### Ejecución automática (Windows CMD)
Puedes usar los scripts incluidos:
```
cd C:\Users\julian\Downloads\project-poli\frontend\vue-chatbot
run_frontend.bat
```
Para build de producción:
```
build_frontend.bat
```

Abre `http://localhost:5173`.

## Funcionalidad
- Indicadores de luces (sala, cocina, habitación) desde `lightsState`.
- Chips rápidos: "prende sala", "prende cocina", "prende habitacion", "apaga todo".
- Chat: envía texto con `ask(sessionId,message)`, muestra historial y respuesta.
- Sesión persistida en `localStorage` (`web-xxxx`).
 - Scripts `.bat` para iniciar y construir automáticamente.
