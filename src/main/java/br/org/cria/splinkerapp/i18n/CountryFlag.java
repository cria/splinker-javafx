package br.org.cria.splinkerapp.i18n;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;

/** Small vector flags, rendered consistently without depending on emoji fonts. */
public final class CountryFlag extends Canvas {
    public CountryFlag(Language language) {
        super(28, 18);
        GraphicsContext g = getGraphicsContext2D();
        switch (language) {
            case PORTUGUESE -> {
                g.setFill(Color.web("#009739"));
                g.fillRect(0, 0, 28, 18);
                g.setFill(Color.web("#FFDF00"));
                g.fillPolygon(new double[]{14, 25, 14, 3}, new double[]{2, 9, 16, 9}, 4);
                g.setFill(Color.web("#002776"));
                g.fillOval(9, 4, 10, 10);
                g.setStroke(Color.WHITE);
                g.setLineWidth(1.3);
                g.strokeArc(8, 7, 13, 7, 25, 120, ArcType.OPEN);
                g.setFill(Color.WHITE);
                g.fillOval(12, 10, 1, 1);
                g.fillOval(15, 11, 1, 1);
                g.fillOval(16, 8, .8, .8);
            }
            case SPANISH -> {
                g.setFill(Color.web("#AA151B"));
                g.fillRect(0, 0, 28, 18);
                g.setFill(Color.web("#F1BF00"));
                g.fillRect(0, 4.5, 28, 9);
                // Simplified coat of arms at icon scale.
                g.setFill(Color.web("#AA151B"));
                g.fillRoundRect(7, 7, 4, 5, 2, 2);
                g.setFill(Color.web("#F1BF00"));
                g.fillRect(8, 7, 1, 2);
                g.setStroke(Color.web("#AA151B"));
                g.setLineWidth(1);
                g.strokeLine(6, 6, 12, 6);
            }
            case ENGLISH -> {
                g.setFill(Color.WHITE);
                g.fillRect(0, 0, 28, 18);
                g.setFill(Color.web("#B22234"));
                for (int stripe = 0; stripe < 13; stripe += 2) {
                    g.fillRect(0, stripe * 18.0 / 13, 28, 18.0 / 13);
                }
                g.setFill(Color.web("#3C3B6E"));
                g.fillRect(0, 0, 12, 18.0 * 7 / 13);
                g.setFill(Color.WHITE);
                for (int row = 0; row < 9; row++) {
                    for (int col = 0; col < (row % 2 == 0 ? 6 : 5); col++) {
                        g.fillOval(1 + col * 1.8 + (row % 2) * .9, 1 + row * .9, .65, .65);
                    }
                }
            }
        }
        g.setStroke(Color.rgb(0, 0, 0, .2));
        g.setLineWidth(1);
        g.strokeRect(.5, .5, 27, 17);
    }
}
