import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

/**
 * The film script: six shots on one timeline.
 *
 * Each shot is handed a time that already starts at 0, so nothing in here
 * has to know where it sits on the 15 second clock. render() is the only
 * way in; it picks the shot and gives every one its own Graphics so a
 * translate or scale can never leak into the next shot.
 */
final class Shots {

    static final double DURATION = 15.0;

    /** Start time of each shot, used by the 1-6 preview keys. */
    static final double[] SHOT_START = { 0.0, 2.5, 5.0, 7.0, 9.0, 12.0 };

    /** Everyone in the film is drawn through this one object. */
    final Figure fig = new Figure();

    /** Draws whichever shot owns time t. */
    void render(Graphics2D g, double t) {
        Graphics2D gs = (Graphics2D) g.create();
        if (t < 2.5) {
            shot1(gs, t);
        } else if (t < 5.0) {
            shot2(gs, t - 2.5);
        } else if (t < 7.0) {
            shot3(gs, t - 5.0);
        } else if (t < 9.0) {
            shot4(gs, t - 7.0);
        } else if (t < 12.0) {
            shot5(gs, t - 9.0);
        } else {
            shot6(gs, t - 12.0);
        }
        gs.dispose();
    }

    void shot1(Graphics2D g, double t) {
        // ta drives everything that moves; it freezes at 2.0 s so the three
        // hold still for the last half second.
        double ta = Math.min(t, 2.0);
        // flash only drives the screen and the light, so the frozen half
        // second still has the result glowing brighter.
        double flash = Art.ease(Art.clamp((t - 2.0) / 0.15, 0, 1));

        fig.outlineColor = Art.S1_INK;
        fig.skinColor = Art.S1_SKIN;
        fig.hairColor = Art.S1_HAIR;
        fig.shirtColor = Art.S1_SHIRT;
        fig.litColor = Art.S1_GLOW;

        Scene.drawRoomScene(g, ta);

        // friends first, then the one in the middle so he sits in front
        fig.clothColor = Art.S1_CARDI;
        fig.drawStudent(g, 118, 392, 3.3, Figure.POSE_LEAN_LAUGH, ta);
        fig.clothColor = Art.S1_NAVY;
        fig.drawStudent(g, 486, 398, -3.3, Figure.POSE_LEAN_WATCH, ta);
        fig.clothColor = Art.S1_NAVY;
        fig.drawStudent(g, 286, 470, 3.4, Figure.POSE_COVER_EYES, ta);

        // the table covers every lap, so it goes on last
        Scene.drawBigTable(g);
        Scene.drawTableHands(g, fig.skinColor, fig.outlineColor);
        Scene.drawTableProps(g);
        Scene.drawTablet(g, flash);
        Scene.drawScreenGlow(g, flash);
        Scene.drawSparks(g, t);

        // --- opening fade in from white --------------------------------
        double fade = 1.0 - Art.ease(Art.clamp(t / 0.3, 0, 1));
        if (fade > 0) {
            g.setColor(new Color(255, 255, 255, (int) (fade * 255)));
            g.fillRect(0, 0, Art.W, Art.H);
        }
    }

    void shot2(Graphics2D g, double t) {
        fig.outlineColor = Art.S1_INK;
        fig.skinColor = Art.S1_SKIN;
        fig.hairColor = Art.S1_HAIR;
        fig.shirtColor = Art.S1_SHIRT;
        fig.litColor = Art.S1_GLOW;

        Scene.drawRoomScene(g, 2.0);

        // the colour cut: one frame, no fade
        Scene.wash(g, Art.S2_BG, 0.34f);

        // everyone is airborne, settling over the shot
        double j1 = Scene.jump(t, 0.0);
        double j2 = Scene.jump(t, 0.12);
        double j3 = Scene.jump(t, 0.06);

        fig.wearsJacket = false;
        fig.clothColor = Art.S1_CARDI;
        fig.drawStudent(g, 118, 392 - j1, 3.3, Figure.POSE_CHEER, t);
        fig.clothColor = Art.S1_NAVY;
        fig.drawStudent(g, 486, 398 - j2, -3.3, Figure.POSE_CHEER, t);
        fig.wearsJacket = true;
        fig.clothColor = Art.S1_NAVY;
        fig.drawStudent(g, 286, 470 - j3, 3.4, Figure.POSE_CHEER, t);
        fig.wearsJacket = false;

        Scene.joyLines(g, 190, 250, t);
        Scene.joyLines(g, 320, 300, t + 0.2);
        Scene.joyLines(g, 430, 258, t + 0.4);

        Scene.drawBigTable(g);
        Scene.drawTableProps(g);
        // the tablet was let go of, and drops the last few pixels
        Graphics2D gt = (Graphics2D) g.create();
        gt.translate(0, -14 * Math.max(0, 1 - t / 0.35));
        Scene.drawTablet(gt, 1.0);
        gt.dispose();

        Scene.confetti(g, t);
        Scene.wash(g, Art.rgb("#FFD97D"), 0.12f);
    }

