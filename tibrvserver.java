
/*
 * Copyright (c) 1998-2002 TIBCO Software Inc.
 * All rights reserved.
 * TIB/Rendezvous is protected under US Patent No. 5,187,787.
 * For more information, please contact:
 * TIBCO Software Inc., Palo Alto, California, USA
 *
 * @(#)tibrvserver.java	1.7
 */


/*
 * tibrvserver - TIB/Rendezvous server program
 *
 * This program will answer trivial request from tibrvclient
 *
 * Optionally the user may specify communication parameters for
 * tibrvTransport_Create.  If none are specified, default values
 * are used.  For information on default values for these parameters,
 * please see the TIBCO/Rendezvous Concepts manual.
 *
 *
 */

import java.util.*;
import com.tibco.tibrv.*;

public class tibrvserver implements TibrvMsgCallback
{

    String service = null;
    String network = null;
    String daemon  = null;

    static String request_subject;
    static String query_subject = "TIBRV.LOCATE";

    TibrvTransport transport;
    TibrvMsg reply_msg;
    TibrvMsg response_msg;

    int x;
    int y;
    int sum;

    boolean msg_received = true;

    public tibrvserver(String args[])
    {
        // parse arguments for possible optional
        // parameters.
        int i = get_InitParams(args);

        // open Tibrv in native implementation
        try
        {
            Tibrv.open(Tibrv.IMPL_NATIVE);
        }
        catch (TibrvException e)
        {
            System.err.println("Failed to open Tibrv in native implementation:");
            e.printStackTrace();
            System.exit(0);
        }

        // Create RVD transport
        try
        {
            transport = new TibrvRvdTransport(service,network,daemon);
            transport.setDescription("tibrvclient");
        }
        catch (TibrvException e)
        {
            System.err.println("Failed to create TibrvRvdTransport:");
            e.printStackTrace();
            System.exit(0);
        }

       // Create request subject (inbox) and listener
        try
        {
            request_subject = transport.createInbox();
            new TibrvListener(Tibrv.defaultQueue(),
                            this,transport,request_subject,null);
        }
        catch (TibrvException e)
        {
            System.err.println("Failed to initialilze request listener:");
            e.printStackTrace();
            System.exit(0);
        }

       // Create query listener
        try
        {
            new TibrvListener(Tibrv.defaultQueue(),
                            this,transport,query_subject,null);
        }
        catch (TibrvException e)
        {
            System.err.println("Failed to initialilze query listener:");
            e.printStackTrace();
            System.exit(0);
        }

        // create query reply and request response messages
        reply_msg = new TibrvMsg();
        response_msg = new TibrvMsg();

        System.out.println("Server ready...");

        // dispatch Tibrv events with 60 second timeout.  If message not
        // received in 60 seconds, quit.
        while(msg_received)
        {
            msg_received = false;
            try
            {
                Tibrv.defaultQueue().timedDispatch(60);
            }
            catch (TibrvException e)
            {
                System.err.println("Exception dispatching default queue:");
                e.printStackTrace();
                System.exit(0);
            }
            catch(InterruptedException ie)
            {
                System.exit(0);
            }
        }
    }

    // Message callback.  Flag message received.  If query, reply with server's
    // request subject.  If request, validate message and reply.
    public void onMsg(TibrvListener listener, TibrvMsg msg)
    {
        msg_received = true;
        if (listener.getSubject() .equals (query_subject))
        {
            try
            {
                reply_msg.setReplySubject(request_subject);
                transport.sendReply(reply_msg,msg);
            }
            catch (TibrvException e)
            {
                System.err.println("Exception dispatching default queue:");
                e.printStackTrace();
                System.exit(0);
            }
        }
        else
        {
            try
            {
                x = msg.getAsInt("x",0);
            }
            catch (TibrvException e)
            {
                System.err.println("tibrvserver: Received bad request (x param).");
                return;
            }
            try
            {
                y = msg.getAsInt("y",0);
            }
            catch (TibrvException e)
            {
                System.err.println("tibrvserver: Received bad request (y param).");
                return;
            }
            sum = x + y;
            try
            {
                response_msg.update("sum", sum, TibrvMsg.U32);
                transport.sendReply(response_msg, msg);
            }
            catch (TibrvException e)
            {
                System.err.println("Error sending a response to request message:");
                e.printStackTrace();
                return;
            }
        }
    }

    // print usage information and quit
    void usage()
    {
        System.err.println("Usage: java tibrvserver [-service service] [-network network]");
        System.err.println("            [-daemon daemon]");
        System.exit(-1);
    }

    // parse command line parameters.
    int get_InitParams(String[] args)
    {
        int i=0;
        if (args.length > 0)
        {
            if (args[i].equals("-?") ||
                args[i].equals("-h") ||
                args[i].equals("-help"))
            {
                usage();
            }
        }
        while(i < args.length-1 && args[i].startsWith("-"))
        {
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
                usage();
        }
        return i;
    }

    public static void main(String args[])
    {
        new tibrvserver(args);
    }

}
