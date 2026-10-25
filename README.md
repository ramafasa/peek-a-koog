# 🙈 Peek-a-Koog

A Kotlin AI agent learns to play hide-and-seek, one failure at a time.

You hide a toy somewhere in a small house. An agent built with [Koog](https://github.com/JetBrains/koog)
has to find it. The first agent is hopeless. Every level of this repo fixes the failure the previous
level showed. This is the demo app for a KotlinConf talk proposal.

## The rule that matters most

The **engine is plain, deterministic Kotlin** (`engine/`). It knows where the toy is and it decides whether
the seeker found it. The LLM only chooses what to do next. A model that *says* it found the toy proves nothing.

## Modules

| Module | What's inside |
|---|---|
| `engine` | The house, the rules and the event log. No AI, fully unit-tested. |
| `agent` | Koog seekers, one per `Level`, and the tools they use (`SeekerTools`). |
| `app` | Compose Desktop UI: the house map, level picker and the engine's event log. |

## Levels

| Level | Idea | Expected failure |
|---|---|---|
| 0 · Just a prompt | No tools at all | Claims to have found the toy without looking anywhere |
| 1 · Tools, in the dark | Tools + Koog's default strategy | Does odd things and you can't see why |
| 2 · Observable | Event handlers, tracing, OpenTelemetry | *planned* |
| 3 · Stateful | Beliefs as structured output, heatmap | *planned* |
| 4 · Graph | Custom strategy graph, move budget, history compression | *planned* |
| 5 · Guarded | The engine, not the model, decides who won | *planned* |
| 6 · Mature | Evals, long-term memory, persistence, model race | *planned* |

The commit that finishes a level gets the tag `level-N`.

## Running

Requires JDK 17+. The seeker picks its LLM from the environment:

| Variable | Model |
|---|---|
| `ANTHROPIC_API_KEY` | Claude Sonnet 5 |
| `OPENAI_API_KEY` | GPT-5 mini |
| *(none)* | `llama3.2` via a local [Ollama](https://ollama.com) |

```bash
export ANTHROPIC_API_KEY=...
./gradlew :app:run
```

Click a hiding spot to hide the toy, pick a level and press **Start seeking**.

```bash
./gradlew test   # engine rules + agent tests with a mocked LLM
```

## Boo! journal

Every real failure goes into [`JOURNAL.md`](JOURNAL.md), with a date and a screenshot.
That journal is the script of the talk.
