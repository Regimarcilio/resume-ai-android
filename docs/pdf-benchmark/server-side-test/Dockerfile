FROM python:3.11-slim

WORKDIR /app

RUN apt-get update && apt-get install -y \
    fontconfig \
    libpango-1.0-0 \
    libpangoft2-1.0-0 \
    libharfbuzz0b \
    libgdk-pixbuf-2.0-0 \
    libffi-dev \
    shared-mime-info \
    && rm -rf /var/lib/apt/lists/*

RUN pip install --no-cache-dir \
    fastapi \
    uvicorn \
    pydantic \
    pydantic-settings \
    jinja2 \
    weasyprint

COPY app/ /app/app/
COPY templates/ /app/templates/
COPY fonts/ /app/fonts/

EXPOSE 8082

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8082"]