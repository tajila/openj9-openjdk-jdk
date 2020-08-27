/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/* @test
   @bug 8250935
   @summary Verifies Date info is placed under correct field
   @run main/manual JFileChooserHomeDetailsInfo
 */

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class JFileChooserHomeDetailsInfo {
    private static final CountDownLatch testEndedSignal = new CountDownLatch(1);
    private static final int testTimeout = 300000;
    private static volatile String testFailureMsg;
    private static volatile boolean testPassed;
    private static volatile boolean testFinished;

    public static void main(String[] args) throws Exception {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                 | UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }
        SwingUtilities.invokeLater(() -> createAndShowTestDialog());

        try {
            if (!testEndedSignal.await(testTimeout, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException(String.format(
                    "Test timeout '%d ms' elapsed.", testTimeout));
            }
            if (!testPassed) {
                String failureMsg = testFailureMsg;
                if ((failureMsg != null) && (!failureMsg.trim().isEmpty())) {
                    throw new RuntimeException(failureMsg);
                } else {
                    throw new RuntimeException("Test failed.");
                }
            }
        } catch (InterruptedException ie) {
            throw new RuntimeException(ie);
        } finally {
            testFinished = true;
        }
    }

    private static void doTest() throws Exception {
        JFileChooser fChooser = new JFileChooser();

        fChooser.setDialogTitle("Please select folder...");
        fChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES );

        fChooser.showOpenDialog(new JFrame());
    }

    private static void pass() {
        testPassed = true;
        testEndedSignal.countDown();
    }

    private static void fail(String failureMsg) {
        testFailureMsg = failureMsg;
        testPassed = false;
        testEndedSignal.countDown();
    }

    private static String convertMillisToTimeStr(int millis) {
        if (millis < 0) {
            return "00:00:00";
        }
        int hours = millis / 3600000;
        int minutes = (millis - hours * 3600000) / 60000;
        int seconds = (millis - hours * 3600000 - minutes * 60000) / 1000;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    private static void createAndShowTestDialog() {
        String description =
            " 1. Click on \"Start Test\" button.\r\n" +
            " 2. A JFileChooser file dialog will be shown. \r\n" +
            " 3. Click on \"Details\" button to show file details.\r\n" +
            " 4. Click on \"Home\" button on file dialog.\r\n" +
            "\r\n" +
            " If the \"Modified\" date time info is appearing in \"Type\" field for user directory\r\n"+
            " cancel the dialog and click on \"FAIL\" button, else click on \"PASS\" button.";

        final JDialog dialog = new JDialog();
        dialog.setTitle("PropertiesButtonPrintDialog");
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dialog.dispose();
                fail("Main dialog was closed.");
            }
        });

        final JLabel testTimeoutLabel = new JLabel(String.format(
            "Test timeout: %s", convertMillisToTimeStr(testTimeout)));
        final long startTime = System.currentTimeMillis();
        final Timer timer = new Timer(0, null);
        timer.setDelay(1000);
        timer.addActionListener((e) -> {
            int leftTime = testTimeout - (int) (System.currentTimeMillis() - startTime);
            if ((leftTime < 0) || testFinished) {
                timer.stop();
                dialog.dispose();
            }
            testTimeoutLabel.setText(String.format(
                "Test timeout: %s", convertMillisToTimeStr(leftTime)));
        });
        timer.start();

        JTextArea textArea = new JTextArea(description);
        textArea.setEditable(false);

        final JButton testButton = new JButton("Start Test");
        final JButton passButton = new JButton("PASS");
        final JButton failButton = new JButton("FAIL");
        testButton.addActionListener((e) -> {
            testButton.setEnabled(false);
            new Thread(() -> {
                try {
                    doTest();

                    SwingUtilities.invokeLater(() -> {
                        passButton.setEnabled(true);
                        failButton.setEnabled(true);
                    });
                } catch (Throwable t) {
                    t.printStackTrace();
                    dialog.dispose();
                    fail("Exception occurred in a thread executing the test.");
                }
            }).start();
        });
        passButton.setEnabled(false);
        passButton.addActionListener((e) -> {
            dialog.dispose();
            pass();
        });
        failButton.setEnabled(false);
        failButton.addActionListener((e) -> {
            dialog.dispose();
            fail(" Date info is placed in the Type column");
        });

        JPanel mainPanel = new JPanel(new BorderLayout());
        JPanel labelPanel = new JPanel(new FlowLayout());
        labelPanel.add(testTimeoutLabel);
        mainPanel.add(labelPanel, BorderLayout.NORTH);
        mainPanel.add(textArea, BorderLayout.CENTER);
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(testButton);
        buttonPanel.add(passButton);
        buttonPanel.add(failButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        dialog.add(mainPanel);

        dialog.pack();
        dialog.setVisible(true);
    }

}
