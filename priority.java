
/*
 * Copyright (c) 1998-2002 TIBCO Software Inc.
 * All rights reserved.
 * TIB/Rendezvous is protected under US Patent No. 5,187,787.
 * For more information, please contact:
 * TIBCO Software Inc., Palo Alto, California, USA
 *
 * @(#)priority.java	1.5
 */


/*
 * priority - demonstrating queues, groups and priorities.
 *
 * There are no parameters required to run this program.
 *
 * This sample creates two queues with different priorities, creates
 * a queue group containing those queues, then publishes messages
 * to two listeners. After messages have been published the program
 * starts a dispatcher thread which processes message events.
 * Because queues have different priorities the callback will first
 * receive messages published into the queue with higher priority.
 *
 */

import com.tibco.tibrv.*;

public class priority implements TibrvMsgCallback
{
    String subject1 = "1";
    String subject2 = "2";

    public priority()
    {
    }

    public void execute()
    {
        try
        {
            // open Tibrv environment
            Tibrv.open(Tibrv.IMPL_JAVA);

            // get process transport
            TibrvTransport transport = Tibrv.processTransport();

            // create two queues
            TibrvQueue queue1 = new TibrvQueue();
            TibrvQueue queue2 = new TibrvQueue();

            // set priorities
            queue1.setPriority(1);
            queue2.setPriority(2);

            // Create queue group and add queues
            TibrvQueueGroup group = new TibrvQueueGroup();
            group.add(queue1);
            group.add(queue2);

            // Create listeners
            new TibrvListener(queue1,this,transport,subject1,null);
            new TibrvListener(queue2,this,transport,subject2,null);

            // Prepare the message
            TibrvMsg msg = new TibrvMsg();

            // Send 10 messages on subject1
            msg.setSendSubject(subject1);
            for (int i=0; i<10; i++)
            {
                msg.update("field","value-1-"+(i+1));
                transport.send(msg);
            }

            // Send 10 messages on subject2
            msg.setSendSubject(subject2);
            for (int i=0; i<10; i++)
            {
                msg.update("field","value-2-"+(i+1));
                transport.send(msg);
            }

            // Create dispatcher thread with timeout 1 second
            TibrvDispatcher dispatcher =
                    new TibrvDispatcher("dispatcher",group,1);

            // Wait until dispatcher processes all messages
            // and exits after 1 second timeout
            try
            {
                dispatcher.join();
            }
            catch(InterruptedException e)
            {
            }

            // Close Tibrv
            Tibrv.close();
        }
        catch (TibrvException rve)
        {
            // this program does not use the network
            // and supposedly should never fail.
            rve.printStackTrace();
            System.exit(0);
        }

    }

    // Message callback
    public void onMsg(TibrvListener listener, TibrvMsg msg)
    {
        System.out.println("Received message on subject "+
                           msg.getSendSubject()+": "+msg);
    }

    public static void main(String args[])
    {
        new priority().execute();
    }

}
