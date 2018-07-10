
/*
 * Copyright (c) 1998-2002 TIBCO Software Inc.
 * All rights reserved.
 * TIB/Rendezvous is protected under US Patent No. 5,187,787.
 * For more information, please contact:
 * TIBCO Software Inc., Palo Alto, California, USA
 *
 * @(#)tibrvfttime.java	1.7
 */


/*
 * tibrvfttime.java - example timestamp message program using
 *                    TIB/Rendezvous Fault Tolerant API
 *
 * This program publishes the time every ten seconds.  It is designed to
 * be run with a fault tolerant backup to maintain a continuous though
 * unsynchronized timestamp.  One active member of group
 * TIBRVFT_TIME_EXAMPLE will publish the time.
 *
 * The user may specify the weight of each member of the fault tolerant
 * group as an optional command line parameter.  The default is 50.
 *
 * Optionally the user may specify communication parameters for
 * the transport used by the timestamp application and, separately,
 * for the fault tolerance.  If none are specified, default values
 * are used.  For information on default values for these parameters,
 * please see the TIBCO/Rendezvous Concepts manual.
 *
 *
 * To hear the timestamp messages, use tibrvlisten with subject
 * TIBRVFT_TIME.
 *
 * Examples:
 *
 *  Publish timestamp using all default parameters:
 *   tibrvfttime
 *
 *  Start backup timestamp publisher using with weight 10:
 *   tibrvfttime 10
 *
 *  Publish timestamps on default service, but use service 7544 for
 *  TIBRVFT messages:
 *   tibrvfttime -ft-service 7544
 */

import java.util.*;
import com.tibco.tibrv.*;

