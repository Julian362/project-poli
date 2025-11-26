# Minimal image with ROS 2 Humble client libs and Python
FROM osrf/ros:humble-desktop

# Install Python deps
RUN apt-get update && apt-get install -y python3-pip && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY requirements.txt /app/requirements.txt
RUN pip3 install --no-cache-dir -r requirements.txt

# Copy app
COPY app.py /app/app.py

# Expose FastAPI port
EXPOSE 8000

# Run API (inside ROS 2 environment)
CMD ["bash", "-lc", "uvicorn app:app --host 0.0.0.0 --port 8000"]
