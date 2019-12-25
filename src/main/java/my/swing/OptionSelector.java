package my.swing;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.miginfocom.swing.MigLayout;

public final class OptionSelector implements Runnable {
	@Getter
	@Setter(value = AccessLevel.PRIVATE)
	private String currentlySelectedOption;

	private JPanel pnlMain;
	private JTextField txtCurrentlySelectedOption;

	private LastOptions lastOptions;
	
	private OptionSelector() {
		// * Load options file
		lastOptions = new LastOptions();
		lastOptions.load();

		// * Init UI
		SwingUtils.activateSystemLookAndFeel();

		pnlMain = new JPanel(new MigLayout("fill"));

		// Selected filename
		txtCurrentlySelectedOption = new JTextField();
		pnlMain.add(txtCurrentlySelectedOption, "split, growx");

		JButton btnSelectFile = new JButton("Select a file...");
		pnlMain.add(btnSelectFile, "wrap");

		// Last files count
		JLabel lblLastFilesCount = new JLabel("Last files count:");
		pnlMain.add(lblLastFilesCount, "split");

		JTextField txtLastFilesCount = new JTextField(lastOptions.getLastFilesCountAsString());
		pnlMain.add(txtLastFilesCount, "wrap");

		// Last selected files
		JList<String> lstLastSelectedFiles = new JList<>(lastOptions);
		pnlMain.add(SwingUtils.wrapInStandardJScrollPane(lstLastSelectedFiles), "grow,span");

		// Action listeners
		txtCurrentlySelectedOption.getDocument().addDocumentListener(new DocumentListener() {

			@Override
			public void removeUpdate(DocumentEvent e) {
				updateSelectedOption();
			}

			@Override
			public void insertUpdate(DocumentEvent e) {
				updateSelectedOption();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				updateSelectedOption();
			}

			private void updateSelectedOption() {
				updateCurrentlySelectedOption(txtCurrentlySelectedOption.getText(), false, true);
			}
		});

		btnSelectFile.addActionListener(new ActionListener() {
			private JFileChooser fcr;

			{
				fcr = new JFileChooser(".");
				fcr.setFileFilter(new FileFilter() {

					@Override
					public String getDescription() {
						return "";
					}

					@Override
					public boolean accept(File aFile) {
						String aFileAbsolutePath = aFile.getAbsolutePath().toLowerCase();
						String currentLastOptionsAbsolutePath = lastOptions.getFile().getAbsolutePath().toLowerCase();

						return !aFileAbsolutePath.equals(currentLastOptionsAbsolutePath);
					}
				});
			}

			@Override
			public void actionPerformed(ActionEvent e) {
				if (fcr.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
					updateCurrentlySelectedOption(fcr.getSelectedFile().getAbsolutePath(), true, true);
				}
			}
		});

		lstLastSelectedFiles.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					int index = lstLastSelectedFiles.locationToIndex(e.getPoint());
					if (index >= 0) {
						lstLastSelectedFiles.ensureIndexIsVisible(index);
						updateCurrentlySelectedOption(lastOptions.getElementAt(index), true, false);
					}
				}
			}
		});
	}

	private void updateCurrentlySelectedOption(String option, boolean updateTextField, boolean updateLastOptions) {
		setCurrentlySelectedOption(option);
		
		if (updateTextField) {
			txtCurrentlySelectedOption.setText(option);
		}
		
		if (updateLastOptions) {
			lastOptions.insertElementAt(option, 0);
		}
	} 

	private static class ConfigurationFileSelectorHolder {
		private final static OptionSelector instance = new OptionSelector();
	}

	/**
	 *
	 * @return
	 */
	private static OptionSelector getInstance() {
		return ConfigurationFileSelectorHolder.instance;
	}

	/**
	 * 
	 * @return The selected option value or null.
	 */
	public static String ask() {
		try {
			OptionSelector instance = getInstance();

			SwingUtilities.invokeAndWait(instance);

			return instance.getCurrentlySelectedOption();
		} catch (InvocationTargetException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void run() {
		int ret = JOptionPane.showConfirmDialog(null, pnlMain, "Select configuration file...", JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.PLAIN_MESSAGE);
		if (ret != JOptionPane.OK_OPTION) {
			setCurrentlySelectedOption(null);
		}
	}

	@Getter
	@Setter(value = AccessLevel.PRIVATE)
	class LastOptions extends DefaultListModel<String> {
		private static final long serialVersionUID = -743610822077885986L;
		private static final String LAST_OPTIONS_FILE = "last.options";

		private int lastFilesCount = 10;
		private File file;

		private void load() {
			// @see http://stackoverflow.com/a/5868528
			file = new File(LAST_OPTIONS_FILE);
			try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
				int index = 1;
				for (String line; (line = br.readLine()) != null;) {
					if (index == 1) {
						setLastFilesCount(Integer.parseInt(line));
					} else {
						insertElementAt(line, size());
					}
					index++;

					if (index - 1 > lastFilesCount) {
						break;
					}
				}
			} catch (FileNotFoundException fnfe) {
				save();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		public String getLastFilesCountAsString() {
			return Integer.toString(getLastFilesCount());
		}

		private void save() {
			try (Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(LAST_OPTIONS_FILE, false), StandardCharsets.UTF_8))) {
				String lineSeparator = System.lineSeparator();
				out.write(Integer.toString(lastFilesCount) + lineSeparator);
				for (Enumeration<String> e = elements(); e.hasMoreElements();) {
					out.write(e.nextElement() + lineSeparator);
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void insertElementAt(String element, int index) {
			if (!contains(element)) {
				int size = size();
				if (size >= lastFilesCount) {
					this.removeRange(lastFilesCount - 1, size - 1);
				}

				super.insertElementAt(element, index);
				save();
			}
		}
	}
}
