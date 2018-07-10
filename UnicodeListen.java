/*
 * Copyright (c) 1998-2002 TIBCO Software Inc.
 * All rights reserved.
 * TIBCO Rendezvous is protected under US Patent No. 5,187,787.
 * For more information, please contact:
 * TIBCO Software Inc., Palo Alto, California, USA
 *
 * @(#)UnicodeListen.java	1.3
 */

/*
 * UnicodeListen.java
 *
 * This program demonstrates how to process messages with exotic character data
 * (in this case, encoded in UTF-8). It receives a message of UTF-8 encoded data
 * from another program (UnicodeSend.java) and then prints the byte values of the
 * data.
 *
 * Optionally the user may specify communication parameters for
 * tibrvTransport_Create.  If none are specified, default values
 * are used.  For information on default values for these parameters,
 * please see the TIBCO/Rendezvous Concepts manual.
 *
 * The user may specify the subject for the listener.  If not specified the
 * default subject is "test.unicode".
 *
 *
 * ** OPTIONAL: **
 * This program can also display the exotic characters in JFrame by including the
 * code for the GUI part (only *Windows* platform is supported in this version).
 * To display the exotic characters in JFrame, do the followings:
 *   1. Use JDK1.2 or higher
 *   2. Install a Chinese font and update the variable 'fontName' to its
 *      corresponding value.
 *   3. Uncomment all lines ended with    "// JFrame version"
 *   4. Comment all lines ended with      "// Console version"
 *
 * To test:
 *   1. Run: java UnicodeListen -subject test
 *   2. Run: java UnicodeSend -subject test -message "Hello, \u60A8\u597D."
 *
 */

import java.io.*;
import com.tibco.tibrv.*;

// import java.awt.*;                                                       // JFrame version
// import java.awt.event.*;                                                 // JFrame version
// import javax.swing.*;                                                    // JFrame version

public class UnicodeListen implements TibrvMsgCallback {                    // Console version
// public class UnicodeListen extends JFrame implements TibrvMsgCallback {  // JFrame version
    // JTextArea       textArea  = new JTextArea(10,50);                    // JFrame version

    String          service   = null;
    String          network   = null;
    String          daemon    = null;
    String          subject   = "test.unicode";
    String          fieldName = "CHINESE_TIB";
    String          encoding  = "UTF-8";
    TibrvTransport  tport;

    String          fontName  = "MingLiU"; // font to display Chinese character
    String          C_for     = "\u7B2C";  // Chinese character 'For ... round'
    String          C_round   = "\u56DE";  // Chinese character 'round'
    String          messageString;

    int             count     = 0;


    public UnicodeListen(String[] args) {

        /*
        super("UnicodeListen.java");                                        // JFrame version

        // GUI setup
        GraphicsEnvironment.getLocalGraphicsEnvironment();                  // JFrame version
        Font font = new Font(fontName, Font.PLAIN, 15);                     // JFrame version
        textArea.setFont(font);                                             // JFrame version

        JPanel jPanel = new JPanel();                                       // JFrame version
        jPanel.setLayout(new BoxLayout(jPanel, BoxLayout.Y_AXIS));          // JFrame version
        jPanel.add(textArea);                                               // JFrame version
        jPanel.add(Box.createRigidArea(new Dimension(0, 10)));              // JFrame version

        getContentPane().add(jPanel, BorderLayout.CENTER);                  // JFrame version
        */

        // parse arguments for possible optional parameters.
        int i = get_InitParams(args);

        // open Tibrv in native implementation
        try {
            Tibrv.open(Tibrv.IMPL_NATIVE);
        }
        catch (TibrvException e) {
            System.err.println("Failed to open Tibrv in native implementation:");
            e.printStackTrace();
            System.exit(0);
        }

        // Set character encoding to decode the data properly.
        try {
            TibrvMsg.setStringEncoding(encoding);
        }
        catch (UnsupportedEncodingException e) {
            System.err.println("Failed to set encoding:");
            e.printStackTrace();
            System.exit(0);
        }

        try {
            // Create a transport for communication with the daemon.
            tport = new TibrvRvdTransport(service, network, daemon);

            // Create a queue for events.
            TibrvQueue queue = new TibrvQueue();

            // Create a listener for our subject.
            TibrvListener listener =
                new TibrvListener(queue, this, tport, subject, null);

            System.err.println("Listening to [" + subject + "]...");

            // Pass the queue to a dispatcher thread.
            TibrvDispatcher disp = new TibrvDispatcher(queue);

        }
        catch (TibrvException e) {
            System.err.println("Error - TibrvException caught:");
            e.printStackTrace();
            System.exit(0);
        }
    }


