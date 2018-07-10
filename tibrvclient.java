
/*
 * Copyright (c) 1998-2002 TIBCO Software Inc.
 * All rights reserved.
 * TIB/Rendezvous is protected under US Patent No. 5,187,787.
 * For more information, please contact:
 * TIBCO Software Inc., Palo Alto, California, USA
 *
 * @(#)tibrvclient.java	1.7
 */


/*
 * tibrvclient - TIB/Rendezvous client program
 *
 * This program will attempt to contact the server program and then
 * perform a series of tests to determine msg throughput and response
 * times.
 *
 * Optionally the user may specify communication parameters for
 * tibrvTransport_Create.  If none are specified, default values
 * are used.  For information on default values for these parameters,
 * please see the TIBCO/Rendezvous Concepts manual.
 *
 * The user may specify the number of server requests.  If none is
 * specified the default value is 10000.
 *
 */

import java.util.*;
import com.tibco.tibrv.*;

public class tibrvclient implements TibrvMsgCallback
{

    String service = null;
    String network = null;
    String daemon  = null;
    long requests = 10000;
    static long responses = 0;
    static String query_subject = "TIBRV.LOCATE";   // To find the server
    static String response_subject;
    static double query_timeout = 10.0;
    static double test_timeout = 10.0;

    TibrvTransport transport;
    static TibrvDate start_dt;
    static TibrvDate stop_dt;
    static double start_time;
    static double stop_time;
    double elapsed;
    long x = 1;
    long y = 2;
    public tibrvclient(String args[])
    {
        // parse arguments for possible optional
        // parameters. These must precede the number of requests
        int i = get_InitParams(args);

        // if requests value is given, set requests count
        if (args.length > i)
        {
            requests = Integer.parseInt(args[i]);
        }

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
            System.err.println(" ");
            System.exit(0);
        }

        // Create a response queue
        TibrvQueue response_queue = null;
        try
        {
            response_queue = new TibrvQueue();
        }
        catch (TibrvException e)
        {
            System.err.println("Failed to create TibrvRvdTransport:");
            e.printStackTrace();
            System.exit(0);
        }

        // Create an inbox subject for communication with the server and
        // create a listener for this response subject.
        try
        {
            response_subject = transport.createInbox();
            new TibrvListener(response_queue,
                            this,transport,response_subject,null);
        }
        catch (TibrvException e)
        {
            System.err.println("Failed to create listener:");
            e.printStackTrace();
            System.exit(0);
        }

        // Create a message for the query.
        TibrvMsg query_msg = new TibrvMsg();
        try
        {
            query_msg.setSendSubject(query_subject);
        }
        catch (TibrvException e) {
            System.err.println("Failed to set send subject:");
            e.printStackTrace();
            System.exit(0);
        }

        // Query for our server.  sendRequest generates an inbox
        // reply subject.
        System.err.println("Attempting to contact server using subject " + query_subject + "...");

        TibrvMsg reply_msg = null;
        try
        {
            reply_msg = transport.sendRequest(query_msg, query_timeout);
        }
        catch (TibrvException e)
        {
            System.err.println("Failed to detect server:");
            e.printStackTrace();
            System.exit(0);
        }

        // If timeout, reply message is null and query failed.
        if (reply_msg == null)
        {
            System.err.println("Failed to detect server.");
            System.exit(0);
        }
        TibrvMsg server_msg = new TibrvMsg();
        String server_subject = reply_msg.getReplySubject();

        // Create a dispatcher with 5 second timeout to process server replies
        TibrvDispatcher dispatcher = new TibrvDispatcher("Dispatcher",response_queue,5.0);

        System.err.println("Succeeded!");

        // Set up client request message and report subjects used.  Send subject
        // is the reply subject from the server's answer to our query.
        try
        {
            System.out.println("Set server subject to : "+server_subject);
            server_msg.setSendSubject(server_subject);
            System.out.println("Set client subject to : "+response_subject);
            server_msg.setReplySubject(response_subject);
        }
        catch (TibrvException e)
        {
            System.err.println("Failed to set subjects, fields for test message:");
            e.printStackTrace();
            System.exit(0);
        }

        // Send the specified number of requests to the server.
        Random rand = new Random();
        System.err.println("Starting test....");
        start_dt = new TibrvDate(new Date());
        start_time = start_dt.getTimeSeconds() + start_dt.getTimeNanoseconds()/1000000000.0;

        for (i=0; i<requests; i++)
        {
            try
            {
                server_msg.updateU32("x", (int) rand.nextInt());
                server_msg.updateU32("y", (int) rand.nextInt());
            }
            catch (TibrvException e)
            {
                System.err.println("Failed to set fields in test message:");
                e.printStackTrace();
                System.exit(0);
            }
            try
            {
                transport.send(server_msg);
            }
            catch (TibrvException e)
            {
                System.err.println("Failed to send test message:");
                e.printStackTrace();
                System.exit(0);
            }
        }
    }

    // Listener callback counts responses, reports after all replies received.
    public void onMsg(TibrvListener listener, TibrvMsg msg)
    {
        if (++responses >= requests)
        {
            stop_dt = new TibrvDate(new Date());
            stop_time = stop_dt.getTimeSeconds() + stop_dt.getTimeNanoseconds()/1000000000.0;

            elapsed = stop_time - start_time;

            System.out.println("Client received all "+requests+" responses");
            System.out.println(requests+" requests took "+elapsed+" secs to process.");
            System.out.println("Effective rate of "+(requests/elapsed)+" request/sec.");

            transport.destroy();
            System.exit(1);
        }
    }

    // Print usage information and quit
    void usage()
    {
        System.err.println("Usage: java tibrvclient [-service service] [-network network]");
        System.err.println("            [-daemon daemon] <#requests>");
        System.exit(-1);
    }

    // Parse command line parameters.
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
        new tibrvclient(args);
    }

}
