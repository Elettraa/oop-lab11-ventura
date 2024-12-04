package it.unibo.oop.reactivegui03;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.lang.reflect.InvocationTargetException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * Third experiment with reactive gui.
 */
@SuppressWarnings("PMD.AvoidPrintStackTrace")
public final class AnotherConcurrentGUI extends JFrame {

    private static final long serialVersionUID = 1L;
    private static final double WIDTH_PERC = 0.2;
    private static final double HEIGHT_PERC = 0.1;
    private static final long TIME_IN_MILLIS = 10_000;
    private final JLabel display = new JLabel();
    private final JButton up = new JButton("up");
    private final JButton down = new JButton("down");
    private final JButton stop = new JButton("stop");

    /**
     * Builds a new CGUI.
     */
    public AnotherConcurrentGUI() {
        super();
        final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        this.setSize((int) (screenSize.getWidth() * WIDTH_PERC), (int) (screenSize.getHeight() * HEIGHT_PERC));
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        final JPanel panel = new JPanel();
        this.getContentPane().add(panel);
        panel.add(display);
        panel.add(down);
        panel.add(up);
        panel.add(stop);
        this.setVisible(true);

        final Agent agent = new Agent();
        final StopAgent agent2 = new StopAgent(agent);
        new Thread(agent).start();
        new Thread(agent2).start();

        stop.addActionListener((e) -> agent.stopCounting());
        up.addActionListener((e) -> agent.countUpwards());
        down.addActionListener((e) -> agent.countDownwards());

    }

    /*
     * The counter agent is implemented as a nested class. This makes it
     * invisible outside and encapsulated.
     */
    private final class Agent implements Runnable {
        /*
         * Stop is volatile to ensure visibility. Look at:
         * 
         * http://archive.is/9PU5N - Sections 17.3 and 17.4
         * 
         * For more details on how to use volatile:
         * 
         * http://archive.is/4lsKW
         * 
         */
        private volatile boolean stop;
        private volatile boolean countingUp = true;
        private int counter;

        @Override
        public void run() {
            while (!this.stop) {
                try {
                    // The EDT doesn't access `counter` anymore, it doesn't need to be volatile
                    final var nextText = Integer.toString(this.counter);
                    SwingUtilities.invokeAndWait(() -> AnotherConcurrentGUI.this.display.setText(nextText));
                    if (countingUp) {
                        this.counter++;
                    } else {
                        this.counter--;
                    }
                    Thread.sleep(100);
                } catch (InvocationTargetException | InterruptedException ex) {
                    /*
                     * This is just a stack trace print, in a real program there
                     * should be some logging and decent error reporting
                     */
                    ex.printStackTrace();
                }
            }
        }

        /**
         * External command to stop counting.
         */
        public void stopCounting() {
            this.stop = true;
        }

        /**
         * External command to count upwards.
         */
        public void countUpwards() {
            this.countingUp = true;
        }

        /**
         * External command to count downwards.
         */
        public void countDownwards() {
            this.countingUp = false;
        }
    }

    private final class StopAgent implements Runnable {
        private final Agent agent;

        StopAgent(final Agent agent) {
            this.agent = agent;
        }

        @Override
        public void run() {
            try {
                Thread.sleep(TIME_IN_MILLIS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            agent.stopCounting();
            SwingUtilities.invokeLater(() -> {
                stop.setEnabled(false);
                up.setEnabled(false);
                down.setEnabled(false);
            });
        }
    }
}
