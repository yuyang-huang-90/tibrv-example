
/*
 * Copyright (c) 1998-2000 TIBCO Software Inc.
 * All rights reserved.
 * TIB/Rendezvous is protected under US Patent No. 5,187,787.
 * For more information, please contact:
 * TIBCO Software Inc., Palo Alto, California, USA
 *
 * @(#)usertypes.java	1.4
 */

/*
 * usertypes - demonstrates use of custom field types in TibrvMsg.
 *
 * This example sets user type for objects of Rectangle class.
 *
 * Result of setting such user type (and implementing associated
 * encoder and decoder) is that Rectangle objects can be added into
 * and retrieved from the TibrvMsg.
 *
 * Notice that encoders for one user type may be able to
 * handle objects of several different classes. This
 * examples only uses one class Rectangle.
 *
 */

import java.util.*;
import java.awt.*;
import java.io.*;

import com.tibco.tibrv.*;

public class usertypes implements TibrvMsgCallback,
                                     TibrvMsgEncoder,
                                     TibrvMsgDecoder
{
    TibrvTransport tport;
    TibrvListener  listener;
    TibrvQueue     queue;

    String subject          = "_LOCAL.test.user.type";
    String customFieldName  = "Rectangle";

    public static final short myFieldType = TibrvMsg.USER_FIRST;

    //
    // Messsage callback
    //
    public void onMsg(TibrvListener listener, TibrvMsg msg) {
        System.err.println("Received message on "+msg.getSendSubject());
        System.err.println("msg = "+msg.toString());
        try {
            Object obj = msg.get(customFieldName);
            if (obj == null)
                 System.err.println("Field "+customFieldName+" not in the message");
            else System.err.println("Field "+customFieldName+": "+obj.toString());

            Tibrv.close();
        }
        catch(TibrvException e) {
            e.printStackTrace();
        }
    }

    //
    // We only can encode Rectangle objects
    //
    public boolean canEncode(short type, Object data) {
        return (data instanceof Rectangle);
    }

    //
    // Encoder for Rectangle objects
    //
    public byte[] encode(short type, Object data) {
        // we only encode Rectangle objects
        try {
            Rectangle rect = (Rectangle)data;
            ByteArrayOutputStream byteout = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(byteout);
            out.writeInt(rect.x);
            out.writeInt(rect.y);
            out.writeInt(rect.width);
            out.writeInt(rect.height);
            out.close();
            return byteout.toByteArray();
        }
        catch(IOException ioe) {
            ioe.printStackTrace();
            return null;
        }
    }

    //
    // Decoder for Rectangle objects
    //
    public Object decode(short type, byte[] bytes) {
        // we only decode Rectangle objects
        try {
            Rectangle rect = new Rectangle();
            ByteArrayInputStream bytein = new ByteArrayInputStream(bytes);
            DataInputStream in = new DataInputStream(bytein);
            rect.x      = in.readInt();
            rect.y      = in.readInt();
            rect.width  = in.readInt();
            rect.height = in.readInt();
            return rect;
        }
        catch(IOException ioe) {
            ioe.printStackTrace();
            return null;
        }
    }

    public void work() {
        try {
            Tibrv.open(Tibrv.IMPL_NATIVE);
            queue = new TibrvQueue();
            TibrvMsg.setHandlers(myFieldType,this,this);
            tport = new TibrvRvdTransport();
            listener = new TibrvListener(queue,this,tport,subject,null);
            TibrvMsg msg = new TibrvMsg();
            msg.add(customFieldName,new Rectangle(22,33,44,55),myFieldType);
            msg.setSendSubject(subject);
            tport.send(msg);
            while(true) {
                try {
                    queue.dispatch();
                }
                catch(InterruptedException e) {
                    System.exit(0);
                }
                catch(TibrvException e) {
                    System.exit(0);
                }
            }
        }
        catch(TibrvException e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    public usertypes() {
    }

    public static void main(String[] args) {
        new usertypes().work();
    }

}
