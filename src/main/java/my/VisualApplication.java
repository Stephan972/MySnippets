package my;

import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.lang.reflect.InvocationTargetException;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.event.ChangeListener;
import javax.swing.text.JTextComponent;

import my.exceptions.ApplicationRuntimeException;
import my.swing.SwingUtils;

public abstract class VisualApplication extends ConsoleApplication implements Runnable {
	private State currentState;

	protected void activateSystemLookAndFeel() {
		SwingUtils.activateSystemLookAndFeel();
	}

	protected void setLineBorderFor(JComponent component, Color color) {
		SwingUtils.setLineBorderFor(component,color);
	}

	protected JLabel bold(JLabel label) {
		return SwingUtils.bold(label);
	}	
	
	protected void addChangeListener(JTextComponent text, ChangeListener listener) {
		SwingUtils.addChangeListener(text,listener);
	}
	
	protected void alert(Throwable t) {
		String msg = t.getMessage();
		alert("An exception has been raised. (see logs for details)\n" + msg.substring(0, Math.min(256, msg.length())));
	}

	protected void alert(String msg) {
		JOptionPane.showMessageDialog(null, msg);
	}

	protected void alert(Component comp, String msg) {
		JOptionPane.showMessageDialog(comp, msg);
	}

	protected void initGUI() {
		try {
			currentState = State.INIT_GUI;
			EventQueue.invokeAndWait(this);
		} catch (InvocationTargetException | InterruptedException e) {
			throw new ApplicationRuntimeException(e);
		}
	}

	/**
	 * 
	 * Build application GUI inside the UI thread.
	 * 
	 */
	protected void onInitGUI() {
		throw new UnsupportedOperationException("VisualApplication::onInitGUI MUST be overriden.");
	}

	@Override
	public void run() {
		switch (currentState) {
		case INIT_GUI:
			onInitGUI();
			break;

		default:
			throw new ApplicationRuntimeException("Unknown state: " + currentState);
		}
	}

	private enum State {
		INIT_GUI;
	}
}
