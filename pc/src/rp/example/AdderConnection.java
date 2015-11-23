package rp.example;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import lejos.pc.comm.NXTComm;
import lejos.pc.comm.NXTCommException;
import lejos.pc.comm.NXTCommFactory;
import lejos.pc.comm.NXTInfo;

public class AdderConnection implements Runnable {

	private DataInputStream m_dis;
	private DataOutputStream m_dos;
	private final int m_start;
	private final int m_count;
	private final NXTInfo m_nxt;
	private final static Object m_lock = new Object();

	public AdderConnection(NXTInfo _nxt, int _start, int _count) {
		m_start = _start;
		m_count = _count;
		m_nxt = _nxt;
	}

	public boolean connect(NXTComm _comm) throws NXTCommException {
		if (_comm.open(m_nxt)) {

			m_dis = new DataInputStream(_comm.getInputStream());
			m_dos = new DataOutputStream(_comm.getOutputStream());
		}
		return isConnected();
	}

	public boolean isConnected() {
		return m_dos != null;
	}

	@Override
	public void run() {

		try {

			for (int i = m_start; i < m_start + m_count; i++) {

				// without synchronisation across threads access Bluetooth this
				// class locks up. Looks like the underlying comms
				// implementation is not thread safe
				synchronized (m_lock) {
					m_dos.writeInt(i);
					m_dos.flush();
				}

				synchronized (m_lock) {
					int answer = m_dis.readInt();
					if (answer != i + 1) {
						break;
					}
				}
			}

			System.out.println(m_nxt.name + " checked out ok");
			return;

		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println(m_nxt.name + " was a problem");

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			NXTComm nxtComm = NXTCommFactory
					.createNXTComm(NXTCommFactory.BLUETOOTH);

			NXTInfo[] nxts = {
					new NXTInfo(NXTCommFactory.BLUETOOTH, "Steve",
							"00165308E539"),
					new NXTInfo(NXTCommFactory.BLUETOOTH, "Blanche 0.1",
							"00165312F1F0"),

					new NXTInfo(NXTCommFactory.BLUETOOTH, "simon",
							"0016530A971B"), };

			Random rand = new Random();

			ArrayList<AdderConnection> connections = new ArrayList<>(
					nxts.length);
			for (NXTInfo nxt : nxts) {
				connections.add(new AdderConnection(nxt, rand.nextInt(), 20));
			}

			for (AdderConnection connection : connections) {
				connection.connect(nxtComm);
			}

			ArrayList<Thread> threads = new ArrayList<>(nxts.length);

			for (AdderConnection connection : connections) {
				threads.add(new Thread(connection));
			}

			for (Thread thread : threads) {
				thread.start();
			}

			for (Thread thread : threads) {
				try {
					thread.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

		} catch (NXTCommException e) {
			e.printStackTrace();
		}

	}
}
