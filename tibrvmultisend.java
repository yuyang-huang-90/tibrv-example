/*
 * Copyright (c) 1998-2002 TIBCO Software Inc.
 * All rights reserved.
 * TIB/Rendezvous is protected under US Patent No. 5,187,787.
 * For more information, please contact:
 * TIBCO Software Inc., Palo Alto, California, USA
 *
 * @(#)tibrvmultisend.java	1.11
 */


/*
 * tibrvmultisend - sample Rendezvous multi-field message publisher
 *
 * This program publishes a message with one or more fields on a
 * specified subject.  Both the subject and the fields must be
 * supplied as command parameters.  Fields with embedded spaces
 * should be quoted.  Fields are specified by the syntax
 *           [[name][,type]=]data
 * Name may not be a quoted string and may not contain comma or
 * equal sign characters.  If the field data contains either comma
 * or equal sign, an equal sign must preceed the data field. Type
 * is a string which represents one of the scalar rv datatypes
 * (string, bool, i8, u8, i16, u16, i32, u32, i64, u64, f32, f64,
 * opaque, xml).  If no name is specified, the field will be named
 * "DATA".  If no type is given or the type is not recognized,
 * the field will be added as a string.  This program does not
 * process datetime, ip address, or array field types. *
 *
 * Optionally the user may specify communication parameters for
 * tibrvTransport_Create.  If none are specified, default values
 * are used.  For information on default values for these parameters,
 * please see the TIBCO/Rendezvous Concepts manual.
 *
 *
 * Normally a listener such as tibrvlisten should be started first.
 *
 * Examples:
 *
 *  Publish a message on subject a.b.c with default parameters and
 *  two string fields named DATA:
 *   java tibrvmultisend a.b.c "This is my first field" "This is my second field"
 *
 *  Publish a message on subject a.b.c using port 7566 with two
 *  numeric fields:
 *   java tibrvmultisend -service 7566 a.b.c field1,i16=12345 field2,f64=12345.6789
 */

import java.util.*;
import java.io.*;
import com.tibco.tibrv.*;

public class tibrvmultisend
{

    String service = null;
    String network = null;
    String daemon  = null;
    String fieldspec = null;
    String fieldname = null;
    String fieldtype = null;
    String fielddata = null;
    TibrvMsgField field = null;
    boolean outofrange;
    boolean precision;
    short  rvtype;
    int    comma;
    int    equal;
    int    len;
    int    begin;
    int    end;
    TibrvException err;
    NumberFormatException numerr;    IOException ioerr;
    IllegalArgumentException argerr;
    String FIELD_NAME = "DATA";

