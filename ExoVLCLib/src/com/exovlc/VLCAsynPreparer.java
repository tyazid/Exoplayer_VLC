package com.exovlc;

import java.util.EventListener;
import java.util.EventObject;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public interface VLCAsynPreparer {

	void setPrepareListener(PrepareListener l);

	void doAsyncPrepare();

	public static final class EventDispatcher {
		private final PrepareListener l;
		private BlockingQueue<PrepareEvent> q;
		private Timer t;
		private boolean alive = true;
		private Thread notifyer;

		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				alive = true;
				notifyer = Thread.currentThread();
				PrepareEvent e;
				while (alive) {
					try {
						e = q.take();
					} catch (InterruptedException e2) {
						break;
					}
					try {
						l.onEvent(e);
					} catch (Exception e1) {
						e1.printStackTrace();

					}
				}
			}
		};

		public EventDispatcher(PrepareListener l) {
			this.l = l;
			this.q = new LinkedBlockingQueue<VLCAsynPreparer.PrepareEvent>();
			this.t = new Timer();
			this.t.schedule(task, 0L);

		}

		public synchronized void kill() {
			if (!alive)
				return;
			alive = false;
			t.cancel();
			if (notifyer != null)
				notifyer.interrupt();
			q = null;
		}

		public synchronized void postEvent(PrepareEvent e) {
			if (q != null)
				q.add(e);
		}
	}

	public static interface PrepareListener extends EventListener {
		void onEvent(PrepareEvent e);
	}

	@SuppressWarnings("serial")
	public static final class PrepareEvent extends EventObject {

		private Throwable err_raison;
		private Type type;

		public enum Type {
			START_PREPARING, END_SUCCESSFULLY, END_FAILED
		};

		public PrepareEvent(Type type, VLCAsynPreparer src) {
			this(type, src, null);
		}

		public PrepareEvent(Type type, VLCAsynPreparer src, Throwable err) {
			super(src);
			this.err_raison = err;
			this.type = type;
		}

		public Throwable getErrorRaison() {
			return err_raison;
		}

		public Type getType() {
			return type;
		}

	}
}
