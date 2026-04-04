.PHONY: run start stop clean

run: start

start:
	@bash server.sh

stop: clean

clean:
	@echo "Stopping processes from PID files (if any)..."
	@if [ -d .pids ]; then \
	  for f in .pids/*.pid; do \
	    [ -f "$$f" ] || continue; \
	    pid=$$(cat "$$f"); \
	    if kill -0 $$pid 2>/dev/null; then \
	      echo "  TERM $$pid ($$f)"; kill $$pid || true; \
	    fi; \
	  done; \
	  sleep 1; \
	  for f in .pids/*.pid; do \
	    [ -f "$$f" ] || continue; \
	    pid=$$(cat "$$f"); \
	    if kill -0 $$pid 2>/dev/null; then \
	      echo "  KILL $$pid ($$f)"; kill -9 $$pid || true; \
	    fi; \
	    rm -f "$$f"; \
	  done; \
	  rmdir .pids 2>/dev/null || true; \
	fi
	@echo "Best-effort cleanup with pkill..."
	-@pkill -x rmiregistry || true
	-@pkill -f 'frontend\.FrontEndServer' || true
	-@pkill -f 'server\.ServerMain' || true
	@echo "Maven clean..."
	@mvn -q clean
