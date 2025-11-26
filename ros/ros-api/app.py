import os
from fastapi import FastAPI
from pydantic import BaseModel
import rclpy
from rclpy.node import Node
from std_msgs.msg import Char

# Map friendly names to ASCII codes
CMD_MAP = {
    "H": 72, "h": 104,
    "C": 67, "c": 99,
    "S": 83, "s": 115,
    "X": 88,
}

class Command(BaseModel):
    data: str  # single character like 'H', 'h', 'C', 'c', 'S', 's', 'X'

class RosPublisher(Node):
    def __init__(self):
        super().__init__('led_rest_api_node')
        self.pub = self.create_publisher(Char, '/led_toggle', 10)

    def publish_char(self, ch: str):
        if ch not in CMD_MAP:
            raise ValueError(f"Invalid command '{ch}'")
        msg = Char()
        msg.data = CMD_MAP[ch]
        self.pub.publish(msg)
        self.get_logger().info(f"Published /led_toggle: {ch} -> {msg.data}")

# FastAPI app
app = FastAPI()

# Initialize ROS 2 once
rclpy.init()
ros_publisher = RosPublisher()

@app.on_event("shutdown")
def shutdown_event():
    ros_publisher.destroy_node()
    rclpy.shutdown()

@app.get("/health")
def health():
    return {"status": "ok"}

@app.post("/cmd")
def send_cmd(cmd: Command):
    ch = cmd.data
    if not isinstance(ch, str) or len(ch) != 1:
        return {"ok": False, "error": "data must be a single character"}
    try:
        ros_publisher.publish_char(ch)
        return {"ok": True, "sent": ch}
    except Exception as e:
        return {"ok": False, "error": str(e)}

@app.post("/hab/on")
def hab_on():
    ros_publisher.publish_char('H')
    return {"ok": True}

@app.post("/hab/off")
def hab_off():
    ros_publisher.publish_char('h')
    return {"ok": True}

@app.post("/coc/on")
def coc_on():
    ros_publisher.publish_char('C')
    return {"ok": True}

@app.post("/coc/off")
def coc_off():
    ros_publisher.publish_char('c')
    return {"ok": True}

@app.post("/sal/on")
def sal_on():
    ros_publisher.publish_char('S')
    return {"ok": True}

@app.post("/sal/off")
def sal_off():
    ros_publisher.publish_char('s')
    return {"ok": True}

@app.get("/status")
def status():
    ros_publisher.publish_char('X')
    return {"ok": True}
