#!/bin/bash
# Pre-commit hook to regenerate VALIDATION.md
# Install with: ln -sf ../../ci/validation/pre-commit-hook.sh .git/hooks/pre-commit

set -e

echo "Regenerating VALIDATION.md..."

# Check if we're in a git repo
if ! git rev-parse --git-dir > /dev/null 2>&1; then
    echo "Not in a git repository, skipping VALIDATION.md generation"
    exit 0
fi

# Check if Maven is available
if ! command -v mvn &> /dev/null; then
    echo "Maven not found, skipping VALIDATION.md generation"
    exit 0
fi

# Get the root of the git repo
REPO_ROOT=$(git rev-parse --show-toplevel)

# Change to the validation module and generate
cd "$REPO_ROOT/ci/validation"

# Generate VALIDATION.md at repo root
mvn exec:java -Dexec.mainClass=com.example.validation.ValidationMarkdownGenerator -Dexec.args="$REPO_ROOT/VALIDATION.md" -q

# Check if the file changed
cd "$REPO_ROOT"
if git diff --quiet VALIDATION.md; then
    echo "VALIDATION.md is up to date"
else
    echo "VALIDATION.md has been updated"
    echo "Please review the changes and commit them"
    # Optionally auto-stage: git add VALIDATION.md
fi

echo "Pre-commit hook completed"