#!/usr/bin/env fish

# ─────────────────────────────────────────────
#  run_spring_instances.fish
#  Launches two Spring Boot instances side-by-side
#  with separated logs using tmux split panes
# ─────────────────────────────────────────────

# Check if tmux is installed
if not command -q tmux
    echo "❌ tmux is not installed. Install it with:"
    echo "   sudo apt install tmux   # Debian/Ubuntu"
    echo "   brew install tmux       # macOS"
    exit 1
end

set SESSION "spring-boot-instances"
set PROJECT_DIR (pwd)

# Kill existing session if it exists
tmux kill-session -t $SESSION 2>/dev/null

echo "🚀 Starting Spring Boot instances in tmux session: '$SESSION'"
echo ""

# Create a new tmux session (detached), first pane = Instance 1
tmux new-session -d -s $SESSION -x "$(tput cols)" -y "$(tput lines)"

# ── Pane 0: Instance 1 (port 8080) ──────────────────────────
tmux send-keys -t $SESSION "cd $PROJECT_DIR && echo '═══════════════════════════════════════' && echo '  🟢 Instance 1 — http://localhost:8080  ' && echo '═══════════════════════════════════════' && mvn spring-boot:run" Enter

# Split the window vertically (side by side)
tmux split-window -h -t $SESSION

# ── Pane 1: Instance 2 (port 8081) ──────────────────────────
tmux send-keys -t $SESSION "cd $PROJECT_DIR && echo '═══════════════════════════════════════' && echo '  🔵 Instance 2 — http://localhost:8081  ' && echo '═══════════════════════════════════════' && sleep 15 && mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081" Enter

# Make both panes equal width
tmux select-layout -t $SESSION even-horizontal

# Attach to the session
echo "✅ Attaching to tmux session..."
echo "💡 Tips:"
echo "   Ctrl+B then ←/→   — switch between panes"
echo "   Ctrl+B then Z      — zoom into focused pane"
echo "   Ctrl+B then D      — detach (apps keep running)"
echo "   tmux kill-session -t $SESSION   — stop everything"
echo ""

tmux attach-session -t $SESSION
