# drawStudent — one method, every character

How one drawing method covers all the people in the film, and how to give it a
new pose. Read this before touching any `body*` / `details*` method.

Companion to [storyboard.md](../storyboard.md) — that file says *what* to draw,
this one says *how the character code is wired*.

## Where the code lives

The project is split across eight files in the default package:

| file | holds |
|---|---|
| `Assignment1_67050285.java` | entry point - opens the window |
| `Film.java` | the canvas, the clock and the preview keys |
| `Shots.java` | the film script - six shots on one timeline |
| `Art.java` | palette, colour maths, canvas size |
| `Midpoint.java` | hand written circle / ellipse rasterisers |
| **`Figure.java`** | **everything in this document** |
| `Scene.java` | the common room of shots 1 and 2 |
| `Hall.java` | result page, lecture hall, stage, rain cloud |

Classes in the default package **cannot be imported**, so every cross-file
reference is written out: `Art.S1_WALL`, `Midpoint.fillCircle(...)`,
`Figure.POSE_CHEER`. That is why the class names are short.

Build and run with:

```
javac *.java
java Assignment1_67050285
```

---

## 1. The one entry point

`drawStudent` is an **instance** method on `Figure`. The panel keeps one
`Figure fig`, so shot code reads `fig.drawStudent(...)`.

```java
drawStudent(Graphics2D g, double x, double y, double scale, int pose, double t)
```

| argument | meaning |
|---|---|
| `x, y` | where the **hip** lands on screen |
| `scale` | size of the figure. **Negative mirrors it** so it faces left |
| `pose` | which shape to draw — the `POSE_*` constants |
| `t` | shot-local time, so a pose can breathe, shake or droop |

Same person, same place, different shot:

```java
drawStudent(g, 286, 470,      3.4, POSE_COVER_EYES, ta);  // shot 1
drawStudent(g, 286, 470 - j3, 3.4, POSE_CHEER,      t);   // shot 2
```

That is the whole idea: **the shot picks a pose number, nothing else changes.**

---

## 2. Local coordinates

`drawStudent` does `translate(x, y)` then `scale(...)` before calling any pose
method, so every pose draws around **(0,0) = the hip**, with negative y going up.

```
   -68   top of head
   -56   head centre = (12, -56), radius 12
   -44   chin
   -40   shoulder
    -6   waist
     0   hip          <-- (0,0), this is what x,y positions
   +22   foot (when legs are drawn at all)
```

Face features are written relative to the head centre, so they survive a head
that moves:

| feature | y |
|---|---|
| eyes | `headY + 1` |
| mouth | `headY + 7` |
| fringe | `headY - 14` to `headY - 4` |

**Never move a character by editing shape numbers.** Change `x, y` only. That
is the rule storyboard §6 asks for, and it is why the same pose works at three
different places in shot 1.

---

## 3. The four fixed steps

`drawStudent` always runs these in order, for every pose:

1. **`body()` — twice** (see §4)
2. **`hairBack()`** — hair that falls *behind* the head
3. **the head** — two `Midpoint.fillCircle` calls: ink, then skin on top
4. **`details()`** — face, fringe, hands

Only four methods ever look at `pose`:

```
body()          details()          hairBack()          headLocalX/Y()
```

Nothing else in the file branches on it.

### The body / details rule

The head circle is drawn **between** step 1 and step 4. So:

| the part is… | put it in |
|---|---|
| behind the head — torso, arms, legs, skirt | `body()` |
| in front of the head — eyes, mouth, fringe, hands over the face | `details()` |

This is why the hands covering the boy's eyes live in `detailsCoverEyes` and
not in the body: they have to paint over the face.

---

## 4. The two-pass outline, and why body uses `part()`

`body()` is called twice from a loop:

- **pass 0** — `outlinePass = true`, `strokeBoost = 1.2`. Everything is drawn
  fat, in the ink colour.
- **pass 1** — `outlinePass = false`, `strokeBoost = 0`. Everything is drawn
  again at normal width, in real colours, on top.

What still shows from pass 0 is an even ink line around every limb. One loop,
and the whole figure is outlined — no outline shape is ever written by hand.

So inside `body()` you must write:

```java
part(g, clothColor);      // correct
g.setColor(clothColor);   // WRONG - kills the outline
```

`part()` hands back the ink colour during pass 0 and the real colour during
pass 1.

`details()` runs **once**, so there `g.setColor` is correct and normal.

> `strokeBoost = 1.2` local units is about a **2 px** line at the usual figure
> scale of ~3.3. That matches the reference art. It was 2.6 at one point, which
> is 8.6 px, and that single number was most of why the render looked like a
> thick cartoon instead of soft line art.

---

## 5. Colours are fields, not arguments

Set them **before** the call:

