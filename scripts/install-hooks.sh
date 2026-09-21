#!/usr/bin/env bash
# ==============================================================================
# Installs KernelCraft local Git hooks into the local repository.
# ==============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(dirname "$SCRIPT_DIR")"
GIT_DIR="$REPO_ROOT/.git"

if [ ! -d "$GIT_DIR" ]; then
    echo "Configuring core.hooksPath to scripts/..."
    git config core.hooksPath scripts/ || true
    echo "Done. Hooks configured to run from scripts/."
    exit 0
fi

HOOK_DEST="$GIT_DIR/hooks/pre-commit"
chmod +x "$SCRIPT_DIR/pre-commit-security.sh"

echo "Installing pre-commit security hook to $HOOK_DEST..."
cp "$SCRIPT_DIR/pre-commit-security.sh" "$HOOK_DEST"
chmod +x "$HOOK_DEST"

echo "KernelCraft Git pre-commit security hook installed successfully."
