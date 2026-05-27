# Biological models

В Vargod зашиты 7 эталонных ОДУ-моделей. Каждая реализована как `OdeModel` в
`business-logic/src/main/kotlin/com/vargod/engine/ode/`, зарегистрирована в
`MapModelRegistry` и засеяна в БД через `DataSeeder` (с дефолтными
RANGE-валидаторами и физическими ограничениями).

Числовое интегрирование — Dormand-Prince 5(4) (Apache Commons Math). По
умолчанию `maxStep=1.0`, `relTol=absTol=1e-10`. Жёсткие модели (Hodgkin-Huxley)
переопределяют это через `recommendedMaxStep`/`recommendedRelTol`/`recommendedAbsTol`
в `model_templates` (миграция `V7__model_template_solver_hints.sql`).

---

## SIR — модель эпидемии (Kermack-McKendrick, 1927)

Самая простая компартментальная модель распространения болезни.

```
dS/dt = -β · S · I / N
dI/dt =  β · S · I / N - γ · I
dR/dt =  γ · I
```

- **Переменные:** `S` (susceptible), `I` (infected), `R` (recovered).
- **Параметры:** `beta` (контактный коэффициент), `gamma` (скорость выздоровления), `N` (общая популяция).
- **Дефолтный валидатор:** `S + I + R = N`.
- **Типичные значения:** `beta=0.3, gamma=0.1, N=1000, S=999, I=1, R=0` — базовый сценарий эпидемии.
- **Ссылка:** [Wikipedia: Compartmental models](https://en.wikipedia.org/wiki/Compartmental_models_in_epidemiology).

---

## SEIRD — SIR + инкубация + смертность

Расширение SIR двумя компартментами: экспонированные (заражены, но не заразны) и умершие.

```
dS/dt = -β · S · I / N
dE/dt =  β · S · I / N - σ · E
dI/dt =  σ · E - (γ + μ) · I
dR/dt =  γ · I
dD/dt =  μ · I
```

- **Переменные:** `S, E, I, R, D`.
- **Параметры:** `beta`, `sigma` (1/инкубационный период), `gamma`, `mu` (летальность), `N`.
- **Сумма сохраняется:** `S+E+I+R+D = N` по построению (сумма правых частей = 0).
- **Типичные значения (COVID-подобный сценарий):** `beta=0.4, sigma=0.2, gamma=0.1, mu=0.01, N=1e6`.
- **Ссылка:** [Wikipedia: Compartmental models — SEIRD variants](https://en.wikipedia.org/wiki/Compartmental_models_in_epidemiology#The_SEIS_model).

---

## LOTKA_VOLTERRA — хищник-жертва (Lotka 1925, Volterra 1926)

Классическая 2D-модель с линейной функциональной реакцией.

```
dx/dt =  α · x - β · x · y
dy/dt = -γ · y + δ · x · y
```

- **Переменные:** `x` (жертва), `y` (хищник).
- **Параметры:** `alpha` (темп роста жертвы), `beta` (скорость поедания), `gamma` (смертность хищника), `delta` (конверсия).
- **Дефолтные значения:** `alpha=1.0, beta=0.1, gamma=1.5, delta=0.075`, начальные `x=10, y=5`.
- **Поведение:** периодические колебания вокруг центра `(γ/δ, α/β)`.
- **Ссылка:** [Wikipedia: Lotka–Volterra equations](https://en.wikipedia.org/wiki/Lotka%E2%80%93Volterra_equations).

---

## ROSENZWEIG_MACARTHUR — хищник-жертва с насыщением (1963)

Логистический рост жертвы + функциональная реакция Холлинга II типа `f(x) = a·x / (1 + a·b·x)`.

```
dx/dt = r · x · (1 - x/K) - f(x) · y
dy/dt = ε · f(x) · y - m · y
```

- **Переменные:** `x` (жертва), `y` (хищник).
- **Параметры:** `r` (рост жертвы), `K` (ёмкость), `a` (атака), `b` (время обработки), `eps` (конверсия), `m` (смертность хищника).
- **Дефолты:** `r=1.0, K=100, a=0.1, b=0.1, eps=0.1, m=0.5`.
- **Поведение:** в зависимости от параметров — стабильное сосуществование, предельный цикл (бифуркация Хопфа) или вымирание хищника.
- **Ссылка:** [Wikipedia: Rosenzweig–MacArthur model](https://en.wikipedia.org/wiki/Rosenzweig%E2%80%93MacArthur_model).

---

## LOGISTIC — логистический рост (Verhulst, 1838)

Одиночная популяция с ограниченной ёмкостью среды.

```
dP/dt = r · P · (1 - P/K)
```

- **Переменные:** `P` (численность).
- **Параметры:** `r` (внутренний темп роста), `K` (ёмкость среды).
- **Дефолты:** `r=0.5, K=1000`, `P0=10`.
- **Поведение:** S-образная кривая, асимптотически `P → K`.
- **Ссылка:** [Wikipedia: Logistic function — Verhulst](https://en.wikipedia.org/wiki/Logistic_function#In_ecology:_modeling_population_growth).

---

## FITZHUGH_NAGUMO — упрощённый нейрон (FitzHugh 1961, Nagumo 1962)

2D-аппроксимация Hodgkin-Huxley без подробной кинетики ионных каналов.

```
dV/dt = V - V³/3 - w + Iext
dw/dt = (V + a - b·w) / τ
```

- **Переменные:** `V` (мембранный потенциал), `w` (медленная восстанавливающая переменная).
- **Параметры:** `a`, `b`, `tau` (>0), `Iext` (внешний ток).
- **Дефолты:** `a=0.7, b=0.8, tau=12.5, Iext=0.5` — режим релаксационных автоколебаний.
- **Поведение:** в зависимости от `Iext` — устойчивая точка, спайки, бистабильность.
- **Ссылка:** [Wikipedia: FitzHugh–Nagumo model](https://en.wikipedia.org/wiki/FitzHugh%E2%80%93Nagumo_model).

---

## HODGKIN_HUXLEY — потенциал действия (1952)

Каноническая 4D-модель потенциала действия гигантского аксона кальмара. Жёсткая система.

```
Cm · dV/dt = Iext - gNa · m³ · h · (V - ENa) - gK · n⁴ · (V - EK) - gL · (V - EL)
dn/dt = αn(V) · (1 - n) - βn(V) · n
dm/dt = αm(V) · (1 - m) - βm(V) · m
dh/dt = αh(V) · (1 - h) - βh(V) · h
```

- **Переменные:** `V` (мВ), `n, m, h` (воротные переменные ∈ [0, 1]).
- **Параметры:**
  - `gNa, gK, gL` (мСм/см²) — максимальные проводимости.
  - `ENa, EK, EL` (мВ) — потенциалы реверсии.
  - `Cm` (мкФ/см²) — мембранная ёмкость (>0).
  - `Iext` (мкА/см²) — инжекционный ток.
- **Стандартные параметры:** `gNa=120, gK=36, gL=0.3, ENa=50, EK=-77, EL=-54.4, Cm=1.0, Iext=10`.
- **Solver hints в БД:** `maxStep=0.01, relTol=1e-6, absTol=1e-9` (без них Dormand-Prince 5(4) даст численную деградацию).
- **Особенности:** `αn` и `αm` имеют устранимые особенности при `V=-55` и `V=-40` (l'Hopital → `0.1` и `1.0`).
- **Ссылка:** [Wikipedia: Hodgkin–Huxley model](https://en.wikipedia.org/wiki/Hodgkin%E2%80%93Huxley_model).

---

## Где смотреть в коде

| Модель | Файл реализации | Тест |
| --- | --- | --- |
| SIR | `business-logic/.../engine/ode/SirModel.kt` | `SirModelTest.kt` |
| SEIRD | `.../SeirdModel.kt` | `SeirdModelTest.kt` |
| LOTKA_VOLTERRA | `.../LotkaVolterraModel.kt` | `LotkaVolterraModelTest.kt` |
| ROSENZWEIG_MACARTHUR | `.../RosenzweigMacArthurModel.kt` | `RosenzweigMacArthurModelTest.kt` |
| LOGISTIC | `.../LogisticGrowthModel.kt` | `LogisticGrowthModelTest.kt` |
| FITZHUGH_NAGUMO | `.../FitzHughNagumoModel.kt` | `FitzHughNagumoModelTest.kt` |
| HODGKIN_HUXLEY | `.../HodgkinHuxleyModel.kt` | `HodgkinHuxleyModelTest.kt` |

Дефолтные значения параметров и валидаторы кодируются в `DataSeeder`
(`data-access/src/main/kotlin/com/vargod/repository/jdbc/DataSeeder.kt`).

Хотите добавить свою модель? См. [how-to-add-model.md](how-to-add-model.md).
