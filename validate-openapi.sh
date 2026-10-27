#!/bin/bash
#
# validate-openapi.sh
#
# Validates OpenAPI specification files committed to the repository.
#
# This is a lightweight structural check intended to run early in the pipeline
# (right after compilation). It verifies that every OpenAPI spec file is valid
# YAML/JSON and conforms to the OpenAPI 3.x format.
#
# Deeper runtime validation (matching DTO validation annotations against the
# live /v3/api-docs of running services) is performed by the separate
# openapi-constraint-check job using ci/validation/OpenApiConstraintVerifier.
#
# Usage:
#   ./validate-openapi.sh
#
# Exit codes:
#   0 - All OpenAPI specs are valid (or no spec files were found)
#   1 - One or more OpenAPI specs are invalid

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$REPO_ROOT"

# Locate OpenAPI spec files, excluding build outputs and version control.
read -r -d '' SPEC_FILES <<EOF || true
$(find "$REPO_ROOT" -type f \
    \( -name 'openapi.yaml' -o -name 'openapi.yml' -o -name 'openapi.json' \
       -o -name '*openapi*.yaml' -o -name '*openapi*.yml' -o -name '*openapi*.json' \) \
    -not -path '*/target/*' \
    -not -path '*/.git/*' \
    -not -path '*/node_modules/*' \
    -not -path '*/build/*' \
  | sort)
EOF

if [ -z "$SPEC_FILES" ]; then
    echo "WARN: No OpenAPI specification files found in the repository."
    echo "      Add openapi.yaml / openapi.json files (or *openapi*.yaml) to validate them here."
    exit 0
fi

SPEC_COUNT=$(printf '%s\n' "$SPEC_FILES" | grep -c .)
FAIL=0

for file in $SPEC_FILES; do
    echo "Validating OpenAPI spec: $file"
    if ! python3 - "$file" <<'PY'
import json
import sys

try:
    import yaml
except ImportError:  # pragma: no cover
    yaml = None

path = sys.argv[1]

with open(path, "r", encoding="utf-8") as handle:
    text = handle.read()

# Parse according to file extension.
if path.lower().endswith(".json"):
    try:
        spec = json.loads(text)
    except json.JSONDecodeError as exc:
        print(f"  ERROR: {path} is not valid JSON: {exc}")
        sys.exit(1)
else:
    if yaml is None:
        print(f"  ERROR: PyYAML is required to parse YAML spec {path}")
        sys.exit(1)
    try:
        spec = yaml.safe_load(text)
    except yaml.YAMLError as exc:
        print(f"  ERROR: {path} is not valid YAML: {exc}")
        sys.exit(1)

if not isinstance(spec, dict):
    print(f"  ERROR: {path} does not contain a top-level mapping/object")
    sys.exit(1)

openapi_version = spec.get("openapi", "")
if not str(openapi_version).startswith("3."):
    print(f"  ERROR: {path} is not an OpenAPI 3.x document (openapi={openapi_version!r})")
    sys.exit(1)

if "info" not in spec:
    print(f"  ERROR: {path} is missing the required 'info' section")
    sys.exit(1)

if "paths" not in spec:
    print(f"  ERROR: {path} is missing the required 'paths' section")
    sys.exit(1)

path_count = len(spec.get("paths", {})) if isinstance(spec.get("paths"), dict) else 0
print(f"  OK: OpenAPI {openapi_version}, {path_count} path(s)")
PY
    then
        echo "  FAILED: $file"
        FAIL=1
    fi
done

echo ""
if [ "$FAIL" -ne 0 ]; then
    echo "OpenAPI validation FAILED ($SPEC_COUNT spec file(s) checked)"
    exit 1
fi

echo "OpenAPI validation PASSED ($SPEC_COUNT spec file(s) checked)"
exit 0