public class tibrvfttime implements TibrvMsgCallback, TibrvTimerCallback,
                TibrvFtMemberCallback
{

    String  service = null;
    String  network = null;
    String  daemon  = null;
    String  ftservice = null;
    String  ftnetwork = null;
    String  ftdaemon  = null;
    boolean active = false;
    String  ftsendsubject = "TIBRVFT_TIME";
    String  ftgroupName = "TIBRVFT_TIME_EXAMPLE";
    int     ftweight = 50;
    int     numactive = 1;
    double  hbInterval = 1.5;
    double  prepareInterval = 0;
    double  activateInterval = 4.8;

    TibrvTransport transport;
    TibrvTimer fttimer;
    TibrvMsg timer_msg;

    public tibrvfttime(String args[])
    {
        // parse arguments for possible optional
        // parameters. These must precede the weight value
        int i = get_InitParams(args);

        // if weight value is given without identifier, set ftweight
        if (args.length > i)
        {
            ftweight = Integer.parseInt(args[i]);
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

        // Main Transport is for timestamp publishing.
        // Initialize it with the given parameters or default NULLs.
        try
        {
            transport = new TibrvRvdTransport(service,network,daemon);
        }
        catch (TibrvException e)
        {
            System.err.println("Failed to create TibrvRvdTransport:");
            e.printStackTrace();
            System.exit(0);
        }

        // Prepare for publishing timestamp every 10 seconds.
        // Start repeating timer here.  Check a flag in the timer
        // callback routine, pubTime, as to whether this member is
        // the active group member.  If so it will format and send
        // the timestamp message.

        System.out.println
            ("Active group member will publish time every 10 seconds.");
        System.out.println("Subject is "+ ftsendsubject);
        System.out.println("Inactive will not publish time.");

        fttimer = null;
        try
        {
            fttimer = new TibrvTimer(Tibrv.defaultQueue(),this,10.0,transport);
        }
        catch (TibrvException e)
        {
            System.err.println("Failed to create timer:");
            e.printStackTrace();
            System.exit(0);
        }

        // RVFT communications may benefit from a separate transport.
        TibrvTransport fttransport = null;
        try
        {
            fttransport = new TibrvRvdTransport(ftservice,ftnetwork,ftdaemon);
        }
        catch (TibrvException e)
        {
            System.err.println("Failed to create ft TibrvRvdTransport:");
            e.printStackTrace();
            System.exit(0);
        }

        // Join the RVFT group.
        try {
            new TibrvFtMember(Tibrv.defaultQueue(),
                              this,
                              fttransport,
                              ftgroupName,
                              ftweight,
                              numactive,
                              hbInterval,
                              prepareInterval,
                              activateInterval,
                              null);
        }
            catch (TibrvException e)
            {
                System.err.println("Exception joining fault tolerance group:");
                e.printStackTrace();
                System.exit(0);
            }

        // Subscribe to FT advisories
        try
        {
            new TibrvListener(Tibrv.defaultQueue(),
                        this,fttransport,"_RV.*.RVFT.*.TIBRVFT_TIME_EXAMPLE",null);
            System.out.println("Listening on: _RV.*.RVFT.*.TIBRVFT_TIME_EXAMPLE");
        }
        catch (TibrvException e)
        {
            System.err.println("Failed to create advisory listener:");
            e.printStackTrace();
            System.exit(0);
        }

        // dispatch Tibrv events
        while(true)
        {
            try
            {
                Tibrv.defaultQueue().dispatch();
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

    public void onFtAction(TibrvFtMember member, String ftgroupName, int action)
    {
        if (action == TibrvFtMember.PREPARE_TO_ACTIVATE) {
            System.out.println("TibrvFtMember.PREPARE_TO_ACTIVATE invoked...");
            System.out.println("####### PREPARE TO ACTIVATE: "+ ftgroupName);
        }
        else if (action == TibrvFtMember.ACTIVATE) {
            System.out.println("TibrvFtMember.ACTIVATE invoked...");
            System.out.println("####### ACTIVATE: "+ ftgroupName);
            active = true;
        }
        else if (action == TibrvFtMember.DEACTIVATE) {
            System.out.println("TibrvFtMember.DEACTIVATE invoked...");
            System.out.println("####### DEACTIVATE: "+ ftgroupName);
            active = false;
        }
    } // onFtAction

    public void onTimer(TibrvTimer timer) {
        // If not the active member, just return.
        if (!active) {return;}

        // Create the timer message
        TibrvMsg timer_msg = new TibrvMsg();

        // Set send subject into the message
        try
        {
            timer_msg.setSendSubject(ftsendsubject);
        }
        catch (TibrvException e)
        {
            System.err.println("Failed to set send subject:");
            e.printStackTrace();
            System.exit(0);
        }

        //set the value and send the message
        try
        {
            timer_msg.update("DATA", new TibrvDate(), TibrvMsg.DATETIME);
            transport.send(timer_msg);
        }
        catch (TibrvException e)
        {
            System.err.println("Error sending a message:");
            e.printStackTrace();
            System.exit(0);
        }

    }

    public void onMsg(TibrvListener listener, TibrvMsg msg)
    {
        System.out.println("#### RVFT ADVISORY: "+
                           msg.getSendSubject()
                          );
        System.out.println(msg.toString()
                          );
        System.out.flush();
    }

    // print usage information and quit
    void usage()
    {
        System.err.println("Usage: java tibrvfttime");
        System.err.println("            -service <s>      Service for rendezvous communications");
        System.err.println("            -network <s>      Network to use");
        System.err.println("            -daemon <s>       Daemon to connect to");
        System.err.println("            -ft-service <s>   Service for fault tolerance communication");
        System.err.println("            -ft-network <s>   Network for fault tolerance");
        System.err.println("            -ft-daemon <s>    Daemon for fault tolerance");
        System.err.println("            -ft-weight <s>    Weight of this instance for fault tolerance");
        System.err.println("            <weight>          Weight can be entered with no identifier");
        System.err.println("            -? or -help or -h Display usage information");
        System.exit(-1);
    }

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
            if (args[i].equals("-ft-service"))
            {
                ftservice = args[i+1];
                i += 2;
            }
            else
            if (args[i].equals("-ft-network"))
            {
                ftnetwork = args[i+1];
                i += 2;
            }
            else
            if (args[i].equals("-ft-daemon"))
            {
                ftdaemon = args[i+1];
                i += 2;
            }
            else
            if (args[i].equals("-ft-weight"))
            {
                ftweight = Integer.parseInt(args[i+1]);
                i += 2;
            }
            else
                usage();
        }
        return i;
    }

    public static void main(String args[])
    {
        new tibrvfttime(args);
    }

}