    void shot3(Graphics2D g, double t) {
        fig.outlineColor = Art.S1_INK;
        double zoom = Art.ease(Art.clamp(t / 1.1, 0, 1));
        double s = 1.0 + 3.5 * zoom;

        // push in on the tablet, easing out as it fills the frame
        Graphics2D gz = (Graphics2D) g.create();
        gz.translate(322, 505);
        gz.scale(s, s);
        gz.translate(-322, -505);
        Scene.drawRoomScene(gz, 2.0);
        Scene.drawBigTable(gz);
        Scene.drawTableProps(gz);
        Scene.drawTablet(gz, 1.0);
        gz.dispose();

        Scene.wash(g, Art.S2_BG, (float) (0.3 * (1 - zoom)));

        // the result page takes over as the push in finishes
        double page = Art.ease(Art.clamp((t - 0.7) / 0.5, 0, 1));
        if (page > 0) {
            Graphics2D gp = (Graphics2D) g.create();
            gp.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, (float) page));
            Hall.drawResultPage(gp, t);
            gp.dispose();
        }

        // fade to white on the cut
        double out = Art.ease(Art.clamp((t - 1.7) / 0.3, 0, 1));
        if (out > 0) {
            g.setColor(new Color(255, 255, 255, (int) (out * 255)));
            g.fillRect(0, 0, Art.W, Art.H);
        }
    }

    void shot4(Graphics2D g, double t) {
        g.setColor(Art.S4_BG);
        g.fillRect(0, 0, Art.W, Art.H);

        // stage at the front of the hall
        g.setColor(Art.rgb("#C9C3B4"));
        g.fillRect(0, 236, Art.W, 74);
        g.setColor(Art.lerp(Art.S4_BG, Art.S1_INK, 0.9));
        g.setStroke(new BasicStroke(2.5f));
        g.draw(new Line2D.Double(0, 236, Art.W, 236));
        g.draw(new Line2D.Double(0, 310, Art.W, 310));

        // blank projector screen
        g.setColor(Art.rgb("#F7F4EC"));
        g.fill(new Rectangle2D.Double(176, 74, 248, 150));
        g.setColor(Art.lerp(Art.S4_BG, Art.S1_INK, 0.9));
        g.setStroke(new BasicStroke(3.5f));
        g.draw(new Rectangle2D.Double(176, 74, 248, 150));

        Hall.spotlight(g, 236, 300);
        Hall.spotlight(g, 364, 300);

        // rows of seat backs and heads, each row nearer and larger
        for (int row = 0; row < 4; row++) {
            double y = 356 + row * 66;
            double hr = 15 + row * 5;
            double gap = 96 + row * 22;
            Hall.audienceRow(g, y, hr, gap, row);
        }

        // fade in from the white that ended shot 3
        double in = 1 - Art.ease(Art.clamp(t / 0.25, 0, 1));
        if (in > 0) {
            g.setColor(new Color(255, 255, 255, (int) (in * 255)));
            g.fillRect(0, 0, Art.W, Art.H);
        }
    }

    void shot5(Graphics2D g, double t) {
        // the whole frame shakes when the words land
        double quake = Art.clamp((t - 1.6) / 0.3, 0, 1);
        double qx = 0;
        double qy = 0;
        if (quake > 0 && quake < 1) {
            double k = (1 - quake) * 5;
            qx = Math.sin(t * 2 * Math.PI * 26) * k;
            qy = Math.cos(t * 2 * Math.PI * 31) * k;
        }
        Graphics2D gs = (Graphics2D) g.create();
        gs.translate(qx, qy);

        // camera pans across to the stage over the first 0.8 s
        double pan = 170 * (1 - Art.ease(Art.clamp(t / 0.8, 0, 1)));
        gs.setColor(Art.S5_BG);
        gs.fillRect(-20, -20, Art.W + 40, Art.H + 40);

        Graphics2D gp = (Graphics2D) gs.create();
        gp.translate(pan, 0);
        Hall.drawStage(gp, t);
        gp.dispose();

        Hall.speechBubble(gs, t);
        gs.dispose();
    }

    void shot6(Graphics2D g, double t) {
        double lift = Art.ease(Art.clamp((t - 1.8) / 0.5, 0, 1)); // 13.8 s: head comes up
        double grey = Art.ease(Art.clamp(t / 1.2, 0, 1)) * (1 - lift);

        Color warm = Art.rgb("#C9C3B4");
        g.setColor(Art.lerp(warm, Art.S6_BG, grey));
        g.fillRect(0, 0, Art.W, Art.H);
        g.setColor(Art.lerp(Art.lerp(warm, Art.S1_INK, 0.2), Art.rgb("#4A4E57"), grey));
        g.fillRect(0, 430, Art.W, Art.H - 430);

        Hall.rainCloud(g, t, 1 - lift);

        // the student, close up, shoulders dropping then lifting again
        double droop = Art.ease(Art.clamp(t / 1.5, 0, 1)) * (1 - lift);
        fig.outlineColor = Art.lerp(Art.rgb("#5A4A40"), Art.rgb("#3A3D44"), grey);
        fig.skinColor = Art.desat(Art.S1_SKIN, grey * 0.8);
        fig.hairColor = Art.desat(Art.S1_HAIR, grey * 0.6);
        fig.shirtColor = Art.desat(Color.WHITE, grey * 0.5);
        fig.clothColor = Art.desat(Art.S1_NAVY, grey * 0.5);
        fig.litColor = Art.S1_GLOW;
        fig.slumpFigure(g, 300, 620 + droop * 26, 7.4, droop, lift, t);

        if (lift > 0) {
            Hall.laptopGlow(g, lift);
        }

        // 14.3 s: fade to black, then the title
        double out = Art.ease(Art.clamp((t - 2.3) / 0.4, 0, 1));
        if (out > 0) {
            g.setColor(new Color(0, 0, 0, (int) (out * 255)));
            g.fillRect(0, 0, Art.W, Art.H);
            double title = Art.ease(Art.clamp((t - 2.4) / 0.3, 0, 1));
            if (title > 0) {
                g.setColor(new Color(255, 255, 255, (int) (title * 255)));
                g.setFont(new Font("Tahoma", Font.BOLD, 46));
                Hall.centred(g, "MY MEMORIES", 312);
            }
        }
    }

}