    public tibrvmultisend(String args[])
    {
        // parse arguments for possible optional
        // parameters. These must precede the subject
        // and message strings
        int i = get_InitParams(args);

        // we must have at least one subject
        if (i > args.length-1)
            usage();
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
        TibrvTransport transport = null;
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

        // Create the message
        TibrvMsg msg = new TibrvMsg();

        // Set send subject into the message
        try
        {
            msg.setSendSubject(args[i++]);
        }
        catch (TibrvException e)
        {
            System.err.println("Failed to set send subject:");
            e.printStackTrace();
            System.exit(0);
        }

        // Set up a max value for range checking of U32 fields.
        long maxu32value = Integer.MAX_VALUE;
        maxu32value = 1 + 2 * maxu32value;

        if (i < args.length)
            System.out.println("Add fields to message:");

        // Step through command line, adding each field to the message.
        while (i < args.length)
        {
            fieldspec = args[i];
            fieldname = FIELD_NAME;
            fieldtype = "";
            outofrange = false;
            precision  = false;
            comma = fieldspec.indexOf(",");
            equal = fieldspec.indexOf("=");

            if (equal >= 0)
            {
                end = equal;
                if (comma >= 0) end = comma;
                // Field name is the string from the first character to
                // the comma or equal sign.  Field type is the string
                // between comma and equal sign.
                if (end > 0) fieldname = fieldspec.substring(0,end);
                if (comma >= 0 && (equal - comma) > 1)
                    if ((equal - comma + 1) > 0)
                        fieldtype = fieldspec.substring(comma+1,equal);
            }

            err = null;
            numerr = null;            ioerr = null;            argerr = null;
            fielddata = fieldspec.substring(equal+1);
            if (fieldtype.equalsIgnoreCase("BOOL"))
            {
                // For a boolean value, assign true if the first char
                // is Y or T, false otherwise.  No invalid values.
                String firstchar = fielddata.substring(0,1);
                boolean data = (firstchar.equalsIgnoreCase("T") |
                                firstchar.equalsIgnoreCase("Y"));
                try
                {
                    msg.add(fieldname, (boolean) data);
                }
                catch (TibrvException e) { err = e; }
            }
            else if (fieldtype.equalsIgnoreCase("I8"))
            {
                // Get value as long.  Add field if value is in range.
                try
                {
                    long data = Long.parseLong(fielddata);
                    if (data > Byte.MAX_VALUE || data < Byte.MIN_VALUE)
                    {
                        outofrange = true;
                    } else {
                        msg.add(fieldname, (byte) data);
                    }
                }
                catch (TibrvException e) { err = e; }
                catch (NumberFormatException e) { numerr = e; }
            }
            else if (fieldtype.equalsIgnoreCase("U8"))
            {
                // Get value as long.  Add field if value is in range.
                rvtype = TibrvMsg.U8;
                try
                {
                    long data = Long.parseLong(fielddata);
                    if (data < 0 || data > 1+2*Byte.MAX_VALUE)
                    {
                        outofrange = true;
                    } else {
                        msg.addU8(fieldname, (byte) data);
                    }
                }
                catch (TibrvException e) { err = e; }
                catch (NumberFormatException e) { numerr = e; }
            }
            else if (fieldtype.equalsIgnoreCase("I16"))
            {
                // Get value as long.  Add field if value is in range.
                rvtype = TibrvMsg.I16;
                try
                {
                    long data = Long.parseLong(fielddata);
                    if (data < Short.MIN_VALUE || data > Short.MAX_VALUE)
                    {
                        outofrange = true;
                    } else {
                        msg.add(fieldname, (short) data);
                    }
                }
                catch (TibrvException e) { err = e; }
                catch (NumberFormatException e) { numerr = e; }
            }
            else if (fieldtype.equalsIgnoreCase("U16"))
            {
                // Get value as long.  Add field if value is in range.
                rvtype = TibrvMsg.U16;
                try
                {
                    long data = Long.parseLong(fielddata);
                    if (data < 0 || data > 1+2*Short.MAX_VALUE)
                    {
                        outofrange = true;
                    } else {
                        msg.addU16(fieldname, (short) data);
                    }
                }
                catch (TibrvException e) { err = e; }
                catch (NumberFormatException e) { numerr = e; }
            }
            else if (fieldtype.equalsIgnoreCase("I32"))
            {
                // Get value as long.  Add field if value is in range.
                try
                {
                    long data = Long.parseLong(fielddata);
                    if (data < Integer.MIN_VALUE || data > Integer.MAX_VALUE)
                    {
                        outofrange = true;
                    } else {
                        msg.add(fieldname, (int) data);
                    }
                }
                catch (TibrvException e) { err = e; }
                catch (NumberFormatException e) { numerr = e; }
            }
            else if (fieldtype.equalsIgnoreCase("U32"))
            {
                // Get value as long.  Add field if value is in range.
                try
                {
                    long data = Long.parseLong(fielddata);
                    if (data < 0 || data > maxu32value)
                    {
                        outofrange = true;
                    } else {
                        msg.addU32(fieldname, (int) data);
                    }
                }
                catch (TibrvException e) { err = e; }
                catch (NumberFormatException e) { numerr = e; }
            }
            else if (fieldtype.equalsIgnoreCase("I64"))
            {
                // Get value as long.  No range checking.
                try
                {
                    long data = Long.parseLong(fielddata);
                    msg.add(fieldname, (long) data);
                }
                catch (TibrvException e) { err = e; }
                catch (NumberFormatException e) { numerr = e; }
            }
            else if (fieldtype.equalsIgnoreCase("U64"))
            {
                // Get value as long.  Check sign.  No range checking.
                try
                {
                    long data = Long.parseLong(fielddata);
                    if (data < 0)
                    {
                        outofrange = true;
                    } else {
                        msg.addU64(fieldname, (long) data);
                    }
                }
                catch (TibrvException e) { err = e; }
                catch (NumberFormatException e) { numerr = e; }
            }
            else if (fieldtype.equalsIgnoreCase("F32"))
            {
                // Get value as double.  No range checking.
                try
                {
                    double dataF64 = (Double.valueOf(fielddata)).doubleValue();
                    if (dataF64 < -Float.MAX_VALUE ||dataF64 > Float.MAX_VALUE)
                                outofrange = true;
                    float data = (float) dataF64;
                    if (data != dataF64) precision = true;
                    msg.add(fieldname, (float) data);
                }
                catch (TibrvException e) { err = e; }
                catch (NumberFormatException e) { numerr = e; }
            }
            else if (fieldtype.equalsIgnoreCase("F64"))
            {
                // Get value as double.  No range checking.
                try
                {
                    double data = (Double.valueOf(fielddata)).doubleValue();
                    msg.add(fieldname, data);
                }
                catch (TibrvException e) { err = e; }
                catch (NumberFormatException e) { numerr = e; }
            }
            else if (fieldtype.equalsIgnoreCase("STRING"))
            {
                // Add string field.
                try
                {
                    msg.add(fieldname, fielddata, TibrvMsg.STRING);
                }
                catch (TibrvException e) { err = e; }
            }
            else if (fieldtype.equalsIgnoreCase("OPAQUE"))
            {
                // Add opaque field.  The data must be in a byte array.  Here
                // we use the .getBytes() method to convert from String.
                try
                {
                    msg.add(fieldname, fielddata.getBytes(), TibrvMsg.OPAQUE);
                }
                catch (TibrvException e) { err = e; }
            }
            else if (fieldtype.equalsIgnoreCase("XML"))
            {
                // Add XML field.  The data must be added from a TibrvXml object.  We can
                // override the default character encoding by including an encoding name.
                // For example, to put the XML text into UNICODE (if the intended listener
                // is a VB program, for example), we would create the object using
                //        new TibrvXml(fielddata.getBytes("UTF-16"))
                try
                {
                    // Add to the message, using a TibrvXml object
                    msg.add(fieldname, new TibrvXml(fielddata.getBytes()), TibrvMsg.XML);
                }
                catch (TibrvException e) { err = e; }
            }
            else
            {
                // if not specified or recognized, default to string field.
                if (fieldtype != "") fieldtype = fieldtype+": unknown, ";
                fieldtype = "("+fieldtype+"default to string)";
                try
                {
                    msg.add(fieldname, fielddata);
                }
                catch (TibrvException e) { err = e; }
            }

            if (err != null)
            {
                // On Tibrv error, show stack trace.
                System.err.println("Failed to add field:");
                System.err.println("         "+fieldname+"  type: "+
                                   fieldtype+"  data: "+fielddata);
                err.printStackTrace();
            }
            else if (numerr != null)
            {
                // On numeric error, report skipping field.
                System.err.println("    Skip "+fieldname+"  type: "+
                                   fieldtype+"  data: "+fielddata+
                                   "    Number format error");
                numerr.printStackTrace();
            }
            else if (ioerr != null)
            {
                // On IO error, report skipping field.
                System.err.println("    Skip "+fieldname+"  type: "+
                                   fieldtype+"  data: "+fielddata+
                                   "    IO error");
                ioerr.printStackTrace();
            }
            else if (argerr != null)
            {
                // On argument error, report skipping field.
                System.err.println("    Skip "+fieldname+"  type: "+
                                   fieldtype+"  data: "+fielddata+
                                   "    arg error");
                argerr.printStackTrace();
            }
            else if (outofrange)
            {
                // If value was out of range, report skipping field.
                System.err.println("    Skip "+fieldname+"  type: "+
                                   fieldtype+"  data: "+fielddata+
                                   "    Data out of range");
            }
            else
            {
                // After successful add, report data to standard out.
                // Report if loss of data in F32 due to limited precision.
                System.out.print("         "+fieldname+"    type: "+
                                   fieldtype+"  data: "+fielddata);
                if (precision)
                    System.out.print("  (precision/rounding)");
                System.out.println();
            }
            i++;
        }

        // Report ready to publish, with string extract of completed message.
        System.out.println("Publishing: subject="+msg.getSendSubject()+
                           " "+msg.toString());

        // Send the message.
        try
        {
            transport.send(msg);
        }
        catch (TibrvException e)
        {
            System.err.println("Error sending message:");
            e.printStackTrace();
            System.exit(0);
        }
        catch (StackOverflowError s)
        {
            System.err.println("Error sending message:");
            s.printStackTrace();
            // System.exit(0);
        }
        // Close Tibrv.  This will cleanup all underlying memory, destroy
        // transport and guarantee delivery.
        try
        {
            Tibrv.close();
        }
        catch(TibrvException e)
        {
            System.err.println("Exception closing Tibrv:");
            e.printStackTrace();
            System.exit(0);
        }

    }

    // print usage information and quit
    void usage()
    {
        System.err.println("Usage: java tibrvmultisend [-service service]");
        System.err.println("                           [-network network]");
        System.err.println("                           [-daemon daemon]");
        System.err.println("                           <subject> ");
        System.err.println("                           <[[name][,type]=data] [[name],[type]=data] ....>");
        System.err.println("       TIB/Rendezvous datatypes accepted by tibrvmultisend include:");
        System.err.println("                           string, opaque, xml, bool, ");
        System.err.println("                           i8, i16, i32, i64,            (signed integer)");
        System.err.println("                           u8, u16, u32, u64,            (unsigned integer)");
        System.err.println("                           f32, f64                      (floating point)");
        System.exit(-1);
    }

    // Parse command line arguments for transport parameters.
    int get_InitParams(String[] args)
    {
        int i=0;

        if (args.length == 0) usage();
        else if (args[i].equals("?") || args[i].startsWith("-h"))
            usage();

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
        new tibrvmultisend(args);
    }

}