    public static void main(String[] args) {

        System.err.println("\nUnicodeListen.java (TIBCO/Rendezvous " +
                           Tibrv.getVersion() + ")\n");

        /*
        JFrame frame = new UnicodeListen(args);                             // JFrame version

        frame.setSize(500, 500);                                            // JFrame version
        frame.pack();                                                       // JFrame version
        frame.setVisible(true);                                             // JFrame version

        frame.addWindowListener(new WindowAdapter() {                       // JFrame version
            public void windowClosing(WindowEvent e) {                      // JFrame version
                try {                                                       // JFrame version
                    Tibrv.close();                                          // JFrame version
                }                                                           // JFrame version
                catch (TibrvException rve) { rve.printStackTrace(); }       // JFrame version
                System.exit(0);                                             // JFrame version
            }                                                               // JFrame version
        });                                                                 // JFrame version
        */

        new UnicodeListen(args);                                            // Console version
    }


    // This is the listener callback function, invoked when the dispatcher thread
    // dispatches an event from the queue.
    public void onMsg(TibrvListener listener, TibrvMsg msg) {
        byte[] b;

        count++;

        System.out.println("\nReceived message["+count+"]:");

        try {
            // Print to console to verify byte values.  This displays the hexadecimal
            // values of each byte of the result of tibrvMsg.toString(), which has
            // the format:
            //   { CHINESE_TIB="<fieldvalue>"[ USER_DATA="<fieldvalue>"] }
            // If displayed on a system whose font does not include the characters
            // in the message, the result looks like:
            //   { CHINESE_TIB="????"[ USER_DATA="<fieldvalue>"] }
            messageString = msg.toString();
            b             = messageString.getBytes(encoding);

            System.out.println(messageString);
            for (int i = 0; i < b.length; i++)
                System.out.println("b[" + i + "]: " +
                                   Integer.toHexString(b[i]).toUpperCase());

            // repaint on the Frame
            // repaint();                                                   // JFrame version
        }
        catch (UnsupportedEncodingException e) {
            System.err.println("Error - Unsupported Encoding:");
            e.printStackTrace();
            System.exit(0);
        }

    }

    /*
    public void paint(Graphics g) {                                         // JFrame version
        textArea.setText(C_for + " " + count + " " +                        // JFrame version
                         C_round + ": " + messageString);                   // JFrame version
    }                                                                       // JFrame version
    */


    // Parse command line parameters.
    public int get_InitParams(String[] args)
    {
        int i=0;
        if (args.length > 0) {
            if (args[i].equals("-?") ||
                args[i].equals("-h") ||
                args[i].equals("-help")) {
                usage();
            }
        }

        while(i < args.length-1 && args[i].startsWith("-")) {
            if (args[i].equals("-service")) {
                service = args[i+1];
                i += 2;
            }
            else
            if (args[i].equals("-network")) {
                network = args[i+1];
                i += 2;
            }
            else
            if (args[i].equals("-daemon")) {
                daemon = args[i+1];
                i += 2;
            }
            else
            if (args[i].equals("-subject")) {
                subject = args[i+1];
                i += 2;
            }
            else
                usage();
        }
        return i;
    }

    // Print usage information and quit
    public void usage()
    {
        System.err.println("Usage: java UnicodeListen [-service service] [-network network]");
        System.err.println("            [-daemon daemon] [-subject subject]");
        System.exit(-1);
    }

}
