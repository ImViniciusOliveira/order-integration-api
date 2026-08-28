#!/usr/bin/env bash
# Hook de seguranca arquitetural (Pre-Tool Use)

# Le o payload bruto da ferramenta/patch/arquivo.
INPUT="$(cat)"

if [[ -n "${TARGET_FILE_PATH:-}" ]]; then
    FILE_PATH="$TARGET_FILE_PATH"
    CONTENT="$INPUT"
else
    contains() {
        echo "$INPUT" | grep -q "$1"
    }

    extract_json_value() {
        local key="$1"
        local value
        value="${INPUT#*\"$key\":\"}"
        if [[ "$value" == "$INPUT" ]]; then
            echo ""
            return
        fi
        value="${value%%\"*}"
        echo "$value"
    }

    extract_patch_file() {
        echo "$INPUT" | awk '
            /^(\*\*\* Add File: |\*\*\* Update File: |\*\*\* Move to: )/ {
                sub(/^(\*\*\* Add File: |\*\*\* Update File: |\*\*\* Move to: )/, "", $0);
                print;
                exit
            }'
    }

    extract_patch_content() {
        echo "$INPUT" | awk '
            /^(\*\*\* Update File: |\*\*\* Add File: )/ { in_file=1; next }
            /^(\*\*\* Delete File: |\*\*\* Move to: )/ { in_file=0; next }
            /^(\*\*\* End of File)/ { in_file=0; next }
            in_file && /^[+-]/ { sub(/^[+-]/, "", $0); print }
        '
    }

    # Se vier do editor com tool_name, so analisa ferramentas de edicao/criacao.
    TOOL_NAME="$(extract_json_value tool_name)"
    if [[ -n "$TOOL_NAME" ]]; then
        case "$TOOL_NAME" in
          *write*|*edit*|*file*|appmod-*) ;;
          *) exit 0 ;;
        esac
    fi

    CONTENT="$(extract_json_value content)"
    FILE_PATH="$(extract_json_value path)"

    # Suporte a patch/diff bruto quando nao vier JSON com path/content.
    if [[ -z "$FILE_PATH" ]]; then
        FILE_PATH="$(extract_json_value filePath)"
    fi
    if [[ -z "$FILE_PATH" ]]; then
        FILE_PATH="$(extract_patch_file)"
    fi
    if [[ -z "$CONTENT" ]]; then
        CONTENT="$(extract_patch_content)"
    fi
fi

# Apenas valida codigo Java
if [[ "$FILE_PATH" != *.java ]]; then
    exit 0
fi

ERRORS=""

if echo "$CONTENT" | grep -q "BeanUtils.copyProperties"; then
    ERRORS+="[Mapeamento] Proibido BeanUtils.copyProperties. Use MapStruct. "
fi

if echo "$CONTENT" | grep -q "System.out.println" || echo "$CONTENT" | grep -q "System.err.println"; then
    ERRORS+="[Logs] Proibido System.out.println. Use @Slf4j. "
fi

if [[ "$FILE_PATH" == *src/main/java/* ]] && echo "$CONTENT" | grep -q "\.trim()"; then
    ERRORS+="[Normalizacao] Proibido trim manual. Use o deserializer global do Jackson. "
fi

if echo "$CONTENT" | grep -q "@Data" || echo "$CONTENT" | grep -q "@Value"; then
    ERRORS+="[Lombok] Proibido @Data/@Value. Use @Getter, @Setter, @Builder, etc. "
fi

if echo "$CONTENT" | grep -qE "@Autowired"; then
    ERRORS+="[Injecao] Proibido @Autowired. Use construtor com private final e @RequiredArgsConstructor. "
fi

if [[ "$FILE_PATH" == *Controller.java ]] && echo "$CONTENT" | grep -qE "Repository"; then
    ERRORS+="[Fluxo] Proibido Controller acessar Repository. Chame o Service. "
fi

if [[ "$FILE_PATH" == *Repository.java || "$FILE_PATH" == *repository/* ]] && echo "$CONTENT" | grep -q "@Query"; then
    ERRORS+="[Consultas] Proibido @Query em Repositories. Use Specifications. "
fi

if [[ "$FILE_PATH" == *domain/*.java ]] && echo "$CONTENT" | grep -q "\.api\."; then
    ERRORS+="[Isolamento] Proibido Domain importar pacote API. "
fi

# Validacao de JavaDoc na declaracao da classe/interface/record
if [[ "$FILE_PATH" == *src/main/java/* ]] && echo "$CONTENT" | grep -qE "(public|abstract|final)?[[:space:]]*(class|interface|record)[[:space:]]+[A-Z]"; then
    if ! echo "$CONTENT" | grep -q "/\*\*"; then
        ERRORS+="[JavaDoc] Obrigatorio adicionar JavaDoc descritivo no topo de classes, interfaces e records. "
    fi
fi

if [[ -n "$ERRORS" ]]; then
    # O exit status e o stderr avisam o Agente de que a acao foi bloqueada
    echo "VIOLACAO ARQUITETURAL: $ERRORS" >&2
    exit 1
fi

exit 0
