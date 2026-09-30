#!/bin/bash
# SuperUnclench QA bridge — run on the Mac:  ./tools/adb-bridge/bridge.sh
# Lets the QA agent drive the emulator via files in tools/adb-bridge/{inbox,outbox}.
# Only these request types are executed (no arbitrary shell):
#   adb <args...>            -> runs `adb <args>` (adb subcommands: shell, exec-out, install, uninstall, devices, logcat -d, emu, root, reboot, wait-for-device, pull)
#   screencap <name>         -> saves outbox/<name>.png
#   screenrecord <name> <s>  -> records <s> seconds (max 30) to outbox/<name>.mp4
#   gradle <tasks...>        -> ./gradlew <tasks> (task names only, e.g. :app:installDebug)
# Each request is a file inbox/<id>.req; the result goes to outbox/<id>.out (exit code on the last line).
set -u
cd "$(dirname "$0")/../.." || exit 1
ROOT="$PWD"; B="$ROOT/tools/adb-bridge"; mkdir -p "$B/inbox" "$B/outbox"
SDK="${ANDROID_HOME:-$HOME/Library/Android/sdk}"; ADB="$SDK/platform-tools/adb"
[ -x "$ADB" ] || { echo "adb not found at $ADB (set ANDROID_HOME)"; exit 1; }
if [ -z "${JAVA_HOME:-}" ] && [ -d "/Applications/Android Studio.app/Contents/jbr/Contents/Home" ]; then
  export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"; fi
command -v timeout >/dev/null || timeout() { shift; "$@"; }  # macOS has no timeout by default
ALLOWED_ADB="shell exec-out install uninstall devices logcat emu root reboot wait-for-device pull get-state"
echo "QA bridge running in $ROOT (adb: $ADB). Ctrl-C to stop."
while true; do
  for req in "$B"/inbox/*.req; do
    [ -e "$req" ] || continue
    id="$(basename "$req" .req)"; out="$B/outbox/$id.out"
    read -r -a a < "$req"; mv "$req" "$B/outbox/$id.req.done"
    kind="${a[0]:-}"; rc=0
    echo "[$(date +%T)] $id: ${a[*]}"
    case "$kind" in
      adb)
        sub="${a[1]:-}"
        if [[ " $ALLOWED_ADB " == *" $sub "* ]] && ! [[ "$sub" == logcat && " ${a[*]} " != *" -d "* ]]; then
          timeout 300 "$ADB" "${a[@]:1}" > "$out" 2>&1; rc=$?
        else echo "rejected: adb $sub not allowed" > "$out"; rc=2; fi ;;
      screencap)
        name="${a[1]//[^A-Za-z0-9._-]/_}"
        "$ADB" exec-out screencap -p > "$B/outbox/$name.png" 2> "$out"; rc=$? ;;
      screenrecord)
        name="${a[1]//[^A-Za-z0-9._-]/_}"; s="${a[2]:-10}"; [[ "$s" =~ ^[0-9]+$ ]] || s=10; [ "$s" -gt 30 ] && s=30
        "$ADB" shell screenrecord --time-limit "$s" /sdcard/qa.mp4 > "$out" 2>&1 && "$ADB" pull /sdcard/qa.mp4 "$B/outbox/$name.mp4" >> "$out" 2>&1; rc=$? ;;
      gradle)
        ok=1; for t in "${a[@]:1}"; do [[ "$t" =~ ^[:A-Za-z0-9_-]+$ ]] || ok=0; done
        if [ $ok = 1 ]; then timeout 900 ./gradlew "${a[@]:1}" > "$out" 2>&1; rc=$?; else echo "rejected: bad task name" > "$out"; rc=2; fi ;;
      *) echo "rejected: unknown request '$kind'" > "$out"; rc=2 ;;
    esac
    echo "EXIT $rc" >> "$out"
  done
  sleep 0.5
done
