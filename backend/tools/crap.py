"""Crapper: CRAP score per method from the JaCoCo XML report.

CRAP(m) = complexity^2 * (1 - coverage)^3 + complexity
coverage = covered instructions / all instructions of the method.

Usage (after ./mvnw verify):  python tools/crap.py target/site/jacoco/jacoco.xml [limit]
Prints methods at or above the limit (default 10) and exits 1 if there are any.
"""
import sys
import xml.etree.ElementTree as ET


def counters(method):
    found = {}
    for c in method.findall("counter"):
        missed, covered = int(c.get("missed")), int(c.get("covered"))
        found[c.get("type")] = (missed, covered)
    return found


def crap(method):
    c = counters(method)
    missed_cx, covered_cx = c.get("COMPLEXITY", (0, 0))
    complexity = missed_cx + covered_cx
    missed_in, covered_in = c.get("INSTRUCTION", (0, 0))
    total = missed_in + covered_in
    coverage = covered_in / total if total else 1.0
    return complexity ** 2 * (1 - coverage) ** 3 + complexity, complexity, coverage


def main():
    path = sys.argv[1]
    limit = float(sys.argv[2]) if len(sys.argv) > 2 else 10.0
    rows = []
    for cls in ET.parse(path).getroot().iter("class"):
        for m in cls.findall("method"):
            score, cx, cov = crap(m)
            rows.append((score, cx, cov, cls.get("name").replace("/", ".") + "." + m.get("name")))
    rows.sort(reverse=True)
    bad = [r for r in rows if r[0] >= limit]
    print(f"{len(rows)} methods, {len(bad)} with CRAP >= {limit:g}. Top 10:")
    for score, cx, cov, name in rows[:10]:
        print(f"  CRAP {score:6.1f}  complexity {cx:3d}  coverage {cov:6.1%}  {name}")
    sys.exit(1 if bad else 0)


if __name__ == "__main__":
    main()
