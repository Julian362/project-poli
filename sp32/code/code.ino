#include <micro_ros_arduino.h>
#include <WiFi.h>
//#include <rcl/rcl.h>
#include <rclc/rclc.h>
#include <rclc/executor.h>
#include <std_msgs/msg/char.h>

// Credentials WiFi 
char* ssid = "DiegoR";
char* password ="15384541";
// char* ssid = "HOME-AB72";
// char* password = "A5AD01162E2DA0F3";

// IP del agente micro-ROS en elhost PC
IPAddress agent_ip(192, 168, 150, 38);
//IPAddress agent_ip(192, 168, 1, 74);
size_t agent_port = 8888;

// LED en GPIO 13
#define LED_PIN 13
bool led_state = false;

// instanciación de objetos micro-ROS
rcl_node_t node;
rcl_subscription_t sub_led;
rcl_publisher_t pub_response;
rcl_allocator_t allocator;
rclc_support_t support;
rclc_executor_t executor;

std_msgs_msg_Char msg; // instancia tipo de mensaje


// Callback asociada al suscriptor sub_led
void led_toggle_CBK(const void *msg_in) {
  const std_msgs_msgChar *msg=(const std_msgsmsg_Char *)msg_in;
  
  if (msg->data == 'r') {
    led_state = !led_state;
    digitalWrite(LED_PIN, led_state ? LOW : HIGH); // enciende en bajo
    
//RETORNA LOS DATOS
std_msgs_msg_Char response_msg;
        response_msg.data = 'r';  // Respuesta (puede ser cualquier dato)
        rcl_publish(&pub_response, &response_msg, NULL);

  }
}

// Función de inicialización LED
void setup_led(){
  pinMode(LED_PIN, OUTPUT);
  digitalWrite(LED_PIN, HIGH);  // Apagar al inicio
}

// Función para la conectividad WiFi
void setup_wifi(){
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
  }

  digitalWrite(LED_PIN, LOW);  // enciende led por ...
  delay(1000);                 // un segundo para indicar ...
  digitalWrite(LED_PIN, HIGH); // conectividad Wifi ...

  // micro-ROS sobre WiFi con protocolo UDP
  set_microros_wifi_transports(ssid, password, "192.168.1.100", agent_port);
}

void setup() {
  setup_led();
  setup_wifi();

  // Inicialización de micro-ROS
  allocator = rcl_get_default_allocator();
  rclc_support_init(&support, 0, NULL, &allocator);
  rclc_node_init_default(&node, "led_toggle_node", "", &support);
  rclc_subscription_init_default(&sub_led, &node,
    ROSIDL_GET_MSG_TYPE_SUPPORT(std_msgs, msg, Char), "led_toggle");
  rclc_executor_init(&executor, &support.context, 1, &allocator);
  rclc_executor_add_subscription(&executor, &sub_led, &msg, &led_toggle_CBK,
   ON_NEW_DATA);
   rclc_publisher_init_default(
    &pub_response,
    &node,
    ROSIDL_GET_MSG_TYPE_SUPPORT(std_msgs, msg, Char),
    "/arduino_response"
);

}

//Loop principal
void loop() {
  //espera publicación en led_toggle
  rclc_executor_spin_some(&executor, RCL_MS_TO_NS(100)); 
  delay(10); // pequeño retardo requerido
}