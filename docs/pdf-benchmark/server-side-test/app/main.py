from fastapi import FastAPI, HTTPException
from fastapi.responses import FileResponse
from pydantic import BaseModel, Field
from typing import List, Optional
from datetime import datetime
import jinja2
import weasyprint
import tempfile
import os
import uuid
import time

app = FastAPI(
    title="Server-side PDF Service",
    description="Resume PDF generation using WeasyPrint",
    version="1.0.0"
)

# Templates
template_loader = jinja2.FileSystemLoader("/app/templates")
template_env = jinja2.Environment(loader=template_loader)

class ResumeData(BaseModel):
    fullName: str
    email: str
    phone: str
    location: str
    linkedIn: str = ""
    github: str = ""
    summary: str
    experience: List[dict]
    education: List[dict]
    skills: List[str]

class PDFRequest(BaseModel):
    resume: ResumeData
    template: str = "modern"
    options: Optional[dict] = None

class PDFResponse(BaseModel):
    success: bool
    fileUrl: Optional[str] = None
    durationMs: int
    fileSizeBytes: int
    errorMessage: Optional[str] = None

@app.get("/health")
async def health():
    return {
        "status": "healthy",
        "timestamp": datetime.utcnow().isoformat(),
        "service": "pdf-service"
    }

@app.post("/v1/pdf/generate", response_model=PDFResponse)
async def generate_pdf(request: PDFRequest):
    start_time = time.time()
    
    try:
        # Load template
        template = template_env.get_template(f"{request.template}.html")
        
        # Render HTML
        html_content = template.render(
            resume=request.resume,
            generatedAt=datetime.utcnow().strftime("%Y-%m-%d %H:%M UTC")
        )
        
        pdf_filename = f"resume_{uuid.uuid4().hex[:8]}.pdf"
        pdf_path = os.path.join("/tmp", pdf_filename)
        
        # Generate PDF with WeasyPrint
        html_doc = weasyprint.HTML(string=html_content, base_url="/app/templates")
        html_doc.write_pdf(pdf_path)
        
        # Check file
        file_size = os.path.getsize(pdf_path)
        duration_ms = int((time.time() - start_time) * 1000)
        
        return PDFResponse(
            success=True,
            fileUrl=f"/v1/pdf/download/{pdf_filename}",
            durationMs=duration_ms,
            fileSizeBytes=file_size
        )
        
    except Exception as e:
        import traceback
        traceback.print_exc()
        return PDFResponse(
            success=False,
            durationMs=int((time.time() - start_time) * 1000),
            fileSizeBytes=0,
            errorMessage=str(e)
        )

@app.get("/v1/pdf/download/{filename}")
async def download_pdf(filename: str):
    file_path = os.path.join("/tmp", filename)
    if not os.path.exists(file_path):
        raise HTTPException(status_code=404, detail="File not found")
    return FileResponse(file_path, media_type='application/pdf', filename=filename)

@app.delete("/v1/pdf/cleanup")
async def cleanup_temp_files():
    """Clean up PDFs older than 1 hour"""
    import glob
    now = time.time()
    cleaned = 0
    for pdf_file in glob.glob("/tmp/resume_*.pdf"):
        if now - os.path.getmtime(pdf_file) > 3600:
            os.unlink(pdf_file)
            cleaned += 1
    return {"cleaned": cleaned}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8082)