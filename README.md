# Elevator System - Low Level Design

**Source:** [Hello Interview - Elevator LLD](https://www.hellointerview.com/learn/low-level-design/problem-breakdowns/elevator)  
**Difficulty:** Medium  
**Asked at:** Microsoft

---

## Problem Statement

> Design an elevator control system for a building. The system should handle multiple elevators, floor requests, and move elevators efficiently to service requests.

---

## Requirements

| #   | Requirement                                                                                         |
| --- | --------------------------------------------------------------------------------------------------- |
| 1   | System manages **3 elevators** serving **10 floors (0-9)**                                          |
| 2   | Users can request an elevator from any floor (hall call). System decides which elevator to dispatch |
| 3   | Once inside, users can select one or more destination floors                                        |
| 4   | Simulation runs in **discrete time steps** (`step()` advances time)                                 |
| 5   | Two request types: **Hall calls** (with direction UP/DOWN) and **Destinations** (no direction)      |
| 6   | System handles multiple concurrent pickup requests across floors                                    |
| 7   | Invalid requests rejected (return false) — non-existent floors                                      |
| 8   | Request for current floor = no-op (already there)                                                   |

**Out of scope:** Weight capacity, door mechanics, emergency stop, dynamic config, UI

---

## Key Clarifying Questions to Ask

1. How many elevators/floors? Fixed or configurable?
2. Hall call (up/down buttons) or destination dispatch (select floor before boarding)?
3. Can passengers select multiple destinations?
4. Should we distinguish hall calls (with direction) vs destination (no direction)?
5. How to handle invalid requests?
6. Capacity/weight limits? Door mechanics? Emergency stops?
7. **Simulation (`step()`) or real control software (hardware events)?** ← Senior-level insight

---

## Core Entities

| Entity                 | Responsibility                                                                |
| ---------------------- | ----------------------------------------------------------------------------- |
| **ElevatorController** | Orchestrator. Receives hall calls, dispatches to best elevator, advances time |
| **Elevator**           | Tracks position, direction, request queue. Executes movement (SCAN algorithm) |
| **Request**            | A stop: floor + type (PICKUP_UP, PICKUP_DOWN, DESTINATION)                    |
| **Direction**          | Enum: UP, DOWN, IDLE                                                          |
| **RequestType**        | Enum: PICKUP_UP, PICKUP_DOWN, DESTINATION                                     |

**Design decisions:**

- `Floor` stays as an integer (no behavior/state)
- `Request` is a class (not just int) — enables direction-aware stopping
- Controller is **stateless** beyond holding elevators (immediate dispatch, no pending queue)

---

## Class Design

```
ElevatorController
├── elevators: List<Elevator>
├── requestElevator(floor, type) → boolean    // external hall call
├── selectFloor(elevatorIndex, floor) → boolean  // passenger destination
└── step()                                     // advance all elevators

Elevator
├── currentFloor: int
├── direction: Direction
├── requests: Set<Request>
├── addRequest(request) → boolean
├── step()                          // movement logic (SCAN)
├── getCurrentFloor() → int
└── getDirection() → Direction

Request
├── floor: int (final)
├── type: RequestType (final)
├── equals() / hashCode()           // critical for HashSet lookups
```

---

## Algorithm: SCAN (Elevator Algorithm)

**Core idea:** Continue in current direction servicing all requests, then reverse when no more requests ahead.

### `Elevator.step()` — 5 Cases

```
Case 1: requests empty → direction = IDLE, return
Case 2: direction == IDLE → find nearest request, set direction
Case 3: current floor has matching request → remove it, return (don't move this tick)
Case 4: no requests ahead → reverse direction, return (don't move this tick)
Case 5: move one floor in current direction
```

### Critical Details

- **Stopping = no movement that tick** (return after removing request)
- **Reversal = no movement that tick** (let next tick check for stops at current floor)
- **Direction-aware stopping:** Going UP → only stop for PICKUP_UP and DESTINATION (skip PICKUP_DOWN)
- **Direction-agnostic travel:** `hasRequestsAhead()` checks ANY request type
- **No hardcoded boundary checks** — `hasRequestsAhead()` naturally handles floors 0 and 9
- **Deterministic tie-breaking** when IDLE: nearest request, then lower floor number (avoid HashSet iteration order)

### Why SCAN > FIFO?

FIFO bounces back and forth (8→3→7). SCAN sweeps in one direction (7→8→3). More efficient, fewer direction changes.

---

## Dispatch Strategy (selectBestElevator)

Priority tiers (best → worst):

| Priority | Strategy          | Description                                            |
| -------- | ----------------- | ------------------------------------------------------ |
| 1        | **Committed**     | Already moving toward the floor in the right direction |
| 2        | **Nearest Idle**  | Stationary elevator closest to requested floor         |
| 3        | **Nearest (any)** | Fallback — closest regardless of state                 |

**Why not just "nearest"?** An elevator on floor 2 going DOWN at floor 8 is "close" (distance 6) but will go DOWN first before coming back UP. A committed elevator already heading that way is better.

---

## Request Flow

```
[Person on floor 5 presses UP]
        │
        ▼
ElevatorController.requestElevator(5, PICKUP_UP)
        │
        ├── Validate (floor 0-9, not DESTINATION type)
        ├── selectBestElevator() → finds best Elevator
        └── elevator.addRequest(Request(5, PICKUP_UP))

[Elevator arrives at floor 5, person boards]
[Person presses floor 8 inside]
        │
        ▼
ElevatorController.selectFloor(elevatorIndex, 8)
        │
        └── elevator.addRequest(Request(8, DESTINATION))
```

---

## Tick-by-Tick Trace (for revision)

**Setup:** Elevator at floor 0, requests: {Request(5, PICKUP_UP)}

```
Tick 1: IDLE → direction=UP (nearest request at 5). Move to floor 1.
Tick 2: Floor 1, no stop. Move to floor 2.
Tick 3: Floor 2, no stop. Move to floor 3.
Tick 4: Floor 3, no stop. Move to floor 4.
Tick 5: Floor 4, no stop. Move to floor 5.
Tick 6: Floor 5, PICKUP_UP matches! Remove. requests={}, IDLE. (no move)
```

**Reversal example:** Elevator at floor 4, going UP. Requests: {Request(6, PICKUP_DOWN), Request(8, DESTINATION)}

```
Tick 1: Floor 4→5 (move up, no stop)
Tick 2: Floor 5→6 (move up)
Tick 3: Floor 6 — check PICKUP_UP at 6? No. check DESTINATION at 6? No. (PICKUP_DOWN skipped!) Move to 7.
Tick 4: Floor 7→8 (move up)
Tick 5: Floor 8 — DESTINATION matches! Remove. hasRequestsAhead(UP)? No. But requests={Request(6,PICKUP_DOWN)}.
         Reverse → direction=DOWN. (no move)
Tick 6: Floor 8 — check PICKUP_DOWN at 8? No. hasRequestsAhead(DOWN)? Yes (floor 6). Move to 7.
Tick 7: Floor 7→6 (move down)
Tick 8: Floor 6 — PICKUP_DOWN matches! Remove. requests={}, IDLE.
```

---

## Extensibility (Senior-level follow-ups)

| Question                  | Key Idea                                                                                                                                        |
| ------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------- |
| **Express elevator**      | Add `isExpress` flag + `expressFloors` set. Reject non-express floors in `addRequest`. Controller prefers express for those floors.             |
| **Cancel/undo request**   | Add `removeRequest(request)` to Elevator. Just removes from set. Core step() unchanged.                                                         |
| **Concurrent hall calls** | Option A: Lock around `requestElevator` + `step`. Option B: Thread-safe pending queue, drain into working set at start of each tick.            |
| **Strategy pattern**      | Extract `selectBestElevator` into `ElevatorSelectionStrategy` interface. Inject different strategies (nearest, direction-aware, load-balanced). |

---

## What Interviewers Expect

| Level      | Expectation                                                                                                                                                                         |
| ---------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Junior** | Basic entities, nearest-elevator dispatch, up/down movement, handles invalid inputs                                                                                                 |
| **Mid**    | SCAN algorithm correct with minimal hints, clean IDLE/UP/DOWN state machine, organized code (controller = coordination, elevator = movement)                                        |
| **Senior** | Asks simulation vs hardware upfront, priority-tier dispatch, handles all edge cases without prompting, discusses tradeoffs fluently, can sketch extensibility without restructuring |

---

## How to Run

```bash
cd /path/to/lld
javac Elevator/*.java
java Elevator.Main
```

**Must run from the parent of `Elevator/` directory** (package root).

---

## Files

| File                      | Purpose                                       |
| ------------------------- | --------------------------------------------- |
| `Direction.java`          | UP, DOWN, IDLE enum                           |
| `RequestType.java`        | PICKUP_UP, PICKUP_DOWN, DESTINATION enum      |
| `Request.java`            | Immutable (floor + type) with equals/hashCode |
| `Elevator.java`           | SCAN movement logic                           |
| `ElevatorController.java` | Dispatch + step orchestration                 |
| `Main.java`               | Test scenarios                                |
