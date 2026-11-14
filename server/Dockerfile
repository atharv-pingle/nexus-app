FROM python:3.11-slim

WORKDIR /app

# Prevent Python from writing .pyc files and enable unbuffered output
ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1

# Install dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy application code
COPY . .

# Expose port 3066
EXPOSE 3066

# Run Uvicorn listening on all interfaces at port 3066
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "3066"]
