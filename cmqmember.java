
/*
 * Copyright (c) 1998-2002 TIBCO Software Inc.
 * All rights reserved.
 * TIB/Rendezvous is protected under US Patent No. 5,187,787.
 * For more information, please contact:
 * TIBCO Software Inc., Palo Alto, California, USA
 *
 * @(#)cmqmember.java	1.5
 */


/*
 * cmqmember - implements a member of Distributed Queue.
 *
 * To use this example you should start 3 or more instances
 * of this program and then run cmsender example.
 *
 * There are no required parameters for this example.
 * Optional parameters are:
 *
 * -service   - RVD transport parameter
 * -network   - RVD transport parameter
 * -daemon    - RVD transport parameter
 * -queue     - CM Queue name
 * -subject   - subject this example listens on
 *
 * If no transport parameters are specified, default values are used.
 * For information on default values for these parameters,  please see
 * the TIBCO/Rendezvous Concepts manual.
 *
 * Default values for other parameters:
 *  queue       "cm.queue"
 *  subject     "cm.test.subject"
 *
 */

import java.util.*;
import com.tibco.tibrv.*;

public class cmqmember implements TibrvMsgCallback
{
    // RVD transport parameters
    String service = null;
    String network = null;
    String daemon  = null;

    // Queue subject
    String subject   = "cm.test.subject";

    // Queue name
    String queueName = "cm.queue";

    //---------------------------------------------------------------
    // cmqmember
    //---------------------------------------------------------------

    public cmqmember(String args[])
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

        // Create RVD transport
        TibrvRvdTransport rvdTransport = null;
        try
        {
            rvdTransport = new TibrvRvdTransport(service,network,daemon);
        }
        catch (TibrvException e)
        {
            System.out.println("Failed to create TibrvRvdTransport:");
            e.printStackTrace();
            System.exit(0);
        }

        try
        {
            // Create event queue
            TibrvQueue queue = new TibrvQueue();

            // Create Distributed Queue
            TibrvCmQueueTransport cmqTransport;
            cmqTransport = new TibrvCmQueueTransport(rvdTransport,queueName);

            // Create queue listener
            TibrvCmListener queueListener;
            queueListener = new TibrvCmListener(queue,this,cmqTransport,subject,null);

            // Create dispatcher
            TibrvDispatcher disp = new TibrvDispatcher(queue);

            // Report we initialized Ok
            System.out.println("Queue name="+queueName+", listening on subject "+subject);

            // We'll never pass through this
            // call because this example never stops.
            disp.join();
        }
        catch (TibrvException e)
        {
            e.printStackTrace();
            System.exit(0);
        }
        catch (InterruptedException e)
        {
            System.exit(0);
        }
    }

    //---------------------------------------------------------------
    // onMsg
    //---------------------------------------------------------------

    public void onMsg(TibrvListener listener, TibrvMsg msg)
    {
        try
        {
            // Report received message and it's seqno.
            long seqno = TibrvCmMsg.getSequence(msg);
            System.out.println("Received message with seqno="+seqno+": "+msg);
            System.out.flush();
        }
        catch(TibrvException e)
        {
            System.out.println("Failed to obtain seqno from CM message:");
            e.printStackTrace();
        }
    }

    //---------------------------------------------------------------
    // usage
    //---------------------------------------------------------------

    void usage()
    {
        System.out.println("Usage: java cmqmember [-service service] [-network network]");
        System.out.println("            [-daemon daemon] [-queue queueName]");
        System.out.println("            [-subject subject]");
        System.out.println("    default values are:");
        System.out.println("       service = "+service);
        System.out.println("       network = "+network);
        System.out.println("       daemon  = "+daemon);
        System.out.println("       queue   = "+queueName);
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
            if (args[i].equals("-queue"))
            {
                queueName = args[i+1];
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
        new cmqmember(args);
    }

}
