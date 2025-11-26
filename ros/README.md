# micro-ROS Agent (Docker Compose)

Este directorio contiene un `docker-compose.yml` para levantar el micro-ROS Agent utilizando la imagen oficial `microros/micro-ros-agent:humble` con transporte UDP en el puerto 8888.

## Servicio
- Nombre del servicio / contenedor: `micro-ros-agent`
- Imagen: `microros/micro-ros-agent:humble`
- Comando: `udp4 --port 8888 -v6`
- Puerto expuesto: `8888/udp` (host -> contenedor)

## Levantar el agente
En la raíz del proyecto (o dentro de `ros/`):

```powershell
cd ros
# Iniciar el agente (modo foreground)
docker compose up
```

Para iniciar en segundo plano:
```powershell
docker compose up -d
```

## Ver logs
```powershell
docker compose logs -f micro-ros-agent
```

## Parar y limpiar (similar a --rm)
```powershell
# Parar
docker compose down
# Parar y eliminar volúmenes (no usados aquí) y huellas
docker compose down -v
```

## Ejecución equivalente sin compose
```powershell
docker run -it --rm -p 8888:8888/udp --name micro-ros-agent microros/micro-ros-agent:humble udp4 --port 8888 -v6
```

## Notas
- `stdin_open: true` y `tty: true` replican `-it`.
- Compose no soporta `--rm` automático: usa `docker compose down` para limpiar.
- Asegúrate de que ningún firewall bloquee UDP 8888.
- Para cambiar el puerto, ajusta `ports:` y el argumento `--port` en el comando.

## Test rápido de puerto (Windows PowerShell)
```powershell
# Ver procesos escuchando puerto 8888
Get-NetUDPEndpoint | Where-Object { $_.LocalPort -eq 8888 }
```

## Extensiones futuras
- Añadir un agente adicional para serial (`serial --dev /dev/ttyUSB0 -b 115200`).
- Añadir healthcheck que verifique que el proceso está respondiendo.
