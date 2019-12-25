package my;

public interface ArgumentName {

	boolean hasCommandLineSwitch();

	/**
	 * 
	 * It is illegal to call this method if {@link #hasCommandLineSwitch()}
	 * returns false.
	 * 
	 * @return the full command line switch of the argument represented by this
	 *         ArgumentName.
	 */
	default String getCommandLineSwitch() {
		throw new UnsupportedOperationException();
	}

	/**
	 * 
	 * <ul>
	 * <li>
	 * TRUE => NEVER display in clear this argument value any where (screen, log
	 * etc)
	 * <li>
	 * FALSE => This argument value can be shown anywhere (screen, log etc)
	 * 
	 * @return
	 */
	boolean doNotPrintValue();
}
