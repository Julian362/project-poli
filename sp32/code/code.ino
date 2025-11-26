#include <micro_ros_arduino.h>
#include <WiFi.h>
#include <rclc/rclc.h>
#include <rclc/executor.h>
#include <std_msgs/msg/char.h>

// Credentials WiFi 
char* ssid = "JG";
char* password ="1007346583";

// IP del agente micro-ROS en el host PC
IPAddress agent_ip(192,168,80,178);
size_t agent_port = 8888; // debe coincidir con el puerto expuesto del agente

// LEDs: habitación, cocina, sala
#define LED_HABITACION 23
#define LED_COCINA 18
#define LED_SALA 21
bool led_h_state = false; // habitación
bool led_c_state = false; // cocina
bool led_s_state = false; // sala
// Temporizadores (deshabilitados): antes usados para auto-apagado
// Mantener variables por si se reactivan en el futuro
unsigned long h_off_deadline = 0;
unsigned long c_off_deadline = 0;
unsigned long s_off_deadline = 0;

// Configuración: ¿LED activo en LOW (cátodo a GND) o activo en HIGH?
// Si al enviar 'H' el LED NO se enciende, cambia a false.
const bool LED_ACTIVE_LOW = false; // poner false si HIGH enciende

inline void setLed(int pin, bool on){
  if(LED_ACTIVE_LOW){
    digitalWrite(pin, on ? LOW : HIGH);
  } else {
    digitalWrite(pin, on ? HIGH : LOW);
  }
}

inline void print_status(){
  Serial.print("estado -> h:"); Serial.print(led_h_state ? "on" : "off");
  Serial.print(" c:"); Serial.print(led_c_state ? "on" : "off");
  Serial.print(" s:"); Serial.println(led_s_state ? "on" : "off");
}

// instanciación de objetos micro-ROS
rcl_node_t node;
rcl_subscription_t sub_led;
rcl_publisher_t pub_response;
rcl_publisher_t pub_heartbeat;
rcl_allocator_t allocator;
rclc_support_t support;
rclc_executor_t executor;

std_msgs__msg__Char msg; // instancia tipo de mensaje


// Callback asociada al suscriptor sub_led
void led_toggle_CBK(const void *msg_in) {
  const std_msgs__msg__Char *in_msg = (const std_msgs__msg__Char *)msg_in;

  // Comandos por LED: 'h' habitación, 'c' cocina, 's' sala
  char cmd = in_msg->data;
  Serial.print("[RX] cmd=");
  Serial.println(cmd);
  if (cmd == 'h') {
    // apagar habitación
    led_h_state = false;
    setLed(LED_HABITACION, false);
    Serial.println("h apagado");
  } else if (cmd == 'H') {
    // prender habitación (sin auto-apagado)
    led_h_state = true;
    setLed(LED_HABITACION, true);
    Serial.println("h prendido");
  } else if (cmd == 'c') {
    // apagar cocina
    led_c_state = false;
    setLed(LED_COCINA, false);
    Serial.println("c apagado");
  } else if (cmd == 'C') {
    // prender cocina (sin auto-apagado)
    led_c_state = true;
    setLed(LED_COCINA, true);
    Serial.println("c prendido");
  } else if (cmd == 's') {
    // apagar sala
    led_s_state = false;
    setLed(LED_SALA, false);
    Serial.println("s apagado");
  } else if (cmd == 'S') {
    // prender sala (sin auto-apagado)
    led_s_state = true;
    setLed(LED_SALA, true);
    Serial.println("s prendido");
  } else {
    if(cmd == 'X') { // consulta de estado sin modificar LEDs
      print_status();
    } else {
      // comando no reconocido: no hacer nada
      Serial.println("[WARN] Comando no reconocido");
      return;
    }
  }

  // Publica respuesta eco del comando recibido
  std_msgs__msg__Char response_msg;
  response_msg.data = cmd;
  rcl_ret_t ret = rcl_publish(&pub_response, &response_msg, NULL);
  if (ret == RCL_RET_OK) {
    Serial.println("[TX] /arduino_response enviado");
  } else {
    Serial.print("[TX][ERR] rcl_publish: ");
    Serial.println((int)ret);
  }
  if(cmd == 'X') {
    // tras publicar eco de 'X', también se lista estado
    print_status();
  }
}

