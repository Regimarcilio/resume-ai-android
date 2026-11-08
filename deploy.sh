#!/bin/bash
# deploy.sh - Automated deployment script for resume-ai-android
# Usage: ./deploy.sh [github_username] [repo_name] [branch]

set -euo pipefail

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default values
GITHUB_USER="${1:-}"
REPO_NAME="${2:-resume-ai-android}"
BRANCH="${3:-develop}"
PROJECT_DIR="/home/oficina/PROJETOS/resume-ai-android"

# Functions
log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

check_prerequisites() {
    log_info "Verificando pré-requisitos..."
    
    # Check git
    if ! command -v git &> /dev/null; then
        log_error "git não encontrado. Instale: sudo apt install git"
        exit 1
    fi
    
    # Check gh CLI (opcional, mas recomendado)
    if ! command -v gh &> /dev/null; then
        log_warn "GitHub CLI (gh) não encontrado. Instale para automação completa: https://cli.github.com/"
        GH_AVAILABLE=false
    else
        GH_AVAILABLE=true
    fi
    
    # Check project dir
    if [ ! -d "$PROJECT_DIR" ]; then
        log_error "Diretório do projeto não encontrado: $PROJECT_DIR"
        exit 1
    fi
    
    cd "$PROJECT_DIR"
    
    # Check git repo
    if [ ! -d ".git" ]; then
        log_error "Não é um repositório git. Execute: git init"
        exit 1
    fi
    
    log_success "Pré-requisitos OK"
}

get_github_user() {
    if [ -z "$GITHUB_USER" ]; then
        if [ "$GH_AVAILABLE" = true ]; then
            GITHUB_USER=$(gh api user --jq .login 2>/dev/null || true)
        fi
        
        if [ -z "$GITHUB_USER" ]; then
            read -p "Digite seu username do GitHub: " GITHUB_USER
        fi
    fi
    log_info "GitHub user: $GITHUB_USER"
}

check_remote() {
    cd "$PROJECT_DIR"
    
    if git remote get-url origin &>/dev/null; then
        CURRENT_REMOTE=$(git remote get-url origin)
        log_info "Remote origin já configurado: $CURRENT_REMOTE"
        
        # Check if it matches expected repo
        if [[ "$CURRENT_REMOTE" == *"$GITHUB_USER/$REPO_NAME"* ]]; then
            log_success "Remote origin correto"
            return 0
        else
            log_warn "Remote origin aponta para repo diferente"
            read -p "Atualizar para $GITHUB_USER/$REPO_NAME? (s/n): " -n 1 -r
            echo
            if [[ $REPLY =~ ^[Ss]$ ]]; then
                git remote set-url origin "https://github.com/$GITHUB_USER/$REPO_NAME.git"
                log_success "Remote origin atualizado"
            fi
        fi
    else
        log_info "Configurando remote origin..."
        git remote add origin "https://github.com/$GITHUB_USER/$REPO_NAME.git"
        log_success "Remote origin adicionado"
    fi
}

create_github_repo() {
    if [ "$GH_AVAILABLE" = true ]; then
        log_info "Verificando se repo existe no GitHub..."
        
        if gh repo view "$GITHUB_USER/$REPO_NAME" &>/dev/null; then
            log_success "Repo $GITHUB_USER/$REPO_NAME já existe"
        else
            log_info "Criando repo $GITHUB_USER/$REPO_NAME..."
            gh repo create "$GITHUB_USER/$REPO_NAME" \
                --description "AI-powered resume builder with ATS scoring, offline-first, LGPD compliant" \
                --public \
                --source=. \
                --remote=origin \
                --push
            log_success "Repo criado e código pushado!"
            return 0
        fi
    else
        log_warn "gh CLI não disponível. Crie o repo manualmente em https://github.com/new"
        log_warn "Nome: $REPO_NAME | Descrição: AI-powered resume builder with ATS scoring, offline-first, LGPD compliant"
        read -p "Pressione Enter após criar o repo no GitHub..."
    fi
}

push_code() {
    cd "$PROJECT_DIR"
    
    log_info "Fazendo push para $BRANCH..."
    
    # Ensure we're on the right branch
    CURRENT_BRANCH=$(git branch --show-current)
    if [ "$CURRENT_BRANCH" != "$BRANCH" ]; then
        log_warn "Branch atual: $CURRENT_BRANCH, esperado: $BRANCH"
        git checkout "$BRANCH" 2>/dev/null || git checkout -b "$BRANCH"
    fi
    
    # Push
    git push -u origin "$BRANCH"
    log_success "Push realizado com sucesso!"
}

enable_github_actions() {
    if [ "$GH_AVAILABLE" = true ]; then
        log_info "Habilitando GitHub Actions..."
        gh api -X PUT "repos/$GITHUB_USER/$REPO_NAME/actions/permissions" \
            -f enabled=true \
            -f allowed_actions="all" 2>/dev/null && log_success "GitHub Actions habilitado" || log_warn "Não foi possível habilitar Actions automaticamente"
    fi
}

show_next_steps() {
    echo ""
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}✅ DEPLOY CONCLUÍDO!${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo ""
    echo -e "${BLUE}📍 Repositório:${NC} https://github.com/$GITHUB_USER/$REPO_NAME"
    echo -e "${BLUE}📍 Branch:${NC} $BRANCH"
    echo -e "${BLUE}📍 Actions:${NC} https://github.com/$GITHUB_USER/$REPO_NAME/actions"
    echo ""
    echo -e "${YELLOW}Próximos passos:${NC}"
    echo "1. Acesse: https://github.com/$GITHUB_USER/$REPO_NAME/actions"
    echo "2. Aguarde o workflow 'CI Pipeline' completar"
    echo "3. Clique no job 'Build Debug APK' > Artifacts > debug-apk"
    echo "4. Baixe o app-debug.apk e instale no celular"
    echo ""
    echo -e "${YELLOW}Para build manual:${NC}"
    echo "  gh workflow run ci.yml -f build_type=debug"
    echo ""
    echo -e "${YELLOW}Para release assinado:${NC}"
    echo "  1. Gere keystore: keytool -genkeypair -alias resume-app ..."
    echo "  2. Adicione secrets no GitHub: SIGNING_KEY, STORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD"
    echo "  3. git tag -a v1.0.0 -m 'Release v1.0.0' && git push origin v1.0.0"
    echo ""
    echo -e "${YELLOW}Link direto do APK (após primeiro build):${NC}"
    echo "  https://nightly.link/$(echo $GITHUB_USER | tr '[:upper:]' '[:lower:]')/resume-ai-android/workflows/ci/develop/app-debug.apk"
    echo ""
}

main() {
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}🚀 DEPLOY RESUME AI ANDROID${NC}"
    echo -e "${BLUE}========================================${NC}"
    echo ""
    
    check_prerequisites
    get_github_user
    check_remote
    create_github_repo
    push_code
    enable_github_actions
    show_next_steps
}

# Run
main "$@"