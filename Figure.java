import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.QuadCurve2D;

/**
 * Every person in the film. One drawStudent call draws any of them: the pose
 * argument picks the shape, the colour fields below pick the outfit.
 *
 * Read docs/drawStudent.md before adding a pose.
 */
final class Figure {

    static final int POSE_COVER_EYES = 0;   // shot 1 - hands over the eyes
    static final int POSE_LEAN_LAUGH = 1;   // shot 1 - friend laughing, phone down
    static final int POSE_LEAN_WATCH = 2;   // shot 1 - friend leaning on the table
    static final int POSE_CHEER = 3;        // shot 2 - arms thrown up
    static final int POSE_SIT_STRAIGHT = 4; // shot 4 (not built yet)
    static final int POSE_SLUMP = 5;        // shot 6 (not built yet)

    Color outlineColor = Art.S1_INK;
    Color skinColor = Art.S1_SKIN;
    Color hairColor = Art.S1_HAIR;
    Color shirtColor = Art.S1_SHIRT;
    /** Jacket, cardigan or skirt - whatever the current character wears. */
    Color clothColor = Art.S1_NAVY;
    /** Screen light accents. */
    Color litColor = Art.S1_GLOW;
    /** true = tracksuit torso, false = skirt. Used by POSE_CHEER. */
    boolean wearsJacket = false;

    /** Local units added to every body stroke on the outline pass. */
    double strokeBoost = 0;
    /** True while the fat under-pass that becomes the outline is drawn. */
    boolean outlinePass = false;

    /** Picks a part colour, or the outline colour while the outline pass runs. */
    void part(Graphics2D g, Color c) {
        g.setColor(outlinePass ? outlineColor : c);
    }

    /**
     * Draws a student. Everything inside is relative to (0,0) which is the
     * hip, so moving the figure only changes x/y. A negative scale mirrors
     * the figure so it faces left.
     *
     * The body is drawn twice: once fat in the outline colour, then again in
     * its own colours on top. What is left showing of the first pass is an
     * even ink line around every limb.
     */
    void drawStudent(Graphics2D g, double x, double y, double scale, int pose, double t) {
        double as = Math.abs(scale);
        double breath = Math.sin(t * 2 * Math.PI * 0.55) * 0.8;
        double hlx = headLocalX(pose);
        double hly = headLocalY(pose) + breath;

        for (int pass = 0; pass < 2; pass++) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.translate(x, y);
            g2.scale(scale, as);
            outlinePass = (pass == 0);
            // 1.2 local units at a scale of ~3.3 lands a ~2 px ink line, which
            // is what the reference art uses. Anything fatter reads as a thick
            // cartoon outline instead of soft line art.
            strokeBoost = outlinePass ? 1.2 : 0;
            body(g2, pose, t, breath);
            g2.dispose();
        }
        outlinePass = false;
        strokeBoost = 0;

        // hair that falls behind the head, before the face goes on top of it
        Graphics2D gb = (Graphics2D) g.create();
        gb.translate(x, y);
        gb.scale(scale, as);
        hairBack(gb, pose, hlx, hly);
        gb.dispose();

        // The head is rasterised in screen space so the midpoint circle keeps
        // one pixel steps instead of blocks the size of the figure scale.
        double hx = x + hlx * scale;
        double hy = y + hly * as;
        double hr = 12 * as;
        g.setColor(outlineColor);
        Midpoint.fillCircle(g, (int) Math.round(hx), (int) Math.round(hy),
                (int) Math.round(hr + 0.72 * as));
        g.setColor(skinColor);
        Midpoint.fillCircle(g, (int) Math.round(hx), (int) Math.round(hy), (int) Math.round(hr));

