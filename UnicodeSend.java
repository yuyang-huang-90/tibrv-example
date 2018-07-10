/*
 * UnicodeSend.java
 *
 * This program demonstrates how to set encoding in TIBCO/Rendezvous in order
 * to publish a message with exotic characters data.
 *
 * It creates a TibrvMsg object with several Chinese characters encoded
 * in UTF-8 and publishes it. After publishing the message, it prints
 * the byte values of the data.
 *
 * User may add a separate field in the message with the '-message' option
 * in which exotic characters may be added by using the proper escape sequence.
 * Eg. -message "Hello, \u60A8\u597D." The two exotic characters referenced
 * by the escape sequence mean "How are you?" in Chinese.  The value specified
 * is added to the message in a field named "USER_DATA".
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
 * To test:
 *   1. Run: java UnicodeListen -subject test
 *   2. Run: java UnicodeSend -subject test -message "Hello, \u60A8\u597D."
 *
 * @(#)UnicodeSend.java	1.3
 */

import java.io.*;
import java.lang.*;
import java.util.*;

import com.tibco.tibrv.*;

public class UnicodeSend
{
    final int MAX_MESSAGE_LENGTH = 256;
    final int _RADIX             = 16;

    String ChineseTIB = "\u8CC7\u8A0A\u5DF4\u58EB";  // Chinese translation of
                                                     // 'The Information Bus'

    String encoding   = "UTF-8";                     // Unicode Transformation Format
                                                     // eight-bit encoding
    String            service   = null;
    String            network   = null;
    String            daemon    = null;
    String            subject   = "test.unicode";
    String            fieldName = "CHINESE_TIB";
    String            userField = "USER_DATA";
    String            message   = null;
    TibrvMsg          msg;
    TibrvTransport    tport;


    public static void main(String[] args)
    {
        System.err.println("\nUnicodeSend.java v1.0 (TIBCO/Rendezvous " +
                           Tibrv.getVersion() + ")\n");

        new UnicodeSend(args);
    }


    public UnicodeSend(String[] args)
    {
        // parse arguments for possible optional
        // parameters. These must precede the subject
        // and message strings
        int i = get_InitParams(args);

        // open Tibrv in native implementation
        try {
            Tibrv.open(Tibrv.IMPL_NATIVE);
        }
        catch (TibrvException e)
        {
            System.err.println("Failed to open Tibrv in native implementation:");
            e.printStackTrace();
            System.exit(0);
        }

        // Create RVD transport
        try {
            tport = new TibrvRvdTransport(service,network,daemon);
        }
        catch (TibrvException e)
        {
            System.err.println("Failed to create TibrvRvdTransport:");
            e.printStackTrace();
            System.exit(0);
        }

        // set encoding before initializing TibrvMsg
           try {
            TibrvMsg.setStringEncoding(encoding);
        }
        catch (UnsupportedEncodingException e) {
            System.err.println("Failed to set encoding:");
            e.printStackTrace();
            System.exit(0);
        }

        // Now construct a TibrvMsg object
        msg = new TibrvMsg();

        try {
            // set sendsubject
            msg.setSendSubject(subject);

            // simply add the chinese characters into the field
            msg.add(fieldName, ChineseTIB);

            // add user message
            if (message != null) {
                msg.add(userField, parseString(message));
            }

            System.out.println("Sending message with UTF-8 encoded data ...\n");
            tport.send(msg);

            // Print the byte values to verify. The encoded byte values
            // should be E8B387 E8A88A E5B7B4 E5A3AB which match the
            // Unicode characters given for UTF-8.
            String dataString = (String) msg.get(fieldName);
            byte[] b          = dataString.getBytes(encoding);

            System.out.println("Byte values of field ["+fieldName+"]:");
            for (int j = 0; j < b.length; j++) {
            System.out.println("\tb[" + j + "]: " +
                               Integer.toHexString(b[j]).toUpperCase());
            }

            // If the user specified a message value, add it to the message.
            if (message != null) {
                String userString = (String) msg.get(userField);
                byte[] ba         = userString.getBytes(encoding);

                System.out.println("Byte values of field ["+userField+"]:");
                for (int j = 0; j < ba.length; j++) {
                    System.out.println("\tba[" + j + "]: " +
                                       Integer.toHexString(ba[j]).toUpperCase());
                }
            }
        }
        catch (TibrvException e) {
            System.err.println("Error - TibrvException caught:");
            e.printStackTrace();
        }
        catch (UnsupportedEncodingException e) {
            System.err.println("Error - Unsupported Encoding:");
            e.printStackTrace();
        }
        finally {
            try {
                Tibrv.close();
            }
            catch (TibrvException e) {
                System.err.println("Error in closing Tibrv:");
                e.printStackTrace();
            }
            System.exit(0);
        }
    }

    // In Java, Unicode characters are referenced by the escape sequence "\\u"
    // followed by the hexadecimal value of the character. However, if the
    // Unicode value is passed from the program command line, use this method
    // to transform the string to its proper byte sequence.
    public String parseString(String message)
    {
        int i         = 0;
        int chrLength = 0;

        char[] cIn  = message.toCharArray();        // input character array
        char[] cOut = new char[MAX_MESSAGE_LENGTH]; // output character array

        while ( i < cIn.length ) {

            if ( cIn[i] == '\\' ) {                 // escape character detected
                i++;

                if ( cIn[i] == 'u' ) {
                    i++;

                    // the next four characters represent a Unicode value
                    char[] c        = { cIn[i], cIn[++i], cIn[++i], cIn[++i] };
                    String s        = new String(c);

                    // check if the entered value is a valid hex string
                    try {
                        Integer.valueOf(s, _RADIX);
                    }
                    catch (NumberFormatException e) {
                        System.err.println("Error - Invalid Hex value: "+s);
                        System.exit(0);
                    }

                    // convert to a character with the specified hex value
                    cOut[chrLength] = (char) Integer.parseInt(s, _RADIX);

                    // check if the character is in a defined Unicode block
                    if (Character.UnicodeBlock.of(cOut[chrLength]) == null) {
                        System.err.println("Error - No defined Unicode block for Hex value: "+s);
                        System.exit(0);
                    }

                    chrLength++;
                }
                else {
                    System.err.println("Error - Invalid escape character: " + cIn[i]);
                    System.exit(0);
                }
            }
            else {                                  // regular ASCII characters
                cOut[chrLength] = cIn[i];
                chrLength++;
            }

            i++;
        }

        return ( new String(cOut,0,chrLength) );
    }


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
            if (args[i].equals("-message")) {
                message = args[i+1];
                i += 2;
                if (message.length() > MAX_MESSAGE_LENGTH) {
                    System.err.println("Error - Message exceeds maximum length of "+
                                       MAX_MESSAGE_LENGTH+" characters!");
                    System.exit(0);
                }
            }
            else
                usage();
        }
        return i;
    }

    // Print usage information and quit
    public void usage()
    {
        System.err.println("Usage: java UnicodeSend [-service service] [-network network]");
        System.err.println("            [-daemon daemon] [-subject subject] [-message message]");
        System.exit(-1);
    }

}
