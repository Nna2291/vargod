#!/usr/bin/env bash
# =============================================================================
#  Запуск юнит-тестов ТиОПО ЛР1 из командной строки (локальная копия репозитория).
#
#  По умолчанию: тесты business-logic + data-access, затем
#    - покрытие JaCoCo (строки + ветвления) в консоль и в HTML,
#    - отчёт Allure.
#
#  Примеры:
#    ./scripts/run-tests.sh                       # всё + отчёты
#    ./scripts/run-tests.sh --random              # случайный порядок (seed печатается)
#    ./scripts/run-tests.sh --seed 1731400000000  # повторить конкретный случайный порядок
#    ./scripts/run-tests.sh --offline             # без сети: только тесты без БД (mock / in-memory)
#    ./scripts/run-tests.sh --london              # только лондонский стиль
#    ./scripts/run-tests.sh --tags technique-boundary-values
#    ./scripts/run-tests.sh --forks 2 --fork-every 1 --module business-logic --no-report
#    ./scripts/run-tests.sh --threads 4 --module business-logic --no-report
#    ./scripts/run-tests.sh --serve               # после прогона открыть Allure на http://localhost:8090
#
#  По умолчанию включён экономный режим для слабых машин (без демона Gradle,
#  Kotlin-компилятор внутри процесса Gradle, 1 воркер). --fast его выключает.
#  Нужен JDK 17+: скрипт ищет его сам; --install-jdk скачает Temurin 17 без sudo.
# =============================================================================
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

usage() { awk 'NR > 1 && /^set -euo/ { exit } NR > 1' "$0" | sed 's/^# \{0,1\}//'; exit "${1:-0}"; }

GRADLE_ARGS=()
TASKS=()
MODULE=""
REPORTS=1
SERVE=0
LOW_MEM=1
OFFLINE=0
INSTALL_JDK=0

while [[ $# -gt 0 ]]; do
  case "$1" in
    --random)        GRADLE_ARGS+=("-PrandomOrder") ;;
    --seed)          GRADLE_ARGS+=("-PrandomSeed=$2"); shift ;;
    --offline)       OFFLINE=1; GRADLE_ARGS+=("-Poffline" "--offline") ;;
    --threads)       GRADLE_ARGS+=("-Pthreads=$2"); shift ;;
    --forks)         GRADLE_ARGS+=("-PmaxForks=$2"); shift ;;
    --fork-every)    GRADLE_ARGS+=("-PforkEvery=$2"); shift ;;
    --tags)          GRADLE_ARGS+=("-PincludeTags=$2"); shift ;;
    --exclude-tags)  GRADLE_ARGS+=("-PexcludeTags=$2"); shift ;;
    --london)        GRADLE_ARGS+=("-PincludeTags=style-london") ;;
    --classical)     GRADLE_ARGS+=("-PincludeTags=style-classical") ;;
    --module)        MODULE="$2"; shift ;;
    --no-report)     REPORTS=0 ;;
    --serve)         SERVE=1 ;;
    --fast)          LOW_MEM=0 ;;
    --install-jdk)   INSTALL_JDK=1 ;;
    -h|--help)       usage 0 ;;
    *) echo "Неизвестный параметр: $1" >&2; usage 1 ;;
  esac
  shift
done

# --- JDK 17+ -----------------------------------------------------------------------------
# Gradle и Spring Boot 3 требуют Java >= 17. Системная java может оказаться старее
# (например, 11), поэтому проверяем версию и при необходимости ищем подходящий JDK.

# Мажорная версия java по пути к её домашнему каталогу ("" если не java).
java_major() {
  [[ -x "$1/bin/java" ]] || return 0
  "$1/bin/java" -version 2>&1 | awk -F'"' '/version/ { split($2, v, "."); print (v[1] == "1") ? v[2] : v[1]; exit }'
}

install_jdk() {
  local arch dest
  if [[ "$(uname -s)" == "Darwin" ]]; then
    echo "На macOS поставьте JDK 17 так:  brew install --cask temurin@17" >&2
    exit 1
  fi
  case "$(uname -m)" in
    x86_64|amd64)  arch="x64" ;;
    aarch64|arm64) arch="aarch64" ;;
    *) echo "Неизвестная архитектура $(uname -m): установите JDK 17 вручную." >&2; exit 1 ;;
  esac
  dest="$HOME/.local/share/jdks"
  mkdir -p "$dest"
  echo ">>> Скачиваю Eclipse Temurin JDK 17 ($arch) в $dest ..."
  curl -fSL "https://api.adoptium.net/v3/binary/latest/17/ga/linux/$arch/jdk/hotspot/normal/eclipse" \
    -o "$dest/jdk17.tar.gz"
  tar -xzf "$dest/jdk17.tar.gz" -C "$dest"
  rm -f "$dest/jdk17.tar.gz"
}