        Graphics2D g3 = (Graphics2D) g.create();
        g3.translate(x, y);
        g3.scale(scale, as);
        details(g3, pose, t, breath);
        g3.dispose();
    }

    double headLocalX(int pose) {
        if (pose == POSE_LEAN_LAUGH) {
            return 24;
        }
        if (pose == POSE_LEAN_WATCH) {
            return 22;
        }
        return 12;
    }

    double headLocalY(int pose) {
        if (pose == POSE_LEAN_LAUGH) {
            return -44;
        }
        if (pose == POSE_LEAN_WATCH) {
            return -42;
        }
        return -56;
    }

    void body(Graphics2D g, int pose, double t, double breath) {
        if (pose == POSE_LEAN_LAUGH) {
            bodyLeanLaugh(g, breath);
        } else if (pose == POSE_LEAN_WATCH) {
            bodyLeanWatch(g, breath);
        } else if (pose == POSE_CHEER) {
            bodyCheer(g, t, breath);
        } else {
            bodyCoverEyes(g, t, breath);
        }
    }

    void details(Graphics2D g, int pose, double t, double breath) {
        if (pose == POSE_LEAN_LAUGH) {
            detailsLeanLaugh(g, breath);
        } else if (pose == POSE_LEAN_WATCH) {
            detailsLeanWatch(g, breath);
        } else if (pose == POSE_CHEER) {
            detailsCheer(g, breath);
        } else {
            detailsCoverEyes(g, t, breath);
        }
    }

    /** Both arms thrown straight up. */
    void bodyCheer(Graphics2D g, double t, double breath) {
        double shoY = -40 + breath;
        double swing = Math.sin(t * 2 * Math.PI * 2.2) * 2.5;

        // lower body runs well past the table edge so the jump never exposes
        // a cut off torso
        part(g, wearsJacket ? clothColor : Art.S1_NAVY);
        stroke(g, 1.2);
        GeneralPath lower = new GeneralPath();
        lower.moveTo(-3, -6);
        lower.lineTo(14, -6);
        lower.lineTo(19, 36);
        lower.lineTo(-8, 36);
        lower.closePath();
        g.fill(lower);
        g.draw(lower);

        part(g, wearsJacket ? clothColor : shirtColor);
        stroke(g, 16);
        g.draw(new CubicCurve2D.Double(2, -4, 2, -18, 3, -30, 5, shoY));

        part(g, skinColor);
        stroke(g, 5);
        limb(g, 1, shoY + 4, -5, shoY - 12, -1 + swing, shoY - 28);
        limb(g, 10, shoY + 4, 17, shoY - 12, 14 - swing, shoY - 29);

        // short sleeves capping the arms
        part(g, wearsJacket ? clothColor : shirtColor);
        stroke(g, 9);
        g.draw(new Line2D.Double(2, shoY + 2, -1, shoY - 5));
        g.draw(new Line2D.Double(10, shoY + 2, 13, shoY - 5));
    }

    void detailsCheer(Graphics2D g, double breath) {
        double headY = -56 + breath;
        double shoY = -40 + breath;

        g.setColor(Art.lerp(shirtColor, outlineColor, 0.4));
        stroke(g, 1.6);
        g.draw(new Line2D.Double(2, shoY - 2, 6, shoY + 7));
        g.draw(new Line2D.Double(10, shoY - 2, 6, shoY + 7));

        hairCap(g, 12, headY, 2);

        // eyes shut with joy, mouth wide open
        g.setColor(outlineColor);
        stroke(g, 1.8);
        g.draw(new QuadCurve2D.Double(5, headY + 2, 7.5, headY - 2, 10, headY + 2));
        g.draw(new QuadCurve2D.Double(14, headY + 2, 16.5, headY - 2, 19, headY + 2));
        GeneralPath mouth = new GeneralPath();
        mouth.moveTo(9, headY + 6);
        mouth.quadTo(12, headY + 13, 15, headY + 6);
        mouth.quadTo(12, headY + 4, 9, headY + 6);
        mouth.closePath();
        g.setColor(Art.lerp(Art.rgb("#E24B4B"), outlineColor, 0.35));
        g.fill(mouth);
        g.setColor(outlineColor);
        stroke(g, 1.4);
        g.draw(mouth);
    }

    /** Seated, elbows on the table, both hands clamped over the eyes. */
    void bodyCoverEyes(Graphics2D g, double t, double breath) {
        double shoY = -40 + breath;

        // tracksuit jacket
        part(g, clothColor);
        stroke(g, 17);
        g.draw(new CubicCurve2D.Double(2, -4, 2, -18, 3, -30, 5, shoY));
        stroke(g, 8);
        g.draw(new QuadCurve2D.Double(-3, shoY + 6, -6, -22, -4, -10));
        g.draw(new QuadCurve2D.Double(13, shoY + 6, 16, -22, 14, -10));
    }

    /** Hair, jacket trim, forearms and the hands he is hiding behind. */
    void detailsCoverEyes(Graphics2D g, double t, double breath) {
        double headY = -56 + breath;
        double shoY = -40 + breath;
        // a fine tremble, high frequency and small - he cannot keep still
        double tremble = shake(t);

        // white stripe down each sleeve
        g.setColor(shirtColor);
        stroke(g, 2.2);
        g.draw(new QuadCurve2D.Double(-3, shoY + 6, -6, -22, -4, -11));
        g.draw(new QuadCurve2D.Double(13, shoY + 6, 16, -22, 14, -11));

        // collar
        g.setColor(Art.lerp(clothColor, Color.WHITE, 0.35));
        stroke(g, 2.2);
        g.draw(new Line2D.Double(2, shoY - 2, 6, shoY + 7));
        g.draw(new Line2D.Double(10, shoY - 2, 6, shoY + 7));

        // sleeved forearms from the elbows on the table up to the wrists
        g.translate(tremble * 0.35, 0);
        inkedShape(g, forearmPath(headY), clothColor, 6);
        g.setColor(shirtColor);
        stroke(g, 1.8);
        g.draw(forearmPath(headY));

        // palms over the eyes, fingers pointing up
        inkedShape(g, new Line2D.Double(5.5, headY + 8, 6.5, headY - 5), skinColor, 7.5);
        inkedShape(g, new Line2D.Double(16.5, headY + 8, 15.5, headY - 5), skinColor, 7.5);

        // finger seams
        g.setColor(Art.lerp(skinColor, outlineColor, 0.55));
        stroke(g, 1.2);
        g.draw(new Line2D.Double(4.0, headY + 6, 5.0, headY - 4));
        g.draw(new Line2D.Double(7.6, headY + 6, 8.3, headY - 4));
        g.draw(new Line2D.Double(15.0, headY + 6, 14.2, headY - 4));
        g.draw(new Line2D.Double(18.6, headY + 6, 17.7, headY - 4));

        // the gap he is peeking through, lit by the screen
        g.setColor(litColor);
        stroke(g, 2.0);
        g.draw(new Line2D.Double(11.2, headY + 6, 11.0, headY - 4));
        g.translate(-tremble * 0.35, 0);

        hairCap(g, 12, headY, 2);

        // one bead of sweat by the temple
        g.setColor(Art.lerp(litColor, Color.WHITE, 0.45));
        GeneralPath drop = new GeneralPath();
        drop.moveTo(26, headY - 8);
        drop.quadTo(28.5, headY - 5, 26, headY - 3);
        drop.quadTo(23.5, headY - 5, 26, headY - 8);
        drop.closePath();
        g.fill(drop);
        g.setColor(Art.lerp(litColor, outlineColor, 0.4));
        stroke(g, 1.2);
        g.draw(drop);
    }

    GeneralPath forearmPath(double headY) {
        GeneralPath p = new GeneralPath();
        p.moveTo(1, -9);
        p.quadTo(0, -26, 5, headY + 10);
        p.moveTo(19, -11);
        p.quadTo(21, -28, 16, headY + 10);
        return p;
    }

    /** Standing, bent over the table, laughing, phone in one hand. */
    void bodyLeanLaugh(Graphics2D g, double breath) {
        double shoY = -30 + breath;

        // skirt, disappearing behind the table
        part(g, clothColor == Art.S1_CARDI ? Art.S1_NAVY : clothColor);
        stroke(g, 3);
        GeneralPath skirt = new GeneralPath();
        skirt.moveTo(-4, -6);
        skirt.lineTo(12, -6);
        skirt.lineTo(19, 26);
        skirt.lineTo(-9, 26);
        skirt.closePath();
        if (outlinePass) {
            g.setColor(outlineColor);
            g.fill(skirt);
        }
        g.draw(skirt);
        if (!outlinePass) {
            g.setColor(Art.S1_NAVY);
            g.fill(skirt);
        }

        // torso leaning forward
        part(g, shirtColor);
        stroke(g, 15);
        g.draw(new CubicCurve2D.Double(3, -6, 5, -16, 9, -24, 14, shoY));

        // cardigan sleeves: one arm hanging, one holding the phone at her side
        part(g, clothColor);
        stroke(g, 8);
        limb(g, 12, shoY + 4, 8, -10, 13, 22);
        limb(g, 16, shoY + 2, 26, -16, 19, -4);
    }

    void detailsLeanLaugh(Graphics2D g, double breath) {
        double headY = -44 + breath;
        double shoY = -30 + breath;

        // the phone in her lowered hand, well clear of her face
        inkedShape(g, new Line2D.Double(18, -6, 20, -2), skinColor, 5);
        GeneralPath phone = new GeneralPath();
        phone.moveTo(15, -13);
        phone.lineTo(23, -11);
        phone.lineTo(20, 1);
        phone.lineTo(12, -1);
        phone.closePath();
        g.setColor(Art.lerp(Art.S1_INK, Color.WHITE, 0.25));
        g.fill(phone);
        g.setColor(outlineColor);
        stroke(g, 2);
        g.draw(phone);

        g.setColor(Art.lerp(shirtColor, outlineColor, 0.4));
        stroke(g, 1.6);
        g.draw(new Line2D.Double(11, shoY - 2, 15, shoY + 7));
        g.draw(new Line2D.Double(19, shoY - 2, 15, shoY + 7));

        hairCap(g, 24, headY, 3);

        // eyes shut, mouth wide - she is delighted
        g.setColor(outlineColor);
        stroke(g, 1.8);
        g.draw(new QuadCurve2D.Double(17, headY + 1, 19.5, headY - 3, 22, headY + 1));
        g.draw(new QuadCurve2D.Double(26, headY + 1, 28.5, headY - 3, 31, headY + 1));
        GeneralPath mouth = new GeneralPath();
        mouth.moveTo(23, headY + 6);
        mouth.quadTo(25.5, headY + 11, 28, headY + 6);
        mouth.quadTo(25.5, headY + 4.5, 23, headY + 6);
        mouth.closePath();
        g.setColor(Art.lerp(Art.rgb("#E24B4B"), outlineColor, 0.35));
        g.fill(mouth);
        g.setColor(outlineColor);
        stroke(g, 1.4);
        g.draw(mouth);
    }

    /** Standing, both hands planted on the table, watching the screen. */
    void bodyLeanWatch(Graphics2D g, double breath) {
        double shoY = -28 + breath;

        part(g, Art.S1_NAVY);
        stroke(g, 3);
        GeneralPath skirt = new GeneralPath();
        skirt.moveTo(-4, -4);
        skirt.lineTo(11, -4);
        skirt.lineTo(17, 26);
        skirt.lineTo(-9, 26);
        skirt.closePath();
        if (outlinePass) {
            g.setColor(outlineColor);
            g.fill(skirt);
        }
        g.draw(skirt);
        if (!outlinePass) {
            g.setColor(Art.S1_NAVY);
            g.fill(skirt);
        }

        part(g, shirtColor);
        stroke(g, 14);
        g.draw(new CubicCurve2D.Double(3, -4, 5, -14, 8, -22, 13, shoY));

        // both arms reaching down past the table edge, where the hands wait
        part(g, skinColor);
        stroke(g, 6.5);
        limb(g, 11, shoY + 5, 14, -12, 11, 15);
        limb(g, 16, shoY + 3, 24, -10, 22, 13);

        // short sleeves
        part(g, shirtColor);
        stroke(g, 9);
        g.draw(new Line2D.Double(11, shoY + 3, 13, shoY + 10));
        g.draw(new Line2D.Double(16, shoY + 1, 20, shoY + 8));
    }

    void detailsLeanWatch(Graphics2D g, double breath) {
        double headY = -42 + breath;
        double shoY = -28 + breath;

        g.setColor(Art.lerp(shirtColor, outlineColor, 0.4));
        stroke(g, 1.6);
        g.draw(new Line2D.Double(10, shoY - 2, 14, shoY + 7));
        g.draw(new Line2D.Double(18, shoY - 2, 14, shoY + 7));

        hairCap(g, 22, headY, 2);

        // calm, interested face
        g.setColor(outlineColor);
        stroke(g, 2.2);
        g.draw(new Line2D.Double(17, headY + 1, 17, headY + 4));
        g.draw(new Line2D.Double(26, headY + 1, 26, headY + 4));
        stroke(g, 1.6);
        g.draw(new QuadCurve2D.Double(20, headY + 8, 22.5, headY + 10, 25, headY + 8));
    }

    /** Hair masses that sit behind the head, drawn before the face. */
    void hairBack(Graphics2D g, int pose, double cx, double cy) {
        if (pose == POSE_LEAN_LAUGH) {
            GeneralPath tail = new GeneralPath();
            tail.moveTo(cx - 8, cy - 9);
            tail.quadTo(cx - 24, cy - 10, cx - 27, cy + 9);
            tail.quadTo(cx - 17, cy + 4, cx - 8, cy - 1);
            tail.closePath();
            fillInked(g, tail, hairColor, 1.1);
        } else if (pose == POSE_LEAN_WATCH) {
            GeneralPath mass = new GeneralPath();
            mass.moveTo(cx - 13, cy - 5);
            mass.quadTo(cx - 18, cy + 8, cx - 15, cy + 21);
            mass.lineTo(cx + 15, cy + 21);
            mass.quadTo(cx + 18, cy + 8, cx + 13, cy - 5);
            mass.quadTo(cx, cy - 17, cx - 13, cy - 5);
            mass.closePath();
            fillInked(g, mass, hairColor, 1.1);
        }
    }

    /**
     * Fringe over the forehead. It stops well above the eye line at cy + 1 so
     * it never hides the face.
     */
    void hairCap(Graphics2D g, double cx, double cy, double sweep) {
        GeneralPath h = new GeneralPath();
        h.moveTo(cx - 12, cy - 1);
        h.quadTo(cx - 13, cy - 14, cx, cy - 14);
        h.quadTo(cx + 13, cy - 14, cx + 12, cy - 1);
        h.quadTo(cx + 8, cy - 8, cx + sweep, cy - 6);
        h.quadTo(cx - 6, cy - 4, cx - 12, cy - 1);
        h.closePath();
        fillInked(g, h, hairColor, 1.1);
    }

    /** Stroke a shape fat in the outline colour, then thin in its own. */
    void inkedShape(Graphics2D g, Shape s, Color fill, double w) {
        g.setColor(outlineColor);
        stroke(g, w + 1.3);
        g.draw(s);
        g.setColor(fill);
        stroke(g, w);
        g.draw(s);
    }

    /** Fill a shape and give it an ink outline. */
    void fillInked(Graphics2D g, Shape s, Color fill, double w) {
        g.setColor(fill);
        g.fill(s);
        g.setColor(outlineColor);
        stroke(g, w);
        g.draw(s);
    }

    /** High frequency, small amplitude: nerves. */
    double shake(double t) {
        return Math.sin(t * 2 * Math.PI * 11.0) * 1.6;
    }

    void stroke(Graphics2D g, double w) {
        g.setStroke(new BasicStroke((float) (w + strokeBoost),
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    }

    /** Two segment limb: hip - knee - foot, or shoulder - elbow - hand. */
    void limb(Graphics2D g, double x1, double y1, double x2, double y2, double x3, double y3) {
        GeneralPath p = new GeneralPath();
        p.moveTo(x1, y1);
        p.lineTo(x2, y2);
        p.lineTo(x3, y3);
        g.draw(p);
    }

    /**
     * The close up. droop bends the shoulders and head down, lift brings them
     * back up when the laptop opens.
     */
    void slumpFigure(Graphics2D g, double x, double y, double s, double droop,
            double lift, double t) {
        double headDrop = droop * 9 - lift * 3;
        double breath = Math.sin(t * 2 * Math.PI * 0.5) * 0.6;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.translate(x, y);
        g2.scale(s, s);

        for (int pass = 0; pass < 2; pass++) {
            outlinePass = (pass == 0);
            strokeBoost = outlinePass ? 1.2 : 0;
            part(g2, clothColor);
            g2.setStroke(new BasicStroke((float) (1.2 + strokeBoost)));
            GeneralPath torso = new GeneralPath();
            torso.moveTo(-17, 8);
            torso.lineTo(-13, -20 + droop * 3);
            torso.quadTo(0, -27 + droop * 4, 13, -20 + droop * 3);
            torso.lineTo(17, 8);
            torso.closePath();
            g2.fill(torso);
            g2.draw(torso);
        }
        outlinePass = false;
        strokeBoost = 0;
        g2.dispose();

        // head, in screen space so the midpoint circle stays crisp
        double hy = y + (-40 + headDrop + breath) * s;
        double hr = 13 * s;
        g.setColor(outlineColor);
        Midpoint.fillCircle(g, (int) x, (int) hy, (int) (hr + 0.8 * s));
        g.setColor(skinColor);
        Midpoint.fillCircle(g, (int) x, (int) hy, (int) hr);

        Graphics2D g3 = (Graphics2D) g.create();
        g3.translate(x, hy);
        g3.scale(s, s);
        g3.setColor(hairColor);
        GeneralPath hair = new GeneralPath();
        hair.moveTo(-13, 0);
        hair.quadTo(-14, -15, 0, -15);
        hair.quadTo(14, -15, 13, 0);
        hair.quadTo(8, -8, 0, -6);
        hair.quadTo(-7, -5, -13, 0);
        hair.closePath();
        g3.fill(hair);
        g3.setColor(outlineColor);
        g3.setStroke(new BasicStroke(1.1f));
        g3.draw(hair);

        // flat eyes and a mouth that turns back up when he looks at the screen
        g3.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g3.draw(new Line2D.Double(-7, 2, -3, 2));
        g3.draw(new Line2D.Double(3, 2, 7, 2));
        // positive curve bows the mouth upward on the page, which reads as a
        // downturned mouth; lift flips it back into a small smile
        double curve = 5 - lift * 9;
        g3.draw(new QuadCurve2D.Double(-4, 8, 0, 8 - curve * 0.6, 4, 8));
        g3.dispose();
    }
}
