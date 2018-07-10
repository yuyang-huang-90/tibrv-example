package br.com.fredericci.test;

import com.tibco.tibrv.Tibrv;
import com.tibco.tibrv.TibrvMsg;
import com.tibco.tibrv.TibrvRvdTransport;

public class TibcoSender
{

	public static void main(String[] args) throws Exception
	{
		String service = "7500";
		String network = "loopback";
		String daemon  = "tcp:9025";
		String subject = "SOME.SUBJECT";

		Tibrv.open(Tibrv.IMPL_NATIVE);
		TibrvRvdTransport transport = new TibrvRvdTransport(service, network, daemon);

		TibrvMsg msg = new TibrvMsg();
		msg.setSendSubject(subject);
		msg.update("FIELD", "ASDF");
		transport.send(msg);

	}

}