| field | what it paints |
|---|---|
| `outlineColor` | every ink line |
| `skinColor` | face, hands, bare arms |
| `hairColor` | hair |
| `shirtColor` | uniform shirt |
| `clothColor` | jacket / cardigan / skirt — whatever this character wears |
| `litColor` | screen-light accents |
| `wearsJacket` | `true` = tracksuit torso, `false` = skirt (used by `POSE_CHEER`) |

```java
clothColor = S1_CARDI;
wearsJacket = false;
drawStudent(g, 118, 392, 3.3, POSE_LEAN_LAUGH, ta);
```

This is why shot 6 can grey the whole character out with
`skinColor = desat(S1_SKIN, grey)` without touching one line of pose code.

---

## 6. Helpers a pose can use

| helper | use |
|---|---|
| `stroke(g, w)` | set line width in **local units**, adds `strokeBoost` for you |
| `limb(g, x1,y1, x2,y2, x3,y3)` | two-segment limb: shoulder-elbow-hand, or hip-knee-foot |
| `inkedShape(g, shape, fill, w)` | stroke a shape fat in ink, then thin in its colour |
| `fillInked(g, shape, fill, w)` | fill a shape and give it an ink outline |
| `hairCap(g, cx, cy, sweep)` | fringe over the forehead |
| `shake(t)` | high frequency, small amplitude — nerves |
| `Art.lerp / desat / ease / clamp` | colour and easing maths, all static |
| `Midpoint.circle / ellipse / fillCircle / fillEllipse / arc` | the rasterisers |

---

## 7. Adding a pose — four steps

Example: `POSE_SIT_STRAIGHT` for shot 4. The constant already exists.

**Step 1 — head position**, only if the head is not at the default `(12, -56)`:

```java
double headLocalY(int pose) {
    if (pose == POSE_SIT_STRAIGHT) {
        return -58;              // sitting tall
    }
    ...
}
```

**Step 2 — the body.** Use `part()` and `stroke()`:

```java
void bodySitStraight(Graphics2D g, double breath) {
    double shoY = -40 + breath;
    part(g, shirtColor);
    stroke(g, 15);
    g.draw(new CubicCurve2D.Double(2, -4, 2, -20, 3, -32, 5, shoY));
    part(g, skinColor);
    stroke(g, 6);
    limb(g, 1, shoY + 4, -3, -22, 2, -8);
}
```

**Step 3 — the details.** Fringe and face, drawn once, plain `g.setColor`:

```java
void detailsSitStraight(Graphics2D g, double breath) {
    double headY = -58 + breath;
    hairCap(g, 12, headY, 2);
    g.setColor(outlineColor);
    stroke(g, 1.8);
    g.draw(new Line2D.Double(6, headY + 1, 10, headY + 1));
    g.draw(new Line2D.Double(14, headY + 1, 18, headY + 1));
}
```

**Step 4 — wire both dispatchers**, `body()` and `details()`:

```java
} else if (pose == POSE_SIT_STRAIGHT) {
    bodySitStraight(g, breath);
}
```

Then call it. Nothing else in the file changes.

---

## 8. Animating inside a pose

Two ways, use whichever fits.

**Drive it from `t`** — good for idle motion that never stops:

```java
double tremble = shake(t);        // sin(t * 2π * 11) * 1.6
g.translate(tremble * 0.35, 0);
```

**Pass an extra argument** — good when the shot controls how far into the pose
the character is. Shot 6 does this: the same figure bends down and straightens
back up because `slumpFigure` takes `droop` and `lift` on top of `t`.

Rule of thumb: **`pose` picks the shape, extra arguments say how far into it.**

---

## 9. Traps that already cost time

- **Round caps overshoot.** A `stroke(g, 17)` line ending at the shoulder
  `y = -40` actually reaches `-48.5`, because a round cap adds half the width
  past the endpoint. That is what eats the neck. Either end the stroke short,
  or use a filled closed path for the torso.
- **Midpoint routines go chunky under `scale`.** `Midpoint.plot` is a
  `fillRect(x, y, 1, 1)`, and inside a
  3.3× transform that paints 3.3 px blocks. That is why the head is rasterised in
  **screen space**, using `x + hlx * scale`, not inside the scaled graphics. Do
  the same for any circle that must look smooth.
- **Negative scale mirrors x only.** `drawStudent` uses
  `g2.scale(scale, Math.abs(scale))` — pass `-3.3` and the figure faces left
  without flipping upside down. Anything you compute in screen space from a
  mirrored figure must use `scale` for x and `Math.abs(scale)` for y.
- **A pose that jumps needs a longer lower body.** In shot 2 the table hides
  everyone's lap, so `bodyCheer` runs its skirt down to `y = +36`. Stop short
  and the bounce exposes a cut-off torso above the table edge.
