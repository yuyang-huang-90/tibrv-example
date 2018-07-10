
/*
 * Copyright (c) 1998-2002 TIBCO Software Inc.
 * All rights reserved.
 * TIB/Rendezvous is protected under US Patent No. 5,187,787.
 * For more information, please contact:
 * TIBCO Software Inc., Palo Alto, California, USA
 *
 * @(#)sendobject.java	1.5
 */

/*
 * sendobject - sends Java objects via TIB/Rendezvous messages
 *
 * This example demonstrates how to send and receive Java
 * objects which support Serializable interface.
 *
 * Note: this can only be used when both the sender and the
 *       receiver are Java applications. Demonstrated technique
 *       must not be used when it is required to exchange messages
 *       with applications implemented in other languages.
 *
 * This example uses NATIVE implementation and does not have any
 * parameters. It can be run with a simple command:
 *
 *  java sendobject
 *
 */

import java.util.*;
import java.io.*;

import com.tibco.tibrv.*;

/*-------------------------------------------------------------------
 * PersonalData is an example of a simple class supporting
 * Serializable interface.
 * This example program sends and receives objects of this class.
 *-----------------------------------------------------------------*/

class PersonalData implements Serializable
{
    String  firstName  = "";
    String  lastName   = "";
    Integer age        = new Integer(0);

    public PersonalData() {}

    public PersonalData(String firstName, String lastName, int age) {
        this.firstName = firstName;
        this.lastName  = lastName;
        this.age       = new Integer(age);
    }

    public String toString() {
        return lastName+", "+firstName+" - "+age+" y.o.";
    }
}

/*-------------------------------------------------------------------
 * sendobject is a simple example which uses Serializable
 * interface in order to send Java objects via TIB/Rendezvous messages.
 *
 * This program creates a simple Tibrv environment with a single
 * queue and one listener. It creates an object of PersonalData
 * class, converts it into a byte array and sends it on the subject.
 * The listener receives the message, recovers the object and quits
 * the program.
 *-----------------------------------------------------------------*/
public class sendobject implements TibrvMsgCallback {

    // Our test subject
    String subject   = "test.send.java.object";

    // Field name we use to add object into TibrvMsg
    String fieldName = "object";

    public sendobject(String[] args)
    {
        try
        {
            // open Tibrv
            Tibrv.open(Tibrv.IMPL_NATIVE);

            // Create queue, dispatcher, simple transport
            TibrvQueue        queue = new TibrvQueue();
            TibrvDispatcher   disp  = new TibrvDispatcher(queue);
            TibrvRvdTransport tport = new TibrvRvdTransport();

            // create listener
            TibrvListener     listener =
                        new TibrvListener(queue,this,tport,subject,null);

            // create an object we want to send as a field
            // in TibrvMsg
            PersonalData data = new PersonalData("John","Doe",25);

            // create the message
            TibrvMsg msg = new TibrvMsg();
            msg.setSendSubject(subject);

            // add object as a field
            boolean ok = addObject(msg,fieldName,data);
            if (!ok) {
                System.err.println("Failed to add object into message");
                System.exit(0);
            }

            // send the message
            tport.send(msg);

            // wait until the listener receives the messages
            // and closes Tibrv
            try {
                disp.join();
            }
            catch(InterruptedException e) {
                System.exit(0);
            }
        }
        catch(TibrvException e) {
            e.printStackTrace(System.err);
            System.exit(0);
        }
    }

    /*---------------------------------------------------------------
     * Example of the method which adds Java object into TibrvMsg
     * as a field with specified name. This method assumes the
     * object implements Serializable interface.
     *-------------------------------------------------------------*/

    public boolean addObject(TibrvMsg msg, String fieldName, Object object)
    {
        try
        {
            // write object into the object stream
            ByteArrayOutputStream outBytes  = new ByteArrayOutputStream();
            ObjectOutputStream    outStream = new ObjectOutputStream(outBytes);
            outStream.writeObject(object);
            outStream.close();

            // get the byte array and include it as a field
            // with specified name
            byte[] array = outBytes.toByteArray();
            msg.add(fieldName,array);

            return true;
        }
        catch(NotSerializableException e) {
            // given object did not support Serializable interface
            System.err.println("ERROR: object is not serializable");
            e.printStackTrace(System.err);
        }
        catch(IOException e) {
            // some problem, not likely to happen since we
            // use byte array as the output media
            System.err.println("ERROR: IOException writing object into stream");
            e.printStackTrace(System.err);
        }
        catch(TibrvException e) {
            e.printStackTrace(System.err);
        }
        return false;
    }

    /*---------------------------------------------------------------
     * Example of the method which recovers Java object from TibrvMsg
     * field with specified name. This method assumes
     * the field with specified name contains a serialized object.
     *-------------------------------------------------------------*/

    public Object getObject(TibrvMsg msg, String fieldName)
    {
        try
        {
            // assume if field exists it must be a byte array
            byte[] array = (byte[])msg.get(fieldName);

            // check if field not found
            if (array == null)
            return null;

            // assume the byte array is a serialized object
            // and recover it
            ByteArrayInputStream  inBytes  = new ByteArrayInputStream(array);
            ObjectInputStream     inStream = new ObjectInputStream(inBytes);
            Object object = inStream.readObject();

            return object;
        }
        catch(ClassNotFoundException e) {
            // when Java tried to recover the object it could not find the
            // class definition. It can not happen in this program but
            // may happen in a real distributed environment
            System.err.println("ERROR: Class not found while recovering object from the stream");
            e.printStackTrace(System.err);
        }
        catch(OptionalDataException e) {
            System.err.println("ERROR: Unexpected data in the stream while recovering object from the stream");
            e.printStackTrace(System.err);
        }
        catch(StreamCorruptedException e) {
            System.err.println("ERROR: Corrupted stream detected while recovering object from the stream");
            e.printStackTrace(System.err);
        }
        catch(IOException e) {
            System.err.println("ERROR: IOException while recovering object from the stream");
            e.printStackTrace(System.err);
        }
        catch(TibrvException e) {
            e.printStackTrace(System.err);
        }
        return null;
    }

    /*---------------------------------------------------------------
     * Listener callback.
     *
     * Upon receiving the message this callback tries to recover
     * the Java object sent in a field of the message and then
     * closes Tibrv.
     *-------------------------------------------------------------*/

    public void onMsg(TibrvListener listener, TibrvMsg msg)
    {
        // try to retrieve the object
        Object object = getObject(msg,fieldName);

        if (object == null)
            System.err.println("Error: object not found in message or exception occurred");
        else
            System.err.println("Retrieved object: class="+object.getClass().getName()+
                               ", toString()="+object);

            // close Tibrv to make this example to exit...
        try {
            Tibrv.close();
        }
        catch(TibrvException e){}
    }

    /*---------------------------------------------------------------
     * main
     *-------------------------------------------------------------*/

    public static void main(String[] args)
    {
        sendobject t = new sendobject(args);
    }

}
