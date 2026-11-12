#!/usr/bin/env bash
# =============================================================================
#  Запуск юнит-тестов ТиОПО ЛР1 из командной строки (локальная копия репозитория).
#
#  По умолчанию: тесты domain + business-logic + data-access, затем
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
# =============================================================================
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

usage() { sed -n '2,24p' "$0" | sed 's/^# \{0,1\}//'; exit "${1:-0}"; }

GRADLE_ARGS=()
TASKS=()
MODULE=""
REPORTS=1
SERVE=0
LOW_MEM=1
OFFLINE=0

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
    -h|--help)       usage 0 ;;
    *) echo "Неизвестный параметр: $1" >&2; usage 1 ;;
  esac
  shift
done

# --- JDK 17: системная java или локально распакованная в ~/.local/share/jdks ----------
if [[ -z "${JAVA_HOME:-}" ]] && ! command -v java >/dev/null 2>&1; then
  LOCAL_JDK="$(ls -d "$HOME"/.local/share/jdks/jdk-17* 2>/dev/null | head -n1 || true)"
  if [[ -n "$LOCAL_JDK" ]]; then
    export JAVA_HOME="$LOCAL_JDK"
  else
    echo "Не найдена Java 17. Установите JDK 17 или задайте JAVA_HOME." >&2
    exit 1
  fi
fi
[[ -n "${JAVA_HOME:-}" ]] && export PATH="$JAVA_HOME/bin:$PATH"

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
  TASKS+=("unitTests")                       # тесты + coverageReport + coverageSummary + Allure
else
  TASKS+=(":domain:test" ":business-logic:test" ":data-access:test" "coverageReport" "coverageSummary")
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
[[ -f build/reports/jacoco/coverageReport/html/index.html ]] && \
  echo "Покрытие (HTML, общее):   build/reports/jacoco/coverageReport/html/index.html"
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