// Función de inicialización LED
void setup_led(){
  pinMode(LED_HABITACION, OUTPUT);
  pinMode(LED_COCINA, OUTPUT);
  pinMode(LED_SALA, OUTPUT);
  // Apagar al inicio
  setLed(LED_HABITACION, false);
  setLed(LED_COCINA, false);
  setLed(LED_SALA, false);

  // Test de arranque muy corto (enciende y apaga cada LED)
  // Duración total < 1s; solo para ver funcionamiento básico
  setLed(LED_HABITACION, true);  Serial.println("h prendido");  delay(150);
  setLed(LED_HABITACION, false); Serial.println("h apagado");   delay(100);

  setLed(LED_COCINA, true);      Serial.println("c prendido");  delay(150);
  setLed(LED_COCINA, false);     Serial.println("c apagado");   delay(100);

  setLed(LED_SALA, true);        Serial.println("s prendido");  delay(150);
  setLed(LED_SALA, false);       Serial.println("s apagado");   delay(100);
}

// Función para la conectividad WiFi
void setup_wifi(){
  Serial.println("[WIFI] Conectando...");
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println("");
  Serial.print("[WIFI] Conectado. IP: ");
  Serial.println(WiFi.localIP());

  // micro-ROS sobre WiFi con protocolo UDP (usa agent_ip/agent_port definidos)
  char agent_ip_str[16];
  snprintf(agent_ip_str, sizeof(agent_ip_str), "%u.%u.%u.%u", agent_ip[0], agent_ip[1], agent_ip[2], agent_ip[3]);
  set_microros_wifi_transports(ssid, password, agent_ip_str, agent_port);
  Serial.print("[MICRO-ROS] Agente: ");
  Serial.print(agent_ip);
  Serial.print(":");
  Serial.println(agent_port);

  // Espera a que el agente responda antes de crear entidades
  Serial.println("[MICRO-ROS] Esperando conexión con el agente...");
  while (RMW_RET_OK != rmw_uros_ping_agent(500, 1)) {
    Serial.print(".");
    delay(250);
  }
  Serial.println("");
  Serial.println("[MICRO-ROS] Agente conectado");
}

void setup() {
  // Inicializa Serial antes de cualquier log
  Serial.begin(115200);
  delay(100);
  Serial.println("[BOOT] Inicio");
  setup_led();
  setup_wifi();

  // Inicialización de micro-ROS
  allocator = rcl_get_default_allocator();
  rclc_support_init(&support, 0, NULL, &allocator);
  rclc_node_init_default(&node, "led_toggle_node", "", &support);
  Serial.println("[MICRO-ROS] Node y soporte inicializados");
  rclc_subscription_init_default(&sub_led, &node,
    ROSIDL_GET_MSG_TYPE_SUPPORT(std_msgs, msg, Char), "/led_toggle");
  rclc_executor_init(&executor, &support.context, 1, &allocator);
  rclc_executor_add_subscription(&executor, &sub_led, &msg, &led_toggle_CBK,
    ON_NEW_DATA);

  rclc_publisher_init_default(
    &pub_response,
    &node,
    ROSIDL_GET_MSG_TYPE_SUPPORT(std_msgs, msg, Char),
    "/arduino_response"
  );

  // Publisher de heartbeat
  rclc_publisher_init_default(
    &pub_heartbeat,
    &node,
    ROSIDL_GET_MSG_TYPE_SUPPORT(std_msgs, msg, Char),
    "/arduino_heartbeat"
  );
  Serial.println("[MICRO-ROS] Suscriptor y publishers listos");

}

//Loop principal
void loop() {
  //espera publicación en led_toggle
  rclc_executor_spin_some(&executor, RCL_MS_TO_NS(100)); 
  delay(10); // pequeño retardo requerido

  // Heartbeat periódico (1 Hz)
  static unsigned long last_hb = 0;
  unsigned long now = millis();
  if (now - last_hb >= 1000) {
    last_hb = now;
    std_msgs__msg__Char hb;
    hb.data = 'B';
    rcl_publish(&pub_heartbeat, &hb, NULL);
    Serial.println("[HB] B");
  }

  // Auto-apagado deshabilitado: los LEDs cambian solo por comandos explícitos
}