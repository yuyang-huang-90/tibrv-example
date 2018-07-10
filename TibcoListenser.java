import java.util.Date;
import com.tibco.tibrv.Tibrv;
import com.tibco.tibrv.TibrvException;
import com.tibco.tibrv.TibrvListener;
import com.tibco.tibrv.TibrvMsg;
import com.tibco.tibrv.TibrvMsgCallback;
import com.tibco.tibrv.TibrvRvdTransport;

public class TibcoListener implements TibrvMsgCallback
{

	public static void main(String[] args) throws TibrvException
	{

		String service = "7500";
		String network = "loopback";
		String daemon = "tcp:9025";
		String subject = "SOME.SUBJECT";

		Tibrv.open(Tibrv.IMPL_NATIVE);
		TibrvRvdTransport transport = new TibrvRvdTransport(service, network, daemon);

		new TibrvListener(Tibrv.defaultQueue(), new TibcoListener(), transport, subject, null);

		while (true) {
			try {
				Tibrv.defaultQueue().dispatch();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}

	public void onMsg(TibrvListener listener, TibrvMsg msg)
	{

		System.out.println((new Date()).toString() + ": subject=" + msg.getSendSubject() + ", reply=" + msg.getReplySubject() + ", message=" + msg.toString());

		System.out.flush();

	}

}
