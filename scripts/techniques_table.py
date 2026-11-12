#!/usr/bin/env python3
"""
Таблица «тест → техника подготовки тестовых данных → стиль» (ТиОПО ЛР1, задание 3).

Техника каждого теста записана аннотацией над ним (@BoundaryValues, @Pairwise, ...,
см. domain/src/testFixtures/.../testing/Tags.kt). Скрипт разбирает исходники тестов
(тесты при этом не запускаются) и пишет markdown-таблицу:

    python3 scripts/techniques_table.py > docs/lab-reports/tiopo-lab1-techniques.md
"""
import collections
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
MODULES = ["business-logic", "data-access"]

TECHNIQUES = {
    "EquivalencePartitioning": "Классы эквивалентности",
    "BoundaryValues": "Граничные значения",
    "StateTransition": "Переходы состояний",
    "DecisionTable": "Таблица решений",
    "Pairwise": "Попарное (комбинаторное)",
    "ErrorGuessing": "Предугадывание ошибок",
}
BASES = {  # базовые классы, задающие стиль наследникам
    "DbTestBase": "классический (PostgreSQL)",
    "OdeModelContractTest": "классический",
}

CLASS_RE = re.compile(r"^(?:abstract )?class (\w+)(?:\s*:\s*(\w+)\(\))?")
FUN_RE = re.compile(r"^    fun `([^`]+)`")
ANN_RE = re.compile(r"^\s*@(\w+)")


def parse(path: pathlib.Path):
    lines = path.read_text(encoding="utf-8").splitlines()
    tests, cls, style, pending = [], None, None, []
    for line in lines:
        m = CLASS_RE.match(line)
        if m:
            cls, base = m.group(1), m.group(2)
            ann = set(pending)
            style = ("лондонский" if "London" in ann else
                     "классический" if "Classical" in ann else BASES.get(base) or BASES.get(cls, "—"))
            if "DbTest" in ann:
                style += " (PostgreSQL)"
            pending = []
            continue
        a = ANN_RE.match(line)
        if a:
            pending.append(a.group(1))
            continue
        f = FUN_RE.match(line)
        if f and cls:
            techs = [TECHNIQUES[x] for x in pending if x in TECHNIQUES]
            param = "ParameterizedTest" in pending
            tests.append((cls, style, f.group(1), ", ".join(techs) or "—", param))
            pending = []
            continue
        if line.strip() and not line.strip().startswith(("*", "/**", "//", "*/")):
            pending = []
    return tests


def main():
    out = sys.stdout
    all_tests = []
    for module in MODULES:
        for path in sorted((ROOT / module / "src" / "test" / "kotlin").rglob("*.kt")):
            for t in parse(path):
                all_tests.append((module, path.relative_to(ROOT).as_posix()) + t)

    counts = collections.Counter(t[5] for t in all_tests)
    out.write("# ТиОПО ЛР1 — техники подготовки тестовых данных\n\n")
    out.write("> Сгенерировано `python3 scripts/techniques_table.py` по аннотациям над тестами.\n")
    out.write("> Параметризованный тест (★) — один метод, несколько наборов данных.\n")
    out.write("\n")
    out.write("## Сводка\n\n| Техника | Тест-методов |\n|---|---|\n")
    for tech, n in counts.most_common():
        out.write(f"| {tech} | {n} |\n")
    out.write(f"| **Всего тест-методов** | **{len(all_tests)}** |\n\n")

    current = None
    for module, path, cls, style, name, tech, param in all_tests:
        if (path, cls) != current:
            current = (path, cls)
            out.write(f"\n### `{cls}` — {module}, стиль: {style}\n\n")
            out.write(f"Файл: [{path.split('/')[-1]}](../../{path})\n\n")
            out.write("| Тест | Техника |\n|---|---|\n")
        star = " ★" if param else ""
        out.write(f"| {name}{star} | {tech} |\n")


if __name__ == "__main__":
    main()
