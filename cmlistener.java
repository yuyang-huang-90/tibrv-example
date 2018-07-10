
/*
 * Copyright (c) 1998-2002 TIBCO Software Inc.
 * All rights reserved.
 * TIB/Rendezvous is protected under US Patent No. 5,187,787.
 * For more information, please contact:
 * TIBCO Software Inc., Palo Alto, California, USA
 *
 * @(#)cmlistener.java	1.5
 */


/*
 * cmlistener - listens for certified messages and confirms them
 *
 * There are no required parameters for this example.
 * Optional parameters are:
 *
 * -service   - RVD transport parameter
 * -network   - RVD transport parameter
 * -daemon    - RVD transport parameter
 * -cmname    - CM name used by CM transport
 * -subject   - subject this example listens on
 *
 * If no transport parameters are specified, default values are used.
 * For information on default values for these parameters,  please see
 * the TIBCO/Rendezvous Concepts manual.
 *
 * Default values for other parameters:
 *  cmname      "cm.listener.cmname"
 *  subject     "cm.test.subject"
 *
 */

import java.util.*;
import java.io.*;
import com.tibco.tibrv.*;

public class cmlistener implements TibrvMsgCallback
{
    // RVD transport parameters
    String service = null;
    String network = null;
    String daemon  = null;

    // Subject we use to listen messages on
    String subject = "cm.test.subject";

    // Our unique CM name
    String cmname  = "cm.listener.cmname";

    TibrvQueue        queue        = null;
    TibrvRvdTransport rvdTransport = null;
    TibrvCmTransport  cmTransport  = null;
    TibrvCmListener   cmListener   = null;

    //---------------------------------------------------------------
    // cmlistener
    //---------------------------------------------------------------

    public cmlistener(String args[])
    {
        // Parse parameters
        parseParams(args);

        // open Tibrv in native implementation
        try
        {
            Tibrv.open(Tibrv.IMPL_NATIVE);
        }
        catch (TibrvException e)
        {
            System.out.println("Failed to open Tibrv in native implementation:");
            e.printStackTrace();
            System.exit(0);
        }

        // Create event queue, transports and listener

        try
        {
            // Our event queue
            queue = new TibrvQueue();

            // Create RVD transport and then CM transport
            rvdTransport = new TibrvRvdTransport(service,network,daemon);
            cmTransport  = new TibrvCmTransport(rvdTransport,cmname,true);

            // Create listener for CM messages
            cmListener = new TibrvCmListener(queue,
                                             this,
                                             cmTransport,
                                             subject,
                                             null);

            // Set explicit confirmation
            cmListener.setExplicitConfirm();

        }
        catch(TibrvException e)
        {
            System.out.println("Failed to create queue, transport or listener:");
            e.printStackTrace();
            System.exit(0);
        }

        // Report we are running Ok
        System.out.println("Listening on subject: "+subject);

        // Dispatch queue
        TibrvDispatcher disp = new TibrvDispatcher(queue);

        // This example never quits...
        // If we would close Tibrv this join() would go through,
        // but because we never close Tibrv we'll get stuck
        // inside the join() call forever.
        try
        {
            disp.join();
        }
        catch(InterruptedException e)
        {
            System.exit(0);
        }
    }

    //---------------------------------------------------------------
    // onMsg
    //---------------------------------------------------------------

    public void onMsg(TibrvListener listener, TibrvMsg msg)
    {
        System.out.println("Received message: "+msg);

        try
        {
            // Report we are confirming message
            long seqno = TibrvCmMsg.getSequence(msg);

            // If it was not CM message or very first message
            // we'll get seqno=0. Only confirm if seqno > 0.
            if (seqno > 0) {
                System.out.println("Confirming message with seqno="+seqno);
                System.out.flush();

                // Confirm the message
                cmListener.confirmMsg(msg);
            }
        }
        catch (TibrvException e)
        {
            System.out.println("Failed to confirm CM message: "+e.toString());
        }

        // if message had the reply subject, send the reply
        try
        {
            if (msg.getReplySubject() != null)
            {
                TibrvMsg reply = new TibrvMsg(msg.getAsBytes());
                cmTransport.sendReply(reply,msg);
            }
        }
        catch (TibrvException e)
        {
            System.out.println("Failed to send reply:");
            e.printStackTrace();
        }
    }

    //---------------------------------------------------------------
    // usage
    //---------------------------------------------------------------

    void usage()
    {
        System.out.println("Usage: java cmlistener [-service service] [-network network]");
        System.out.println("            [-daemon daemon] [-cmname cmname]");
        System.out.println("            [-subject subject]");
        System.out.println("    default values are:");
        System.out.println("       service = "+service);
        System.out.println("       network = "+network);
        System.out.println("       daemon  = "+daemon);
        System.out.println("       cmname  = "+cmname);
        System.out.println("       subject = "+subject);
        System.exit(-1);
    }

    //---------------------------------------------------------------
    // parseParams
    //---------------------------------------------------------------

    void parseParams(String[] args)
    {
        int i=0;
        while(i < args.length)
        {
            if (args[i].equalsIgnoreCase("-h") ||
                args[i].equalsIgnoreCase("-help") ||
                args[i].equalsIgnoreCase("?")) {
                usage();
            }
            else
            if (i == args.length-1) // all parameters require value
            {
                usage();
            }
            else
            if (args[i].equals("-service"))
            {
                service = args[i+1];
                i += 2;
            }
            else
            if (args[i].equals("-network"))
            {
                network = args[i+1];
                i += 2;
            }
            else
            if (args[i].equals("-daemon"))
            {
                daemon = args[i+1];
                i += 2;
            }
            else
            if (args[i].equals("-subject"))
            {
                subject = args[i+1];
                i += 2;
            }
            else
            if (args[i].equals("-cmname"))
            {
                cmname = args[i+1];
                i += 2;
            }
            else
                usage();
        }
    }

    //---------------------------------------------------------------
    // main()
    //---------------------------------------------------------------

    public static void main(String args[])
    {
        new cmlistener(args);
    }

}