pick_jdk() {
  local candidate
  local -a candidates=()
  [[ -n "${JAVA_HOME:-}" ]] && candidates+=("$JAVA_HOME")
  if command -v java >/dev/null 2>&1; then
    candidates+=("$(dirname "$(dirname "$(readlink -f "$(command -v java)" 2>/dev/null || command -v java)")")")
  fi
  if [[ -x /usr/libexec/java_home ]]; then # macOS
    candidate="$(/usr/libexec/java_home -v 17 2>/dev/null || true)"
    [[ -n "$candidate" ]] && candidates+=("$candidate")
  fi
  for candidate in "$HOME"/.local/share/jdks/* /usr/lib/jvm/* "$HOME"/.sdkman/candidates/java/* /opt/*jdk* \
                   /Library/Java/JavaVirtualMachines/*/Contents/Home \
                   /opt/homebrew/opt/openjdk@17 /opt/homebrew/opt/openjdk /usr/local/opt/openjdk@17 /usr/local/opt/openjdk; do
    [[ -d "$candidate" ]] && candidates+=("$candidate")
  done
  for candidate in "${candidates[@]}"; do
    local major
    major="$(java_major "$candidate")"
    if [[ -n "$major" && "$major" -ge 17 ]]; then
      echo "$candidate"
      return 0
    fi
  done
  return 1
}

if ! JDK="$(pick_jdk)"; then
  if [[ $INSTALL_JDK -eq 1 ]]; then
    install_jdk
    JDK="$(pick_jdk)" || { echo "JDK 17 не удалось установить." >&2; exit 1; }
  else
    echo "Не найден JDK 17 или новее (системная java: $(command -v java >/dev/null && java -version 2>&1 | head -n1 || echo 'нет'))." >&2
    echo "Варианты:" >&2
    echo "  ./scripts/run-tests.sh --install-jdk     # скачать Temurin 17 в ~/.local/share/jdks (без sudo)" >&2
    echo "  sudo apt install openjdk-17-jdk-headless # Ubuntu" >&2
    echo "  brew install --cask temurin@17           # macOS" >&2
    exit 1
  fi
fi
export JAVA_HOME="$JDK"
export PATH="$JAVA_HOME/bin:$PATH"
echo ">>> JDK: $JAVA_HOME (Java $(java_major "$JAVA_HOME"))"

if [[ $LOW_MEM -eq 1 ]]; then
  GRADLE_ARGS+=(
    "--no-daemon"
    "--max-workers=1"
    "-Pkotlin.compiler.execution.strategy=in-process"
    "-Dorg.gradle.jvmargs=-Xmx1536m -Dfile.encoding=UTF-8"
  )
fi

# --- Что запускать ---------------------------------------------------------------------
if [[ -n "$MODULE" ]]; then
  TASKS+=(":$MODULE:test")
  [[ $REPORTS -eq 1 ]] && TASKS+=(":$MODULE:allureReport")
  TASKS+=("coverageSummary")
elif [[ $REPORTS -eq 1 ]]; then
  TASKS+=("unitTests")                       # тесты + coverageSummary + Allure
else
  TASKS+=(":business-logic:test" ":data-access:test" "coverageSummary")
fi

if [[ $OFFLINE -eq 1 ]]; then
  echo ">>> Офлайн-режим: Gradle --offline, тесты с тегом 'db' исключены."
  echo "    (Зависимости, бинарник PostgreSQL и allure-commandline должны быть скачаны"
  echo "     заранее — хотя бы одним обычным запуском с сетью.)"
fi

echo ">>> ./gradlew ${TASKS[*]} ${GRADLE_ARGS[*]}"
set +e
./gradlew "${TASKS[@]}" "${GRADLE_ARGS[@]}"
STATUS=$?
set -e

echo
echo "================================ Отчёты ================================"
echo "Тесты (HTML, по модулям):  */build/reports/tests/test/index.html"
echo "Покрытие (HTML):          */build/reports/jacoco/test/html/index.html"
ALLURE_DIR="$( (find build/reports ${MODULE:+"$MODULE/build/reports"} -maxdepth 3 -type f -name index.html -path '*allure-report*' 2>/dev/null || true) | head -n1 | xargs -r dirname)"
if [[ -n "$ALLURE_DIR" ]]; then
  echo "Allure:                   $ALLURE_DIR/index.html"
  echo "  (открывать через HTTP:  python3 -m http.server 8090 --directory $ALLURE_DIR)"
fi
echo "Учёт процессов/потоков:   */build/test-processes/test/*.pid"
echo "========================================================================"

if [[ $SERVE -eq 1 && -n "$ALLURE_DIR" ]]; then
  echo "Allure: http://localhost:8090  (Ctrl+C — остановить)"
  python3 -m http.server 8090 --directory "$ALLURE_DIR"
fi

exit $STATUS
